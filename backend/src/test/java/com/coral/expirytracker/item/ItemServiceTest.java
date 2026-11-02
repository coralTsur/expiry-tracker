package com.coral.expirytracker.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class ItemServiceTest {

	private static final LocalDate TODAY = LocalDate.of(2030, 1, 15);

	private final ItemRepository repository;
	private final ItemService service;

	ItemServiceTest(@Autowired ItemRepository repository) {
		this.repository = repository;
		this.service = new ItemService(repository, clockAt(TODAY));
	}

	// data.sql dates follow the real today and the clock here is fixed, so its rows must not reach any test.
	@BeforeEach
	void removeSampleData() {
		repository.deleteAll();
	}

	@Test
	void findAll_includesExpiredAndRedeemedItems() {
		repository.save(item("Active", TODAY.plusDays(5)));
		repository.save(item("Expired", TODAY.minusDays(5)));
		Item redeemedItem = item("Redeemed", TODAY.plusDays(10));
		redeemedItem.setRedeemed(true);
		repository.save(redeemedItem);

		assertThat(service.findAll())
				.extracting(ItemResponse::name)
				.containsExactlyInAnyOrder("Active", "Expired", "Redeemed");
	}

	@Test
	void findAll_sortsByExpirationDateAscending() {
		repository.save(item("In 20 days", TODAY.plusDays(20)));
		repository.save(item("Expired", TODAY.minusDays(5)));
		repository.save(item("In 400 days", TODAY.plusDays(400)));

		assertThat(service.findAll())
				.extracting(ItemResponse::name)
				.containsExactly("Expired", "In 20 days", "In 400 days");
	}

	@Test
	void findById_returnsAllFieldsOfTheItem() {
		Item item = new Item(Category.GIFT_CARD, "BuyMe", TODAY.plusDays(5), true);
		item.setAmount(new BigDecimal("200.00"));
		item.setCurrentAmount(new BigDecimal("150.00"));
		item.setLastFourDigits("1234");
		item.setAcceptedStores("Fox, Castro");
		item.setDaysBeforeReminder(14);
		item.setRedeemed(true);
		Item saved = repository.save(item);

		ItemResponse response = service.findById(saved.getId());

		assertThat(response.id()).isEqualTo(saved.getId());
		assertThat(response.category()).isEqualTo(Category.GIFT_CARD);
		assertThat(response.name()).isEqualTo("BuyMe");
		assertThat(response.expirationDate()).isEqualTo(TODAY.plusDays(5));
		assertThat(response.amount()).isEqualByComparingTo("200.00");
		assertThat(response.currentAmount()).isEqualByComparingTo("150.00");
		assertThat(response.lastFourDigits()).isEqualTo("1234");
		assertThat(response.acceptedStores()).isEqualTo("Fox, Castro");
		assertThat(response.reminderEnabled()).isTrue();
		assertThat(response.daysBeforeReminder()).isEqualTo(14);
		assertThat(response.redeemed()).isTrue();
	}

	@Test
	void findById_throwsItemNotFound_whenIdDoesNotExist() {
		assertThatThrownBy(() -> service.findById(99L))
				.isInstanceOf(ItemNotFoundException.class)
				.hasMessage("פריט 99 לא נמצא");
	}

	@Test
	void expiringSoon_includesItemExpiringToday() {
		repository.save(item("Today", TODAY));

		assertThat(service.findExpiringSoon())
				.extracting(ItemResponse::name)
				.containsExactly("Today");
	}

	@Test
	void expiringSoon_includesItemExpiringIn30Days() {
		repository.save(item("In 30 days", TODAY.plusDays(30)));

		assertThat(service.findExpiringSoon())
				.extracting(ItemResponse::name)
				.containsExactly("In 30 days");
	}

	@Test
	void expiringSoon_excludesItemExpiredYesterday() {
		repository.save(item("Yesterday", TODAY.minusDays(1)));

		assertThat(service.findExpiringSoon()).isEmpty();
	}

	@Test
	void expiringSoon_excludesItemExpiringIn31Days() {
		repository.save(item("In 31 days", TODAY.plusDays(31)));

		assertThat(service.findExpiringSoon()).isEmpty();
	}

	@Test
	void expiringSoon_excludesRedeemedItems() {
		Item redeemedItem = item("Redeemed", TODAY.plusDays(5));
		redeemedItem.setRedeemed(true);
		repository.save(redeemedItem);

		assertThat(service.findExpiringSoon()).isEmpty();
	}

	@Test
	void expiringSoon_ignoresReminderSettings() {
		Item reminderOff = item("Reminder off", TODAY.plusDays(10));
		reminderOff.setDaysBeforeReminder(null);
		repository.save(reminderOff);
		Item reminderNotDueYet = new Item(Category.INSURANCE, "Reminder not due yet", TODAY.plusDays(20), true);
		reminderNotDueYet.setDaysBeforeReminder(7);
		repository.save(reminderNotDueYet);

		assertThat(service.findExpiringSoon())
				.extracting(ItemResponse::name)
				.containsExactlyInAnyOrder("Reminder off", "Reminder not due yet");
	}

	@Test
	void expiringSoon_sortsByExpirationDate() {
		repository.save(item("In 20 days", TODAY.plusDays(20)));
		repository.save(item("In 5 days", TODAY.plusDays(5)));
		repository.save(item("In 10 days", TODAY.plusDays(10)));

		assertThat(service.findExpiringSoon())
				.extracting(ItemResponse::name)
				.containsExactly("In 5 days", "In 10 days", "In 20 days");
	}

	@Test
	void expiringSoon_windowMovesWithTheClock() {
		repository.save(item("In 40 days", TODAY.plusDays(40)));
		ItemService tenDaysLater = new ItemService(repository, clockAt(TODAY.plusDays(10)));

		assertThat(service.findExpiringSoon()).isEmpty();
		assertThat(tenDaysLater.findExpiringSoon())
				.extracting(ItemResponse::name)
				.containsExactly("In 40 days");
	}

	// ---- create: saving and defaults ----

	@Test
	void create_savesItemAndReturnsItWithNewId() {
		ItemRequest request = validRequest()
				.amount("200.00").currentAmount("150.00").lastFourDigits("1234").acceptedStores("Fox, Castro")
				.reminderEnabled(true).daysBeforeReminder(14).build();

		ItemResponse response = service.create(request);

		assertThat(response.id()).isNotNull();
		assertThat(repository.findById(response.id())).isPresent();
		assertThat(repository.count()).isEqualTo(1);
		assertThat(response.category()).isEqualTo(Category.GIFT_CARD);
		assertThat(response.name()).isEqualTo("BuyMe");
		assertThat(response.expirationDate()).isEqualTo(TODAY.plusDays(60));
		assertThat(response.amount()).isEqualByComparingTo("200.00");
		assertThat(response.currentAmount()).isEqualByComparingTo("150.00");
		assertThat(response.lastFourDigits()).isEqualTo("1234");
		assertThat(response.acceptedStores()).isEqualTo("Fox, Castro");
		assertThat(response.reminderEnabled()).isTrue();
		assertThat(response.daysBeforeReminder()).isEqualTo(14);
		assertThat(response.redeemed()).isFalse();
	}

	@Test
	void create_optionalFieldsNotSent_areSavedAsNull() {
		ItemResponse response = service.create(validRequest().build());

		assertThat(response.amount()).isNull();
		assertThat(response.currentAmount()).isNull();
		assertThat(response.lastFourDigits()).isNull();
		assertThat(response.acceptedStores()).isNull();
	}

	@Test
	void create_emptyOptionalText_isSavedAsNull() {
		ItemResponse response = service.create(validRequest().lastFourDigits("").acceptedStores("").build());

		assertThat(response.lastFourDigits()).isNull();
		assertThat(response.acceptedStores()).isNull();
	}

	@Test
	void create_redeemedNotSent_isFalse() {
		ItemResponse response = service.create(validRequest().redeemed(null).build());

		assertThat(response.redeemed()).isFalse();
	}

	@Test
	void create_redeemedSentTrue_isSavedAsSent() {
		ItemResponse response = service.create(validRequest().redeemed(true).build());

		assertThat(response.redeemed()).isTrue();
		assertThat(repository.findById(response.id()).orElseThrow().isRedeemed()).isTrue();
	}

	@Test
	void create_daysBeforeReminderNotSent_reminderOn_gets30() {
		ItemResponse response = service.create(validRequest().reminderEnabled(true).daysBeforeReminder(null).build());

		assertThat(response.daysBeforeReminder()).isEqualTo(30);
	}

	@Test
	void create_daysBeforeReminderNotSent_reminderOff_gets30() {
		ItemResponse response = service.create(validRequest().reminderEnabled(false).daysBeforeReminder(null).build());

		assertThat(response.daysBeforeReminder()).isEqualTo(30);
	}

	@Test
	void create_daysBeforeReminderNotSent_rejectedWhenDefault30IsNotLessThanDaysRemaining() {
		ItemRequest request = validRequest()
				.expirationDate(TODAY.plusDays(20)).reminderEnabled(true).daysBeforeReminder(null).build();

		assertRejectedWith(request, Map.of("daysBeforeReminder",
				"טווח התזכורת ארוך מהזמן שנשאר עד התוקף, אנא בחרי טווח קצר יותר"));
	}

	// ---- create: required fields ----

	@Test
	void create_rejectsMissingName() {
		assertRejectedWith(validRequest().name(null).build(), Map.of("name", "יש להזין שם"));
	}

	@Test
	void create_rejectsNameWithOnlySpaces() {
		assertRejectedWith(validRequest().name("   ").build(), Map.of("name", "יש להזין שם"));
	}

	@Test
	void create_rejectsMissingCategory() {
		assertRejectedWith(validRequest().category(null).build(), Map.of("category", "יש לבחור קטגוריה"));
	}

	@Test
	void create_rejectsMissingExpirationDate() {
		assertRejectedWith(validRequest().expirationDate(null).build(),
				Map.of("expirationDate", "יש להזין תאריך תוקף"));
	}

	@Test
	void create_rejectsMissingReminderEnabled() {
		assertRejectedWith(validRequest().reminderEnabled(null).build(),
				Map.of("reminderEnabled", "יש לבחור אם להפעיל תזכורת"));
	}

	// ---- create: expiration date must be in the future ----

	@Test
	void create_rejectsExpirationDateToday() {
		assertRejectedWith(validRequest().expirationDate(TODAY).build(),
				Map.of("expirationDate", "התאריך כבר עבר, אנא הזיני תאריך עתידי"));
	}

	@Test
	void create_rejectsExpirationDateInThePast() {
		assertRejectedWith(validRequest().expirationDate(TODAY.minusDays(1)).build(),
				Map.of("expirationDate", "התאריך כבר עבר, אנא הזיני תאריך עתידי"));
	}

	@Test
	void create_acceptsExpirationDateTomorrow() {
		ItemResponse response = service.create(validRequest().expirationDate(TODAY.plusDays(1)).build());

		assertThat(response.expirationDate()).isEqualTo(TODAY.plusDays(1));
	}

	@Test
	void create_dateRuleUsesTheInjectedClock() {
		ItemRequest request = validRequest().expirationDate(TODAY.plusDays(5)).build();
		ItemService tenDaysLater = new ItemService(repository, clockAt(TODAY.plusDays(10)));

		assertThatThrownBy(() -> tenDaysLater.create(request)).isInstanceOf(InvalidItemException.class);
		assertThat(repository.count()).isZero();
		assertThat(service.create(request).id()).isNotNull();
	}

	// ---- create: reminder must be shorter than the days remaining ----

	@Test
	void create_rejectsReminderLongerThanDaysRemaining() {
		ItemRequest request = validRequest()
				.expirationDate(TODAY.plusDays(20)).reminderEnabled(true).daysBeforeReminder(30).build();

		assertRejectedWith(request, Map.of("daysBeforeReminder",
				"טווח התזכורת ארוך מהזמן שנשאר עד התוקף, אנא בחרי טווח קצר יותר"));
	}

	@Test
	void create_rejectsReminderEqualToDaysRemaining() {
		ItemRequest request = validRequest()
				.expirationDate(TODAY.plusDays(20)).reminderEnabled(true).daysBeforeReminder(20).build();

		assertRejectedWith(request, Map.of("daysBeforeReminder",
				"טווח התזכורת ארוך מהזמן שנשאר עד התוקף, אנא בחרי טווח קצר יותר"));
	}

	@Test
	void create_acceptsReminderOneLessThanDaysRemaining() {
		ItemRequest request = validRequest()
				.expirationDate(TODAY.plusDays(20)).reminderEnabled(true).daysBeforeReminder(19).build();

		assertThat(service.create(request).daysBeforeReminder()).isEqualTo(19);
	}

	@Test
	void create_acceptsReminderOfZeroDays_forItemExpiringTomorrow() {
		ItemRequest request = validRequest()
				.expirationDate(TODAY.plusDays(1)).reminderEnabled(true).daysBeforeReminder(0).build();

		assertThat(service.create(request).daysBeforeReminder()).isZero();
	}

	@Test
	void create_reminderOff_skipsReminderRule() {
		ItemRequest request = validRequest()
				.expirationDate(TODAY.plusDays(20)).reminderEnabled(false).daysBeforeReminder(30).build();

		assertThat(service.create(request).daysBeforeReminder()).isEqualTo(30);
	}

	@Test
	void create_skipsReminderRule_whenExpirationDateIsMissing() {
		ItemRequest request = validRequest()
				.expirationDate(null).reminderEnabled(true).daysBeforeReminder(30).build();

		assertRejectedWith(request, Map.of("expirationDate", "יש להזין תאריך תוקף"));
	}

	@Test
	void create_skipsReminderRule_whenExpirationDateHasPassed() {
		ItemRequest request = validRequest()
				.expirationDate(TODAY.minusDays(5)).reminderEnabled(true).daysBeforeReminder(30).build();

		assertRejectedWith(request, Map.of("expirationDate", "התאריך כבר עבר, אנא הזיני תאריך עתידי"));
	}

	@Test
	void create_rejectsNegativeDaysBeforeReminder() {
		ItemRequest request = validRequest().reminderEnabled(true).daysBeforeReminder(-1).build();

		assertRejectedWith(request, Map.of("daysBeforeReminder", "מספר הימים לא יכול להיות שלילי"));
	}

	@Test
	void create_rejectsNegativeDaysBeforeReminder_evenWhenReminderIsOff() {
		ItemRequest request = validRequest().reminderEnabled(false).daysBeforeReminder(-1).build();

		assertRejectedWith(request, Map.of("daysBeforeReminder", "מספר הימים לא יכול להיות שלילי"));
	}

	// ---- create: amounts ----

	@Test
	void create_rejectsNegativeAmount() {
		assertRejectedWith(validRequest().amount("-1").build(), Map.of("amount", "הסכום לא יכול להיות שלילי"));
	}

	@Test
	void create_rejectsNegativeCurrentAmount() {
		assertRejectedWith(validRequest().amount("100").currentAmount("-1").build(),
				Map.of("currentAmount", "הסכום לא יכול להיות שלילי"));
	}

	@Test
	void create_acceptsZeroAmounts() {
		ItemResponse response = service.create(validRequest().amount("0").currentAmount("0").build());

		assertThat(response.amount()).isEqualByComparingTo("0");
		assertThat(response.currentAmount()).isEqualByComparingTo("0");
	}

	@Test
	void create_rejectsCurrentAmountAboveAmount() {
		assertRejectedWith(validRequest().amount("100").currentAmount("100.01").build(),
				Map.of("currentAmount", "הסכום העדכני לא יכול להיות גדול מהסכום המקורי"));
	}

	@Test
	void create_acceptsCurrentAmountEqualToAmount_evenWithDifferentScale() {
		ItemResponse response = service.create(validRequest().amount("200").currentAmount("200.00").build());

		assertThat(response.currentAmount()).isEqualByComparingTo("200");
	}

	@Test
	void create_acceptsCurrentAmountWithoutAmount() {
		ItemResponse response = service.create(validRequest().currentAmount("50").build());

		assertThat(response.amount()).isNull();
		assertThat(response.currentAmount()).isEqualByComparingTo("50");
	}

	// ---- create: last four digits ----

	@Test
	void create_rejectsLastFourDigitsWithThreeDigits() {
		assertRejectedWith(validRequest().lastFourDigits("123").build(),
				Map.of("lastFourDigits", "יש להזין בדיוק 4 ספרות"));
	}

	@Test
	void create_rejectsLastFourDigitsWithFiveDigits() {
		assertRejectedWith(validRequest().lastFourDigits("12345").build(),
				Map.of("lastFourDigits", "יש להזין בדיוק 4 ספרות"));
	}

	@Test
	void create_rejectsLastFourDigitsWithLetters() {
		assertRejectedWith(validRequest().lastFourDigits("12a4").build(),
				Map.of("lastFourDigits", "יש להזין בדיוק 4 ספרות"));
	}

	@Test
	void create_acceptsLastFourDigitsWithLeadingZeros() {
		assertThat(service.create(validRequest().lastFourDigits("0042").build()).lastFourDigits()).isEqualTo("0042");
	}

	// ---- create: all errors together ----

	@Test
	void create_returnsAllErrorsTogether() {
		ItemRequest request = validRequest()
				.name(" ").category(null).expirationDate(TODAY.minusDays(1)).reminderEnabled(null)
				.amount("-5").currentAmount("10").lastFourDigits("12").build();

		assertRejectedWith(request, Map.of(
				"name", "יש להזין שם",
				"category", "יש לבחור קטגוריה",
				"expirationDate", "התאריך כבר עבר, אנא הזיני תאריך עתידי",
				"reminderEnabled", "יש לבחור אם להפעיל תזכורת",
				"amount", "הסכום לא יכול להיות שלילי",
				"currentAmount", "הסכום העדכני לא יכול להיות גדול מהסכום המקורי",
				"lastFourDigits", "יש להזין בדיוק 4 ספרות"));
	}

	private void assertRejectedWith(ItemRequest request, Map<String, String> expectedFieldErrors) {
		assertThatThrownBy(() -> service.create(request))
				.isInstanceOfSatisfying(InvalidItemException.class,
						exception -> assertThat(exception.getFieldErrors()).containsExactlyInAnyOrderEntriesOf(expectedFieldErrors));
		// spec section 5: when there is an error, the item is not saved.
		assertThat(repository.count()).isZero();
	}

	// A valid request with reminder off, expiring 60 days after the fixed today. Each test changes only what it checks.
	private static RequestBuilder validRequest() {
		return new RequestBuilder();
	}

	private static final class RequestBuilder {

		private Category category = Category.GIFT_CARD;
		private String name = "BuyMe";
		private LocalDate expirationDate = TODAY.plusDays(60);
		private BigDecimal amount;
		private BigDecimal currentAmount;
		private String lastFourDigits;
		private String acceptedStores;
		private Boolean reminderEnabled = false;
		private Integer daysBeforeReminder;
		private Boolean redeemed;

		RequestBuilder category(Category value) {
			category = value;
			return this;
		}

		RequestBuilder name(String value) {
			name = value;
			return this;
		}

		RequestBuilder expirationDate(LocalDate value) {
			expirationDate = value;
			return this;
		}

		RequestBuilder amount(String value) {
			amount = new BigDecimal(value);
			return this;
		}

		RequestBuilder currentAmount(String value) {
			currentAmount = new BigDecimal(value);
			return this;
		}

		RequestBuilder lastFourDigits(String value) {
			lastFourDigits = value;
			return this;
		}

		RequestBuilder acceptedStores(String value) {
			acceptedStores = value;
			return this;
		}

		RequestBuilder reminderEnabled(Boolean value) {
			reminderEnabled = value;
			return this;
		}

		RequestBuilder daysBeforeReminder(Integer value) {
			daysBeforeReminder = value;
			return this;
		}

		RequestBuilder redeemed(Boolean value) {
			redeemed = value;
			return this;
		}

		ItemRequest build() {
			return new ItemRequest(category, name, expirationDate, amount, currentAmount, lastFourDigits,
					acceptedStores, reminderEnabled, daysBeforeReminder, redeemed);
		}

	}

	private static Clock clockAt(LocalDate date) {
		return Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
	}

	private Item item(String name, LocalDate expirationDate) {
		return new Item(Category.GIFT_CARD, name, expirationDate, false);
	}

}
