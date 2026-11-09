import { useState } from 'react';
import { CATEGORY_LABELS } from '../utils/categoryLabels';
import { DAYS_PRESETS, toRequestBody, validateItem } from '../utils/itemForm';
import './ItemForm.css';

// Turns the server's { field: message } into the form's error entries.
function toServerErrors(serverFieldErrors) {
  const errors = {};
  for (const [field, message] of Object.entries(serverFieldErrors ?? {})) {
    errors[field] = { message, fromServer: true };
  }
  return errors;
}

// After a change, an error is only updated if it is already shown. A new error appears on blur (spec section 5).
// changedField: its server error no longer applies, so it is removed; its client error is checked again.
// Other fields: a client error is checked again (amount can affect currentAmount), a server error is kept.
function errorsAfterChange(prevErrors, changedField, newValues, originalValues) {
  const clientErrors = validateItem(newValues, originalValues);
  const nextErrors = {};
  for (const [field, error] of Object.entries(prevErrors)) {
    if (error.fromServer && field !== changedField) {
      nextErrors[field] = error;
    } else if (!error.fromServer && clientErrors[field]) {
      nextErrors[field] = { message: clientErrors[field], fromServer: false };
    }
  }
  return nextErrors;
}

// initialValues: the values the form starts with (EMPTY_ITEM_VALUES when adding, the saved item when editing).
// They are also the "original" values for the update rules of spec section 5.
// onSubmit(item): async, receives the request body. If it throws, the form shows the error.
// showRedeemed: true on the edit screen only, where an item can be marked or unmarked as redeemed.
// The page decides what the submit does, so the add and edit screens share this form.
function ItemForm({ initialValues, submitLabel, onSubmit, showRedeemed = false }) {
  const [values, setValues] = useState(initialValues);
  // { field: { message, fromServer } }. fromServer: a client re-check must not remove a server error.
  const [fieldErrors, setFieldErrors] = useState({});
  const [generalError, setGeneralError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  // Remembers that "אחר" was chosen, so typing 1 -> 14 in the number input does not switch the dropdown to 14.
  const [otherDaysChosen, setOtherDaysChosen] = useState(
    !DAYS_PRESETS.includes(initialValues.daysBeforeReminder),
  );

  function updateField(field, value) {
    // The new values are built here, so validation does not see the values from before this change.
    const newValues = { ...values, [field]: value };
    setValues(newValues);
    setFieldErrors((prevErrors) => errorsAfterChange(prevErrors, field, newValues, initialValues));
  }

  function handleChange(event) {
    updateField(event.target.name, event.target.value);
  }

  // Radio values are the strings "true" and "false". The state holds a real boolean.
  function handleReminderChange(event) {
    updateField('reminderEnabled', event.target.value === 'true');
  }

  // A checkbox's value is always "on". Whether it is ticked is in checked.
  function handleRedeemedChange(event) {
    updateField('redeemed', event.target.checked);
  }

  function handleDaysOptionChange(event) {
    const choice = event.target.value;
    if (choice === 'other') {
      // The current number stays, and the user edits it in the number input.
      setOtherDaysChosen(true);
    } else {
      setOtherDaysChosen(false);
      updateField('daysBeforeReminder', choice);
    }
  }

  // spec section 5: a field's error appears when the user leaves the field.
  function handleBlur(event) {
    const field = event.target.name;
    const message = validateItem(values, initialValues)[field];
    setFieldErrors((prevErrors) => {
      const nextErrors = { ...prevErrors };
      if (message) {
        nextErrors[field] = { message, fromServer: false };
      } else if (prevErrors[field] && !prevErrors[field].fromServer) {
        delete nextErrors[field];
      }
      return nextErrors;
    });
  }

  async function handleSubmit(event) {
    // Without this the browser reloads the page.
    event.preventDefault();
    setGeneralError(null);

    const clientErrors = validateItem(values, initialValues);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors((prevErrors) => {
        // Server errors on fields the client finds valid are kept. The rest are replaced by client errors.
        const nextErrors = {};
        for (const [field, error] of Object.entries(prevErrors)) {
          if (error.fromServer) {
            nextErrors[field] = error;
          }
        }
        for (const [field, message] of Object.entries(clientErrors)) {
          nextErrors[field] = { message, fromServer: false };
        }
        return nextErrors;
      });
      return;
    }

    setSubmitting(true);
    try {
      await onSubmit(toRequestBody(values, initialValues));
      // On success the page navigates away, so submitting stays true and the button cannot be clicked twice.
    } catch (err) {
      setFieldErrors(toServerErrors(err.fieldErrors));
      setGeneralError(err.message);
      setSubmitting(false);
    }
  }

  // Links an input to its error text, so a screen reader reads the error with the field.
  function describedBy(field) {
    return fieldErrors[field] ? `${field}-error` : undefined;
  }

  function renderError(field) {
    const error = fieldErrors[field];
    if (!error) {
      return null;
    }
    return (
      <p id={`${field}-error`} className="field-error" role="alert">
        {error.message}
      </p>
    );
  }

  return (
    <>
      {generalError && (
        <p className="form-error" role="alert">
          {generalError}
        </p>
      )}

      {/* noValidate: the browser's own error bubbles would block onSubmit and are not in Hebrew. */}
      <form className="item-form" onSubmit={handleSubmit} noValidate>
        <div className="form-field">
          <label htmlFor="category">קטגוריה</label>
          <select
            id="category"
            name="category"
            value={values.category}
            onChange={handleChange}
            onBlur={handleBlur}
            required
            aria-invalid={fieldErrors.category ? true : undefined}
            aria-describedby={describedBy('category')}
          >
            <option value="">בחרי קטגוריה</option>
            {Object.entries(CATEGORY_LABELS).map(([code, label]) => (
              <option key={code} value={code}>
                {label}
              </option>
            ))}
          </select>
          {renderError('category')}
        </div>

        <div className="form-field">
          <label htmlFor="name">שם</label>
          <input
            id="name"
            name="name"
            type="text"
            value={values.name}
            onChange={handleChange}
            onBlur={handleBlur}
            required
            aria-invalid={fieldErrors.name ? true : undefined}
            aria-describedby={describedBy('name')}
          />
          {renderError('name')}
        </div>

        <div className="form-field">
          <label htmlFor="expirationDate">תאריך תוקף</label>
          {/* The value is already yyyy-MM-dd, the format the API expects. */}
          <input
            id="expirationDate"
            name="expirationDate"
            type="date"
            className={values.expirationDate ? '' : 'date-empty'}
            value={values.expirationDate}
            onChange={handleChange}
            onBlur={handleBlur}
            required
            aria-invalid={fieldErrors.expirationDate ? true : undefined}
            aria-describedby={describedBy('expirationDate')}
          />
          {renderError('expirationDate')}
        </div>

        <div className="form-field">
          <label htmlFor="amount">סכום</label>
          <input
            id="amount"
            name="amount"
            type="number"
            min="0"
            step="0.01"
            inputMode="decimal"
            value={values.amount}
            onChange={handleChange}
            onBlur={handleBlur}
            aria-invalid={fieldErrors.amount ? true : undefined}
            aria-describedby={describedBy('amount')}
          />
          {renderError('amount')}
        </div>

        <div className="form-field">
          <label htmlFor="currentAmount">סכום עדכני</label>
          <input
            id="currentAmount"
            name="currentAmount"
            type="number"
            min="0"
            step="0.01"
            inputMode="decimal"
            value={values.currentAmount}
            onChange={handleChange}
            onBlur={handleBlur}
            aria-invalid={fieldErrors.currentAmount ? true : undefined}
            aria-describedby={describedBy('currentAmount')}
          />
          {renderError('currentAmount')}
        </div>

        <div className="form-field">
          <label htmlFor="lastFourDigits">4 ספרות אחרונות</label>
          {/* Text, not type="number", so leading zeros such as 0123 are kept. */}
          <input
            id="lastFourDigits"
            name="lastFourDigits"
            type="text"
            inputMode="numeric"
            maxLength={4}
            autoComplete="off"
            value={values.lastFourDigits}
            onChange={handleChange}
            onBlur={handleBlur}
            aria-invalid={fieldErrors.lastFourDigits ? true : undefined}
            aria-describedby={describedBy('lastFourDigits')}
          />
          {renderError('lastFourDigits')}
        </div>

        <div className="form-field">
          <label htmlFor="acceptedStores">רשתות מכבדות</label>
          <input
            id="acceptedStores"
            name="acceptedStores"
            type="text"
            value={values.acceptedStores}
            onChange={handleChange}
            onBlur={handleBlur}
          />
        </div>

        <fieldset className="form-field" aria-describedby={describedBy('reminderEnabled')}>
          <legend>תזכורת</legend>
          <div className="radio-group">
            <span>
              <input
                id="reminderEnabled-yes"
                name="reminderEnabled"
                type="radio"
                value="true"
                checked={values.reminderEnabled === true}
                onChange={handleReminderChange}
                onBlur={handleBlur}
              />
              <label htmlFor="reminderEnabled-yes">כן</label>
            </span>
            <span>
              <input
                id="reminderEnabled-no"
                name="reminderEnabled"
                type="radio"
                value="false"
                checked={values.reminderEnabled === false}
                onChange={handleReminderChange}
                onBlur={handleBlur}
              />
              <label htmlFor="reminderEnabled-no">לא</label>
            </span>
          </div>
          {renderError('reminderEnabled')}
        </fieldset>

        {/* Hidden unless the reminder is "כן". Its value stays in state, so it comes back if the reminder is turned on again. */}
        {values.reminderEnabled === true && (
          <div className="form-field">
            <label htmlFor="daysBeforeReminder">ימים לפני התזכורת</label>
            <select
              id="daysBeforeReminder"
              name="daysBeforeReminder"
              value={otherDaysChosen ? 'other' : values.daysBeforeReminder}
              onChange={handleDaysOptionChange}
              onBlur={handleBlur}
              aria-invalid={fieldErrors.daysBeforeReminder ? true : undefined}
              aria-describedby={describedBy('daysBeforeReminder')}
            >
              {DAYS_PRESETS.map((days) => (
                <option key={days} value={days}>
                  {days}
                </option>
              ))}
              <option value="other">אחר</option>
            </select>

            {otherDaysChosen && (
              <>
                <label htmlFor="daysBeforeReminderOther">מספר ימים</label>
                <input
                  id="daysBeforeReminderOther"
                  name="daysBeforeReminder"
                  type="number"
                  min="0"
                  step="1"
                  inputMode="numeric"
                  value={values.daysBeforeReminder}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  aria-invalid={fieldErrors.daysBeforeReminder ? true : undefined}
                  aria-describedby={describedBy('daysBeforeReminder')}
                />
              </>
            )}
            {renderError('daysBeforeReminder')}
          </div>
        )}

        {showRedeemed && (
          <div className="form-field checkbox-field">
            <input
              id="redeemed"
              name="redeemed"
              type="checkbox"
              checked={values.redeemed}
              onChange={handleRedeemedChange}
            />
            <label htmlFor="redeemed">מומש</label>
          </div>
        )}

        <button type="submit" disabled={submitting}>
          {submitLabel}
        </button>
      </form>
    </>
  );
}

export default ItemForm;
