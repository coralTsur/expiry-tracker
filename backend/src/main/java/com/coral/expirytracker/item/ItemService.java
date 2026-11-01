package com.coral.expirytracker.item;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ItemService {

	private static final int EXPIRING_SOON_DAYS = 30;

	private final ItemRepository repository;
	private final Clock clock;

	public ItemService(ItemRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	public List<ItemResponse> findAll() {
		// spec section 5: the full list includes expired and redeemed items, so there is no filter.
		// spec section 8: sorted by expirationDate, nearest to furthest.
		return toResponses(repository.findAll(Sort.by(Sort.Direction.ASC, "expirationDate")));
	}

	public ItemResponse findById(Long id) {
		// spec section 8: no item with this id is a 404, which the exception handler produces.
		return repository.findById(id)
				.map(ItemResponse::from)
				.orElseThrow(() -> new ItemNotFoundException(id));
	}

	public List<ItemResponse> findExpiringSoon() {
		// spec section 5, "expiring soon": not redeemed, and expiring from today to 30 days from today, inclusive.
		// Reminder settings are deliberately not part of this rule.
		// spec section 8: sorted by expirationDate. The query covers the filter and the sort.
		LocalDate today = LocalDate.now(clock);
		return toResponses(repository.findByRedeemedFalseAndExpirationDateBetweenOrderByExpirationDateAsc(
				today, today.plusDays(EXPIRING_SOON_DAYS)));
	}

	private List<ItemResponse> toResponses(List<Item> items) {
		return items.stream().map(ItemResponse::from).toList();
	}

}
