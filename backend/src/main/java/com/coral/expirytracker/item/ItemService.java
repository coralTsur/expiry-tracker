package com.coral.expirytracker.item;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ItemService {

	private static final int EXPIRING_SOON_DAYS = 30;
	private static final int DEFAULT_DAYS_BEFORE_REMINDER = 30;

	// spec section 8, "error messages by field": copied exactly.
	private static final String NAME_MISSING = "יש להזין שם";
	private static final String CATEGORY_MISSING = "יש לבחור קטגוריה";
	private static final String EXPIRATION_DATE_MISSING = "יש להזין תאריך תוקף";
	private static final String EXPIRATION_DATE_NOT_IN_FUTURE = "התאריך כבר עבר, אנא הזיני תאריך עתידי";
	private static final String REMINDER_ENABLED_MISSING = "יש לבחור אם להפעיל תזכורת";
	private static final String REMINDER_TOO_LONG = "טווח התזכורת ארוך מהזמן שנשאר עד התוקף, אנא בחרי טווח קצר יותר";
	private static final String DAYS_NEGATIVE = "מספר הימים לא יכול להיות שלילי";
	private static final String AMOUNT_NEGATIVE = "הסכום לא יכול להיות שלילי";
	private static final String CURRENT_AMOUNT_ABOVE_AMOUNT = "הסכום העדכני לא יכול להיות גדול מהסכום המקורי";
	private static final String LAST_FOUR_DIGITS_INVALID = "יש להזין בדיוק 4 ספרות";

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

	public ItemResponse create(ItemRequest request) {
		// Read "today" once, so every date rule in this request uses the same day.
		LocalDate today = LocalDate.now(clock);
		int daysBeforeReminder = daysBeforeReminderOrDefault(request);

		// spec section 5: on create, the date rule and the reminder rule are always checked.
		throwIfInvalid(validate(request, daysBeforeReminder, today, true, true));

		Item item = new Item(request.category(), request.name(), request.expirationDate(), request.reminderEnabled());
		copyRequestFields(request, daysBeforeReminder, item);
		// spec section 8: the request has no id, so the database always generates a new one.
		return ItemResponse.from(repository.save(item));
	}

	public ItemResponse update(Long id, ItemRequest request) {
		// spec section 8: no item with this id is a 404. Checked first, before any validation.
		Item item = repository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));

		// Read "today" once, so every date rule in this request uses the same day.
		LocalDate today = LocalDate.now(clock);
		int daysBeforeReminder = daysBeforeReminderOrDefault(request);

		// "Changed" means different from the stored item. daysBeforeReminder is compared after the default of 30,
		// so a value that was not sent counts as 30.
		boolean expirationDateChanged = !Objects.equals(request.expirationDate(), item.getExpirationDate());
		boolean reminderFieldsChanged = expirationDateChanged
				|| !Objects.equals(request.reminderEnabled(), item.isReminderEnabled())
				|| !Objects.equals(daysBeforeReminder, item.getDaysBeforeReminder());

		// spec section 5: on update, the date rule runs only if expirationDate changed, and the reminder rule
		// runs only if expirationDate, reminderEnabled or daysBeforeReminder changed.
		throwIfInvalid(validate(request, daysBeforeReminder, today, expirationDateChanged, reminderFieldsChanged));

		// Every rule passed, so only now is the stored item changed.
		copyRequestFields(request, daysBeforeReminder, item);
		return ItemResponse.from(repository.save(item));
	}

	// spec sections 3 and 8: daysBeforeReminder that was not sent gets 30, also when the reminder is off.
	// The default is applied before validation, so the rules check the value that will be saved.
	private static int daysBeforeReminderOrDefault(ItemRequest request) {
		return request.daysBeforeReminder() != null ? request.daysBeforeReminder() : DEFAULT_DAYS_BEFORE_REMINDER;
	}

	private static void throwIfInvalid(Map<String, String> fieldErrors) {
		if (!fieldErrors.isEmpty()) {
			// spec section 5: when there is an error, the item is not saved.
			throw new InvalidItemException(fieldErrors);
		}
	}

	// spec section 8: every problem is collected first, so all errors are returned together.
	// checkDateRule and checkReminderRule are always true on create. On update they depend on what changed.
	private Map<String, String> validate(ItemRequest request, int daysBeforeReminder, LocalDate today,
			boolean checkDateRule, boolean checkReminderRule) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();

		// spec section 3: name is required. spec section 5: a name with only spaces counts as missing.
		if (request.name() == null || request.name().isBlank()) {
			fieldErrors.put("name", NAME_MISSING);
		}

		// spec section 3: category is required.
		if (request.category() == null) {
			fieldErrors.put("category", CATEGORY_MISSING);
		}

		// spec section 3: expirationDate is required.
		// spec section 5: it must be in the future (from tomorrow on), when the date rule is checked.
		boolean expirationDateInFuture = request.expirationDate() != null && request.expirationDate().isAfter(today);
		if (request.expirationDate() == null) {
			fieldErrors.put("expirationDate", EXPIRATION_DATE_MISSING);
		} else if (checkDateRule && !expirationDateInFuture) {
			fieldErrors.put("expirationDate", EXPIRATION_DATE_NOT_IN_FUTURE);
		}

		// spec section 3: reminderEnabled is required.
		if (request.reminderEnabled() == null) {
			fieldErrors.put("reminderEnabled", REMINDER_ENABLED_MISSING);
		}

		// spec section 5: days before reminder is a whole number, 0 or more.
		if (daysBeforeReminder < 0) {
			fieldErrors.put("daysBeforeReminder", DAYS_NEGATIVE);
		} else if (checkReminderRule && Boolean.TRUE.equals(request.reminderEnabled()) && expirationDateInFuture) {
			// spec section 5: with the reminder on, days before reminder must be less than the days left
			// until expiration. Equal is rejected, because the rule says "less than".
			// spec section 5: when the date is missing or already passed, this rule is skipped.
			long daysRemaining = ChronoUnit.DAYS.between(today, request.expirationDate());
			if (daysBeforeReminder >= daysRemaining) {
				fieldErrors.put("daysBeforeReminder", REMINDER_TOO_LONG);
			}
		}

		// spec section 5: amount and current amount cannot be negative. 0 is allowed.
		if (isNegative(request.amount())) {
			fieldErrors.put("amount", AMOUNT_NEGATIVE);
		}
		if (isNegative(request.currentAmount())) {
			fieldErrors.put("currentAmount", AMOUNT_NEGATIVE);
		} else if (request.currentAmount() != null && request.amount() != null
				&& request.currentAmount().compareTo(request.amount()) > 0) {
			// spec section 5: current amount cannot be greater than the amount, checked only when both are filled.
			// compareTo, not equals, so 200 and 200.00 count as the same amount.
			fieldErrors.put("currentAmount", CURRENT_AMOUNT_ABOVE_AMOUNT);
		}

		// spec section 5: if lastFourDigits is filled, it must be exactly 4 digits.
		String lastFourDigits = emptyToNull(request.lastFourDigits());
		if (lastFourDigits != null && !lastFourDigits.matches("[0-9]{4}")) {
			fieldErrors.put("lastFourDigits", LAST_FOUR_DIGITS_INVALID);
		}

		return fieldErrors;
	}

	// Shared by create and update. Called only after validation passed.
	private void copyRequestFields(ItemRequest request, int daysBeforeReminder, Item item) {
		item.setCategory(request.category());
		item.setName(request.name());
		item.setExpirationDate(request.expirationDate());
		item.setReminderEnabled(request.reminderEnabled());
		// spec section 8: an optional field that was not sent is saved empty. On PUT this clears the stored value.
		item.setAmount(request.amount());
		item.setCurrentAmount(request.currentAmount());
		// spec section 5: an optional text field sent empty is saved empty (null).
		item.setLastFourDigits(emptyToNull(request.lastFourDigits()));
		item.setAcceptedStores(emptyToNull(request.acceptedStores()));
		item.setDaysBeforeReminder(daysBeforeReminder);
		// spec section 8: redeemed that was not sent keeps its value: false for a new item (the entity default),
		// the stored value on update. A value that was sent is saved as sent.
		if (request.redeemed() != null) {
			item.setRedeemed(request.redeemed());
		}
	}

	private static boolean isNegative(BigDecimal value) {
		return value != null && value.signum() < 0;
	}

	private static String emptyToNull(String value) {
		return value == null || value.isEmpty() ? null : value;
	}

	private List<ItemResponse> toResponses(List<Item> items) {
		return items.stream().map(ItemResponse::from).toList();
	}

}
