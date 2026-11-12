import { describe, test, expect } from 'vitest';
import { getItemStatus } from './itemStatus';

// A fixed "today", so the tests give the same result on any day they run.
const TODAY = '2026-11-12';

// Builds an item with the spec field names. getItemStatus only reads these two fields.
function makeItem(redeemed, expirationDate) {
  return { redeemed, expirationDate };
}

describe('getItemStatus', () => {
  test('a redeemed item is REDEEMED even when it has expired', () => {
    expect(getItemStatus(makeItem(true, '2026-11-11'), TODAY)).toBe('REDEEMED');
  });

  test('an item that expired yesterday is EXPIRED', () => {
    expect(getItemStatus(makeItem(false, '2026-11-11'), TODAY)).toBe('EXPIRED');
  });

  test('an item that expires today is EXPIRING_SOON', () => {
    expect(getItemStatus(makeItem(false, '2026-11-12'), TODAY)).toBe('EXPIRING_SOON');
  });

  test('an item that expires in exactly 30 days is EXPIRING_SOON', () => {
    expect(getItemStatus(makeItem(false, '2026-12-12'), TODAY)).toBe('EXPIRING_SOON');
  });

  test('an item that expires in 31 days is VALID', () => {
    expect(getItemStatus(makeItem(false, '2026-12-13'), TODAY)).toBe('VALID');
  });
});
