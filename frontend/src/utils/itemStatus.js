// The expiry status shown on each table row (spec sections 5 and 9).
// It is computed on every render from the item and today's date, and never stored,
// so it is always correct even when the day changes while the item stays the same.

import { daysBetween, todayString } from './dates';

// spec section 5. The server has the same number (ItemService.EXPIRING_SOON_DAYS); change both together.
export const EXPIRING_SOON_DAYS = 30;

// Hebrew labels from spec section 5.
export const STATUS_LABELS = {
  REDEEMED: 'מומש',
  EXPIRED: 'פג',
  EXPIRING_SOON: 'פג בקרוב',
  VALID: 'בתוקף',
};

// The checks run in the order spec section 5 lists them, and the first match wins.
// So a redeemed item shows "מומש" even when it has also expired.
// today is a parameter so a test can pass a fixed date instead of depending on the real clock.
export function getItemStatus(item, today = todayString()) {
  if (item.redeemed) return 'REDEEMED';
  const daysLeft = daysBetween(today, item.expirationDate);
  if (daysLeft < 0) return 'EXPIRED';
  // Inclusive on both ends (today counts as 0 days left), the same rule as the "פג בקרוב" screen.
  if (daysLeft <= EXPIRING_SOON_DAYS) return 'EXPIRING_SOON';
  return 'VALID';
}
