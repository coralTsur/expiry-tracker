// @vitest-environment jsdom
// The line above gives this file a browser-like DOM (jsdom). The utils tests keep the faster node environment.
import { describe, test, expect, vi, afterEach } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ItemForm from './ItemForm';
import { EMPTY_ITEM_VALUES } from '../utils/itemForm';

// Globals are off, so Testing Library cannot register its own cleanup. Without it, each test's form stays in the page.
afterEach(() => {
  cleanup();
});

// The add screen renders the form this way (AddItemPage).
function renderAddForm() {
  const onSubmit = vi.fn();
  render(<ItemForm initialValues={EMPTY_ITEM_VALUES} submitLabel="הוסף" onSubmit={onSubmit} />);
  return { onSubmit };
}

// The texts of all shown error messages, in page order.
// The role "alert" does not take its accessible name from its text, so getByRole('alert', { name }) would not find it.
function alertTexts() {
  return screen.queryAllByRole('alert').map((alert) => alert.textContent);
}

describe('ItemForm on the add screen', () => {
  test('leaving the name field empty shows the name error', async () => {
    const user = userEvent.setup();
    renderAddForm();

    await user.click(screen.getByLabelText('שם'));
    await user.tab();

    expect(alertTexts()).toContain('יש להזין שם');
  });

  test('an invalid lastFourDigits shows no error while the user is still in the field', async () => {
    const user = userEvent.setup();
    renderAddForm();

    await user.click(screen.getByLabelText('4 ספרות אחרונות'));
    await user.keyboard('123');

    expect(alertTexts()).not.toContain('יש להזין בדיוק 4 ספרות');
  });

  test('an invalid lastFourDigits shows the error after leaving the field', async () => {
    const user = userEvent.setup();
    renderAddForm();

    await user.click(screen.getByLabelText('4 ספרות אחרונות'));
    await user.keyboard('123');
    await user.tab();

    expect(alertTexts()).toContain('יש להזין בדיוק 4 ספרות');
  });

  test('a shown name error disappears while the user types a valid name', async () => {
    const user = userEvent.setup();
    renderAddForm();

    await user.click(screen.getByLabelText('שם'));
    await user.tab();
    expect(alertTexts()).toContain('יש להזין שם');

    // Clicking back leaves the date field, so its own error appears. This test checks only the name error.
    await user.click(screen.getByLabelText('שם'));
    await user.keyboard('BuyMe');

    expect(alertTexts()).not.toContain('יש להזין שם');
  });

  test('submitting the empty form shows all required-field errors and does not call onSubmit', async () => {
    const user = userEvent.setup();
    const { onSubmit } = renderAddForm();

    await user.click(screen.getByRole('button', { name: 'הוסף' }));

    expect(alertTexts()).toStrictEqual([
      'יש לבחור קטגוריה',
      'יש להזין שם',
      'יש להזין תאריך תוקף',
      'יש לבחור אם להפעיל תזכורת',
    ]);
    expect(onSubmit).not.toHaveBeenCalled();
  });
});
