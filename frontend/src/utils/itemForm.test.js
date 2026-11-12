import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest';
import { validateItem, EMPTY_ITEM_VALUES } from './itemForm';

// A valid add-screen form. The far-future date and reminderEnabled false keep the date and
// reminder rules quiet, so each test checks one non-date rule. Amounts are empty, so the
// currentAmount rule is skipped unless a test fills both amounts.
const VALID_VALUES = {
  ...EMPTY_ITEM_VALUES,
  category: 'GIFT_CARD',
  name: 'BuyMe',
  expirationDate: '2099-12-31',
  reminderEnabled: false,
};

// On the add screen the form starts from EMPTY_ITEM_VALUES.
function validate(changes) {
  return validateItem({ ...VALID_VALUES, ...changes }, EMPTY_ITEM_VALUES);
}

describe('validateItem', () => {
  test('a valid form has no errors', () => {
    expect(validate({})).toStrictEqual({});
  });

  test('a name with only spaces is missing', () => {
    expect(validate({ name: '   ' }).name).toBe('יש להזין שם');
  });

  test('lastFourDigits with 3 digits is an error', () => {
    expect(validate({ lastFourDigits: '123' }).lastFourDigits).toBe('יש להזין בדיוק 4 ספרות');
  });

  test('lastFourDigits with exactly 4 digits has no error', () => {
    expect(validate({ lastFourDigits: '1234' })).not.toHaveProperty('lastFourDigits');
  });

  test('amount 0 has no error', () => {
    expect(validate({ amount: '0' })).not.toHaveProperty('amount');
  });

  test('a negative amount is an error', () => {
    expect(validate({ amount: '-1' }).amount).toBe('הסכום לא יכול להיות שלילי');
  });

  test('currentAmount equal to amount has no error', () => {
    expect(validate({ amount: '100', currentAmount: '100' })).not.toHaveProperty('currentAmount');
  });

  test('currentAmount above amount is an error', () => {
    expect(validate({ amount: '100', currentAmount: '101' }).currentAmount).toBe(
      'הסכום העדכני לא יכול להיות גדול מהסכום המקורי',
    );
  });
});

describe('validateItem date rules (today is 2026-11-12)', () => {
  // validateItem reads today from the real clock, so the clock is frozen to keep the results stable.
  // Local noon: todayString reads local date parts, so today is 2026-11-12 in any time zone.
  // Month 10 is November, because Date months start at 0.
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 10, 12, 12, 0));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  test('a date in the past is an error', () => {
    expect(validate({ expirationDate: '2026-11-11' }).expirationDate).toBe(
      'התאריך כבר עבר, אנא הזיני תאריך עתידי',
    );
  });

  test('today is an error', () => {
    expect(validate({ expirationDate: '2026-11-12' }).expirationDate).toBe(
      'התאריך כבר עבר, אנא הזיני תאריך עתידי',
    );
  });

  test('tomorrow has no error', () => {
    expect(validate({ expirationDate: '2026-11-13' })).not.toHaveProperty('expirationDate');
  });

  test('a reminder shorter than the days left has no error', () => {
    // 2026-11-12 to 2026-12-12 is 30 days, because November has 30 days.
    expect(
      validate({ reminderEnabled: true, expirationDate: '2026-12-12', daysBeforeReminder: '29' }),
    ).not.toHaveProperty('daysBeforeReminder');
  });

  test('a reminder equal to the days left is an error', () => {
    expect(
      validate({ reminderEnabled: true, expirationDate: '2026-12-12', daysBeforeReminder: '30' })
        .daysBeforeReminder,
    ).toBe('טווח התזכורת ארוך מהזמן שנשאר עד התוקף, אנא בחרי טווח קצר יותר');
  });

  test('on edit, a past date that did not change has no error', () => {
    // The saved item as the edit screen holds it (toFormValues). Its date has already passed.
    // Spec section 7: the change is saved, and the form saves only when there are no errors at all.
    const originalValues = { ...VALID_VALUES, expirationDate: '2026-11-01' };
    const values = { ...originalValues, name: 'BuyMe 2' };
    expect(validateItem(values, originalValues)).toStrictEqual({});
  });
});
