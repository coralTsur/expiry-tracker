// Helpers for the item form: initial values, form values -> request body, and client-side validation.

import { daysBetween, todayString } from './dates';

// The dropdown values for "days before reminder" (spec section 3). Any other number is "אחר".
export const DAYS_PRESETS = ['7', '14', '30'];

// Inputs need strings: React warns when a controlled input gets null.
// reminderEnabled is null until the user picks "כן" or "לא" (spec section 3: required, no default).
export const EMPTY_ITEM_VALUES = {
  category: '',
  name: '',
  expirationDate: '',
  amount: '',
  currentAmount: '',
  lastFourDigits: '',
  acceptedStores: '',
  reminderEnabled: null,
  daysBeforeReminder: '30', // spec section 3: default 30.
  redeemed: false, // spec section 3: default "לא".
};

// The server's default for daysBeforeReminder that was not sent (spec section 8).
const DEFAULT_DAYS_BEFORE_REMINDER = 30;

// Server item -> form values. Inputs need strings, so numbers become strings and null becomes ''.
// daysBeforeReminder must be a string too: DAYS_PRESETS holds strings, so the number 14 would show as "אחר".
export function toFormValues(item) {
  return {
    category: item.category ?? '',
    name: item.name ?? '',
    expirationDate: item.expirationDate ?? '',
    amount: numberToText(item.amount),
    currentAmount: numberToText(item.currentAmount),
    lastFourDigits: item.lastFourDigits ?? '',
    acceptedStores: item.acceptedStores ?? '',
    reminderEnabled: item.reminderEnabled,
    daysBeforeReminder: numberToText(item.daysBeforeReminder),
    redeemed: item.redeemed,
  };
}

function numberToText(value) {
  return value === null || value === undefined ? '' : String(value);
}

// spec section 8, "error messages by field" and "client-side only error messages". Copied exactly.
const NAME_MISSING = 'יש להזין שם';
const CATEGORY_MISSING = 'יש לבחור קטגוריה';
const EXPIRATION_DATE_MISSING = 'יש להזין תאריך תוקף';
const EXPIRATION_DATE_NOT_IN_FUTURE = 'התאריך כבר עבר, אנא הזיני תאריך עתידי';
const REMINDER_ENABLED_MISSING = 'יש לבחור אם להפעיל תזכורת';
const REMINDER_TOO_LONG = 'טווח התזכורת ארוך מהזמן שנשאר עד התוקף, אנא בחרי טווח קצר יותר';
const DAYS_NEGATIVE = 'מספר הימים לא יכול להיות שלילי';
const DAYS_MISSING = 'יש להזין מספר ימים';
const DAYS_NOT_WHOLE = 'יש להזין מספר שלם';
const AMOUNT_NEGATIVE = 'הסכום לא יכול להיות שלילי';
const CURRENT_AMOUNT_ABOVE_AMOUNT = 'הסכום העדכני לא יכול להיות גדול מהסכום המקורי';
const LAST_FOUR_DIGITS_INVALID = 'יש להזין בדיוק 4 ספרות';

function isWholeNumberAtLeastZero(text) {
  if (text === '') {
    return false;
  }
  const number = Number(text);
  return Number.isInteger(number) && number >= 0;
}

// The days value the way the server compares it: '' is not sent, so it counts as the default of 30.
function daysForComparison(text) {
  return text === '' ? DEFAULT_DAYS_BEFORE_REMINDER : Number(text);
}

// checkRange: false on an update where none of the reminder fields changed (spec section 5).
function validateDaysBeforeReminder(values, today, checkRange) {
  if (values.daysBeforeReminder === '') {
    return DAYS_MISSING;
  }
  const days = Number(values.daysBeforeReminder);
  if (!Number.isInteger(days)) {
    return DAYS_NOT_WHOLE;
  }
  if (days < 0) {
    return DAYS_NEGATIVE;
  }
  // spec section 5: when the date is missing or already passed, this rule is skipped.
  const dateInFuture = values.expirationDate !== '' && values.expirationDate > today;
  if (checkRange && dateInFuture && days >= daysBetween(today, values.expirationDate)) {
    return REMINDER_TOO_LONG;
  }
  return null;
}

