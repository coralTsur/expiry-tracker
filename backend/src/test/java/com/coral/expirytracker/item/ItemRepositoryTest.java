package com.coral.expirytracker.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class ItemRepositoryTest {

	private static final LocalDate TODAY = LocalDate.of(2030, 1, 15);

	@Autowired
	private ItemRepository repository;

	@BeforeEach
	void removeSampleData() {
		repository.deleteAll();
	}

	@Test
	void returnsItemsSortedByExpirationDate() {
		repository.save(item("In 20 days", TODAY.plusDays(20)));
		repository.save(item("In 5 days", TODAY.plusDays(5)));
		repository.save(item("In 10 days", TODAY.plusDays(10)));

		assertThat(findExpiringWithin30Days())
				.extracting(Item::getName)
				.containsExactly("In 5 days", "In 10 days", "In 20 days");
	}

	@Test
	void includesBothBoundaries() {
		repository.save(item("Today", TODAY));
		repository.save(item("In 30 days", TODAY.plusDays(30)));

		assertThat(findExpiringWithin30Days())
				.extracting(Item::getName)
				.containsExactly("Today", "In 30 days");
	}

	@Test
	void excludesItemsOutsideTheWindow() {
		repository.save(item("Yesterday", TODAY.minusDays(1)));
		repository.save(item("In 31 days", TODAY.plusDays(31)));

		assertThat(findExpiringWithin30Days()).isEmpty();
	}

	@Test
	void excludesRedeemedItems() {
		Item redeemedItem = item("Redeemed", TODAY.plusDays(5));
		redeemedItem.setRedeemed(true);
		repository.save(redeemedItem);

		assertThat(findExpiringWithin30Days()).isEmpty();
	}

	private List<Item> findExpiringWithin30Days() {
		return repository.findByRedeemedFalseAndExpirationDateBetweenOrderByExpirationDateAsc(TODAY, TODAY.plusDays(30));
	}

	private Item item(String name, LocalDate expirationDate) {
		return new Item(Category.GIFT_CARD, name, expirationDate, false);
	}

}
