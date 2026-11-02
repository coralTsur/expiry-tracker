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
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class ItemServiceTest {

	private static final LocalDate TODAY = LocalDate.of(2030, 1, 15);

	private final ItemRepository repository;
	private final TestEntityManager entityManager;
	private final ItemService service;

	ItemServiceTest(@Autowired ItemRepository repository, @Autowired TestEntityManager entityManager) {
		this.repository = repository;
		this.entityManager = entityManager;
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

	// ---- update: section 7 acceptance criteria ----

	// spec section 7: updating current amount to more than the amount -> error, the change is not saved.
	@Test
	void update_rejectsCurrentAmountAboveAmount_andKeepsStoredItem() {
		Item stored = storedItem(TODAY.plusDays(60), false, 30);
		stored.setAmount(new BigDecimal("200"));
		stored.setCurrentAmount(new BigDecimal("150"));
		repository.save(stored);

		assertUpdateRejectedWith(stored.getId(), requestFrom(stored).currentAmount("250").build(),
				Map.of("currentAmount", "הסכום העדכני לא יכול להיות גדול מהסכום המקורי"));
		assertThat(reloaded(stored.getId()).getCurrentAmount()).isEqualByComparingTo("150");
	}

	// spec section 7: updating only current amount on an item with 20 days left and a 30-day reminder -> saved.
	@Test
	void update_onlyCurrentAmount_onItemWith20DaysAndReminder30_isSaved() {
		Item stored = storedItem(TODAY.plusDays(20), true, 30);
		stored.setAmount(new BigDecimal("200"));
		stored.setCurrentAmount(new BigDecimal("150"));
		repository.save(stored);

		service.update(stored.getId(), requestFrom(stored).currentAmount("100").build());

		assertThat(reloaded(stored.getId()).getCurrentAmount()).isEqualByComparingTo("100");
	}

	// spec section 7: updating the name of an expired item -> saved.
	@Test
	void update_nameOfExpiredItem_isSaved() {
		Item stored = repository.save(storedItem(TODAY.minusDays(5), true, 7));

		service.update(stored.getId(), requestFrom(stored).name("New name").build());

		assertThat(reloaded(stored.getId()).getName()).isEqualTo("New name");
	}

	// ---- update: not found ----

	@Test
	void update_throwsItemNotFound_whenIdDoesNotExist() {
		assertThatThrownBy(() -> service.update(99L, validRequest().build()))
				.isInstanceOf(ItemNotFoundException.class)
				.hasMessage("פריט 99 לא נמצא");
		assertThat(repository.count()).isZero();
	}

	// ---- update: date rule runs only when the date changed ----

	@Test
	void update_rejectsExpirationDateChangedToToday() {
		Item stored = repository.save(storedItem(TODAY.plusDays(60), false, 30));

		assertUpdateRejectedWith(stored.getId(), requestFrom(stored).expirationDate(TODAY).build(),
				Map.of("expirationDate", "התאריך כבר עבר, אנא הזיני תאריך עתידי"));
		assertThat(reloaded(stored.getId()).getExpirationDate()).isEqualTo(TODAY.plusDays(60));
	}

	@Test
	void update_rejectsExpirationDateChangedToThePast() {
		Item stored = repository.save(storedItem(TODAY.plusDays(60), false, 30));

		assertUpdateRejectedWith(stored.getId(), requestFrom(stored).expirationDate(TODAY.minusDays(1)).build(),
				Map.of("expirationDate", "התאריך כבר עבר, אנא הזיני תאריך עתידי"));
		assertThat(reloaded(stored.getId()).getExpirationDate()).isEqualTo(TODAY.plusDays(60));
	}

	@Test
	void update_acceptsExpirationDateChangedToTomorrow() {
		Item stored = repository.save(storedItem(TODAY.minusDays(5), false, 30));

		service.update(stored.getId(), requestFrom(stored).expirationDate(TODAY.plusDays(1)).build());

		assertThat(reloaded(stored.getId()).getExpirationDate()).isEqualTo(TODAY.plusDays(1));
	}

	@Test
	void update_rejectsMissingExpirationDate() {
		Item stored = repository.save(storedItem(TODAY.plusDays(60), false, 30));

		assertUpdateRejectedWith(stored.getId(), requestFrom(stored).expirationDate(null).build(),
				Map.of("expirationDate", "יש להזין תאריך תוקף"));
		assertThat(reloaded(stored.getId()).getExpirationDate()).isEqualTo(TODAY.plusDays(60));
	}

	// ---- update: reminder rule runs only when a reminder field changed ----

	@Test
	void update_reminderRuleRuns_whenExpirationDateChanged() {
		Item stored = repository.save(storedItem(TODAY.plusDays(60), true, 30));

		assertUpdateRejectedWith(stored.getId(), requestFrom(stored).expirationDate(TODAY.plusDays(20)).build(),
				Map.of("daysBeforeReminder", "טווח התזכורת ארוך מהזמן שנשאר עד התוקף, אנא בחרי טווח קצר יותר"));
		assertThat(reloaded(stored.getId()).getExpirationDate()).isEqualTo(TODAY.plusDays(60));
	}

	@Test
	void update_reminderRuleRuns_whenReminderTurnedOn() {
		Item stored = repository.save(storedItem(TODAY.plusDays(20), false, 30));

		assertUpdateRejectedWith(stored.getId(), requestFrom(stored).reminderEnabled(true).build(),
				Map.of("daysBeforeReminder", "טווח התזכורת ארוך מהזמן שנשאר עד התוקף, אנא בחרי טווח קצר יותר"));
		assertThat(reloaded(stored.getId()).isReminderEnabled()).isFalse();
	}

	@Test
	void update_reminderRuleRuns_whenDaysBeforeReminderChanged() {
		Item stored = repository.save(storedItem(TODAY.plusDays(20), true, 10));

		assertUpdateRejectedWith(stored.getId(), requestFrom(stored).daysBeforeReminder(25).build(),
				Map.of("daysBeforeReminder", "טווח התזכורת ארוך מהזמן שנשאר עד התוקף, אנא בחרי טווח קצר יותר"));
		assertThat(reloaded(stored.getId()).getDaysBeforeReminder()).isEqualTo(10);
	}

	@Test
	void update_acceptsDaysBeforeReminderChangedToLessThanDaysRemaining() {
		Item stored = repository.save(storedItem(TODAY.plusDays(20), true, 10));

		service.update(stored.getId(), requestFrom(stored).daysBeforeReminder(19).build());

		assertThat(reloaded(stored.getId()).getDaysBeforeReminder()).isEqualTo(19);
	}

	@Test
	void update_reminderRuleSkipped_whenExpirationDateHasPassed() {
		Item stored = repository.save(storedItem(TODAY.minusDays(5), true, 7));

		service.update(stored.getId(), requestFrom(stored).daysBeforeReminder(30).build());

		assertThat(reloaded(stored.getId()).getDaysBeforeReminder()).isEqualTo(30);
	}

	@Test
	void update_daysBeforeReminderNotSent_countsAs30_andIsChecked() {
		Item stored = repository.save(storedItem(TODAY.plusDays(20), true, 14));

		assertUpdateRejectedWith(stored.getId(), requestFrom(stored).daysBeforeReminder(null).build(),
				Map.of("daysBeforeReminder", "טווח התזכורת ארוך מהזמן שנשאר עד התוקף, אנא בחרי טווח קצר יותר"));
		assertThat(reloaded(stored.getId()).getDaysBeforeReminder()).isEqualTo(14);
	}

	@Test
	void update_daysBeforeReminderNotSent_isSavedAs30() {
		Item stored = repository.save(storedItem(TODAY.plusDays(60), true, 14));

		service.update(stored.getId(), requestFrom(stored).daysBeforeReminder(null).build());

		assertThat(reloaded(stored.getId()).getDaysBeforeReminder()).isEqualTo(30);
	}

	// ---- update: replacing the item ----

	@Test
	void update_redeemedNotSent_keepsStoredValue() {
		Item stored = storedItem(TODAY.plusDays(60), false, 30);
		stored.setRedeemed(true);
		repository.save(stored);

		service.update(stored.getId(), requestFrom(stored).redeemed(null).build());

		assertThat(reloaded(stored.getId()).isRedeemed()).isTrue();
	}

	@Test
	void update_redeemedSentFalse_isSaved() {
		Item stored = storedItem(TODAY.plusDays(60), false, 30);
		stored.setRedeemed(true);
		repository.save(stored);

		service.update(stored.getId(), requestFrom(stored).redeemed(false).build());

		assertThat(reloaded(stored.getId()).isRedeemed()).isFalse();
	}

	@Test
	void update_optionalFieldsNotSent_areCleared() {
		Item stored = storedItem(TODAY.plusDays(60), false, 30);
		stored.setAmount(new BigDecimal("200"));
		stored.setCurrentAmount(new BigDecimal("150"));
		stored.setLastFourDigits("1234");
		stored.setAcceptedStores("Fox, Castro");
		repository.save(stored);

		service.update(stored.getId(), validRequest().build());

		Item item = reloaded(stored.getId());
		assertThat(item.getAmount()).isNull();
		assertThat(item.getCurrentAmount()).isNull();
		assertThat(item.getLastFourDigits()).isNull();
		assertThat(item.getAcceptedStores()).isNull();
	}

	@Test
	void update_savesAllSentFields_andKeepsTheSameId() {
		Item stored = repository.save(storedItem(TODAY.plusDays(60), false, 30));
		ItemRequest request = validRequest()
				.category(Category.INSURANCE).name("Car insurance").expirationDate(TODAY.plusDays(90))
				.amount("3200").currentAmount("3000").lastFourDigits("5678").acceptedStores("Any")
				.reminderEnabled(true).daysBeforeReminder(14).redeemed(true).build();

		ItemResponse response = service.update(stored.getId(), request);

		assertThat(response.id()).isEqualTo(stored.getId());
		assertThat(repository.count()).isEqualTo(1);
		Item item = reloaded(stored.getId());
		assertThat(item.getCategory()).isEqualTo(Category.INSURANCE);
		assertThat(item.getName()).isEqualTo("Car insurance");
		assertThat(item.getExpirationDate()).isEqualTo(TODAY.plusDays(90));
		assertThat(item.getAmount()).isEqualByComparingTo("3200");
		assertThat(item.getCurrentAmount()).isEqualByComparingTo("3000");
		assertThat(item.getLastFourDigits()).isEqualTo("5678");
		assertThat(item.getAcceptedStores()).isEqualTo("Any");
		assertThat(item.isReminderEnabled()).isTrue();
		assertThat(item.getDaysBeforeReminder()).isEqualTo(14);
		assertThat(item.isRedeemed()).isTrue();
	}

	// ---- update: rules that are always checked ----

	@Test
	void update_rejectsNameWithOnlySpaces() {
		Item stored = repository.save(storedItem(TODAY.plusDays(60), false, 30));

		assertUpdateRejectedWith(stored.getId(), requestFrom(stored).name("   ").build(),
				Map.of("name", "יש להזין שם"));
		assertThat(reloaded(stored.getId()).getName()).isEqualTo("Stored item");
	}

	@Test
	void update_rejectsNegativeAmount() {
		Item stored = repository.save(storedItem(TODAY.plusDays(60), false, 30));

		assertUpdateRejectedWith(stored.getId(), requestFrom(stored).amount("-1").build(),
				Map.of("amount", "הסכום לא יכול להיות שלילי"));
		assertThat(reloaded(stored.getId()).getAmount()).isNull();
	}

	@Test
	void update_rejectsInvalidLastFourDigits() {
		Item stored = repository.save(storedItem(TODAY.plusDays(60), false, 30));

		assertUpdateRejectedWith(stored.getId(), requestFrom(stored).lastFourDigits("12").build(),
				Map.of("lastFourDigits", "יש להזין בדיוק 4 ספרות"));
		assertThat(reloaded(stored.getId()).getLastFourDigits()).isNull();
	}

	@Test
	void update_returnsAllErrorsTogether_andKeepsStoredItem() {
		Item stored = repository.save(storedItem(TODAY.plusDays(60), false, 30));
		ItemRequest request = requestFrom(stored)
				.name(" ").expirationDate(TODAY.minusDays(1)).amount("-5").lastFourDigits("12").build();

		assertUpdateRejectedWith(stored.getId(), request, Map.of(
				"name", "יש להזין שם",
				"expirationDate", "התאריך כבר עבר, אנא הזיני תאריך עתידי",
				"amount", "הסכום לא יכול להיות שלילי",
				"lastFourDigits", "יש להזין בדיוק 4 ספרות"));
		Item item = reloaded(stored.getId());
		assertThat(item.getName()).isEqualTo("Stored item");
		assertThat(item.getExpirationDate()).isEqualTo(TODAY.plusDays(60));
		assertThat(item.getAmount()).isNull();
		assertThat(item.getLastFourDigits()).isNull();
	}

	private void assertUpdateRejectedWith(Long id, ItemRequest request, Map<String, String> expectedFieldErrors) {
		assertThatThrownBy(() -> service.update(id, request))
				.isInstanceOfSatisfying(InvalidItemException.class,
						exception -> assertThat(exception.getFieldErrors()).containsExactlyInAnyOrderEntriesOf(expectedFieldErrors));
		assertThat(repository.count()).isEqualTo(1);
	}

	// Writes pending changes to the database and empties the persistence context, so the item is read
	// from the database and not from the cache. This is what proves a rejected update was not saved.
	private Item reloaded(Long id) {
		entityManager.flush();
		entityManager.clear();
		return repository.findById(id).orElseThrow();
	}

	private Item storedItem(LocalDate expirationDate, boolean reminderEnabled, Integer daysBeforeReminder) {
		Item item = new Item(Category.GIFT_CARD, "Stored item", expirationDate, reminderEnabled);
		item.setDaysBeforeReminder(daysBeforeReminder);
		return item;
	}

	// A request equal to the stored item, so each update test changes only the field it checks.
	// redeemed is left unset (not sent).
	private static RequestBuilder requestFrom(Item item) {
		RequestBuilder builder = new RequestBuilder();
		builder.category = item.getCategory();
		builder.name = item.getName();
		builder.expirationDate = item.getExpirationDate();
		builder.amount = item.getAmount();
		builder.currentAmount = item.getCurrentAmount();
		builder.lastFourDigits = item.getLastFourDigits();
		builder.acceptedStores = item.getAcceptedStores();
		builder.reminderEnabled = item.isReminderEnabled();
		builder.daysBeforeReminder = item.getDaysBeforeReminder();
		return builder;
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
