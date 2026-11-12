import { describe, test, expect } from 'vitest';
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
