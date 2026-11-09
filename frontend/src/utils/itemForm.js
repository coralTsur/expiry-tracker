// Helpers for the item form: initial values, form values -> request body, and client-side validation.

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
};

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

const MS_PER_DAY = 24 * 60 * 60 * 1000;

// Today in the browser's time zone, as yyyy-MM-dd, the same format as <input type="date">.
function todayString() {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

// Whole days from one yyyy-MM-dd date to another.
// Date.UTC ignores daylight-saving changes, so every day is exactly 24 hours long.
function daysBetween(fromDate, toDate) {
  const [fromYear, fromMonth, fromDay] = fromDate.split('-').map(Number);
  const [toYear, toMonth, toDay] = toDate.split('-').map(Number);
  const from = Date.UTC(fromYear, fromMonth - 1, fromDay);
  const to = Date.UTC(toYear, toMonth - 1, toDay);
  return Math.round((to - from) / MS_PER_DAY);
}

function validateDaysBeforeReminder(values, today) {
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
  if (dateInFuture && days >= daysBetween(today, values.expirationDate)) {
    return REMINDER_TOO_LONG;
  }
  return null;
}

// Returns { fieldName: message } for every field with a problem. An empty object means the form is valid.
// The same rules as the server (spec sections 5 and 8). If the two disagree, the server decides.
export function validateItem(values) {
  const errors = {};
  const today = todayString();

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
  } else if (values.expirationDate <= today) {
    errors.expirationDate = EXPIRATION_DATE_NOT_IN_FUTURE;
  }

  if (values.reminderEnabled === null) {
    errors.reminderEnabled = REMINDER_ENABLED_MISSING;
  }

  // The days field is hidden when the reminder is not "כן", so it is not checked either.
  if (values.reminderEnabled === true) {
    const daysError = validateDaysBeforeReminder(values, today);
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

// Form values (strings) -> the request body of spec section 8.
// id and redeemed are not sent: the server sets the id, and redeemed that is not sent is false on create.
export function toRequestBody(values) {
  return {
    category: emptyToNull(values.category),
    name: emptyToNull(values.name),
    expirationDate: emptyToNull(values.expirationDate),
    amount: toNumberOrNull(values.amount),
    currentAmount: toNumberOrNull(values.currentAmount),
    lastFourDigits: emptyToNull(values.lastFourDigits),
    acceptedStores: emptyToNull(values.acceptedStores),
    reminderEnabled: values.reminderEnabled,
    // With the reminder off, the hidden value is not sent, and the server stores its default of 30.
    daysBeforeReminder: values.reminderEnabled === true ? Number(values.daysBeforeReminder) : null,
  };
}
