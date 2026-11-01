package com.coral.expirytracker.item;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemResponse(
		Long id,
		Category category,
		String name,
		LocalDate expirationDate,
		BigDecimal amount,
		BigDecimal currentAmount,
		String lastFourDigits,
		String acceptedStores,
		boolean reminderEnabled,
		Integer daysBeforeReminder,
		boolean redeemed) {

	public static ItemResponse from(Item item) {
		return new ItemResponse(
				item.getId(),
				item.getCategory(),
				item.getName(),
				item.getExpirationDate(),
				item.getAmount(),
				item.getCurrentAmount(),
				item.getLastFourDigits(),
				item.getAcceptedStores(),
				item.isReminderEnabled(),
				item.getDaysBeforeReminder(),
				item.isRedeemed());
	}

}
