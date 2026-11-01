package com.coral.expirytracker.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

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

	private static Clock clockAt(LocalDate date) {
		return Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
	}

	private Item item(String name, LocalDate expirationDate) {
		return new Item(Category.GIFT_CARD, name, expirationDate, false);
	}

}