// Returns { fieldName: message } for every field with a problem. An empty object means the form is valid.
// The same rules as the server (spec sections 5 and 8). If the two disagree, the server decides.
// originalValues: the values the form started with. On the add screen they are EMPTY_ITEM_VALUES,
// so every field counts as changed and the date and reminder rules always run, as spec section 5 requires.
export function validateItem(values, originalValues) {
  const errors = {};
  const today = todayString();

  // spec section 5, update: the date rule runs only if expirationDate changed, and the reminder rule
  // only if expirationDate, reminderEnabled or daysBeforeReminder changed. Same comparison as the server.
  const dateChanged = values.expirationDate !== originalValues.expirationDate;
  const reminderFieldsChanged =
    dateChanged ||
    values.reminderEnabled !== originalValues.reminderEnabled ||
    daysForComparison(values.daysBeforeReminder) !==
      daysForComparison(originalValues.daysBeforeReminder);

  // spec section 5: a name with only spaces counts as missing.
  if (values.name.trim() === '') {
    errors.name = NAME_MISSING;
  }

  if (values.category === '') {
    errors.category = CATEGORY_MISSING;
  }

  // yyyy-MM-dd strings sort like dates, so they can be compared directly.
  if (values.expirationDate === '') {
    errors.expirationDate = EXPIRATION_DATE_MISSING;
  } else if (dateChanged && values.expirationDate <= today) {
    errors.expirationDate = EXPIRATION_DATE_NOT_IN_FUTURE;
  }

  if (values.reminderEnabled === null) {
    errors.reminderEnabled = REMINDER_ENABLED_MISSING;
  }

  // The days field is hidden when the reminder is not "כן", so it is not checked either.
  if (values.reminderEnabled === true) {
    const daysError = validateDaysBeforeReminder(values, today, reminderFieldsChanged);
    if (daysError) {
      errors.daysBeforeReminder = daysError;
    }
  }

  // spec section 5: 0 is allowed. An empty optional field is not checked.
  if (values.amount !== '' && Number(values.amount) < 0) {
    errors.amount = AMOUNT_NEGATIVE;
  }

  if (values.currentAmount !== '' && Number(values.currentAmount) < 0) {
    errors.currentAmount = AMOUNT_NEGATIVE;
  } else if (
    values.currentAmount !== '' &&
    values.amount !== '' &&
    Number(values.currentAmount) > Number(values.amount)
  ) {
    // spec section 5: checked only when both amounts are filled.
    errors.currentAmount = CURRENT_AMOUNT_ABOVE_AMOUNT;
  }

  // No trim, the same as the server.
  if (values.lastFourDigits !== '' && !/^[0-9]{4}$/.test(values.lastFourDigits)) {
    errors.lastFourDigits = LAST_FOUR_DIGITS_INVALID;
  }

  return errors;
}

// spec section 8: an empty field is sent as null, not as "".
function emptyToNull(text) {
  return text === '' ? null : text;
}

// Checked before Number(), because Number('') is 0, not null.
function toNumberOrNull(text) {
  return text === '' ? null : Number(text);
}

// With the reminder off the days field is hidden, but its value is still saved by the server.
// Sending null would make the server store 30 and lose a saved value such as 14, so the hidden value
// is sent if it is valid. If it is not, the user cannot see it to fix it, so the original value is sent.
// On the add screen the original is '30', the same as the server's default.
function daysToSend(values, originalValues) {
  if (values.reminderEnabled === true || isWholeNumberAtLeastZero(values.daysBeforeReminder)) {
    // With the reminder on, validateItem has already checked the value.
    return Number(values.daysBeforeReminder);
  }
  return toNumberOrNull(originalValues.daysBeforeReminder);
}

// Form values (strings) -> the request body of spec section 8. PUT sends the full item, so every field is here.
// id is not sent: it comes from the path or the server sets it.
export function toRequestBody(values, originalValues) {
  return {
    category: emptyToNull(values.category),
    name: emptyToNull(values.name),
    expirationDate: emptyToNull(values.expirationDate),
    amount: toNumberOrNull(values.amount),
    currentAmount: toNumberOrNull(values.currentAmount),
    lastFourDigits: emptyToNull(values.lastFourDigits),
    acceptedStores: emptyToNull(values.acceptedStores),
    reminderEnabled: values.reminderEnabled,
    daysBeforeReminder: daysToSend(values, originalValues),
    redeemed: values.redeemed,
  };
}
