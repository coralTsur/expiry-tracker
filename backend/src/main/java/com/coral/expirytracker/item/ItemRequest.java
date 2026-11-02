package com.coral.expirytracker.item;

import java.math.BigDecimal;
import java.time.LocalDate;

// spec section 8: the request has the same fields as the item, without id. The server sets the id.
// reminderEnabled and redeemed are Boolean, not boolean, so a value that was not sent stays null instead of false.
public record ItemRequest(
		Category category,
		String name,
		LocalDate expirationDate,
		BigDecimal amount,
		BigDecimal currentAmount,
		String lastFourDigits,
		String acceptedStores,
		Boolean reminderEnabled,
		Integer daysBeforeReminder,
		Boolean redeemed) {
}
