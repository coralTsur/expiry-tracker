INSERT INTO items (category, name, expiration_date, amount, current_amount, last_four_digits, accepted_stores, reminder_enabled, days_before_reminder, redeemed)
VALUES ('CAR_TEST', 'Car test', CURRENT_DATE + INTERVAL '12' DAY, NULL, NULL, NULL, NULL, TRUE, 14, FALSE);

INSERT INTO items (category, name, expiration_date, amount, current_amount, last_four_digits, accepted_stores, reminder_enabled, days_before_reminder, redeemed)
VALUES ('GIFT_CARD', 'Supermarket gift card', CURRENT_DATE + INTERVAL '20' DAY, 200.00, 150.00, '1234', 'Shufersal, Rami Levy', TRUE, 30, FALSE);

INSERT INTO items (category, name, expiration_date, amount, current_amount, last_four_digits, accepted_stores, reminder_enabled, days_before_reminder, redeemed)
VALUES ('GIFT_CARD', 'Birthday gift card', CURRENT_DATE + INTERVAL '10' DAY, 150.00, 0.00, '5678', NULL, TRUE, 30, TRUE);

INSERT INTO items (category, name, expiration_date, amount, current_amount, last_four_digits, accepted_stores, reminder_enabled, days_before_reminder, redeemed)
VALUES ('GIFT_CARD', 'Old gift card', CURRENT_DATE - INTERVAL '5' DAY, 100.00, 100.00, '0042', NULL, TRUE, 7, FALSE);

INSERT INTO items (category, name, expiration_date, amount, current_amount, last_four_digits, accepted_stores, reminder_enabled, days_before_reminder, redeemed)
VALUES ('RENT', 'Apartment lease', CURRENT_DATE + INTERVAL '45' DAY, 5000.00, NULL, NULL, NULL, TRUE, 14, FALSE);

INSERT INTO items (category, name, expiration_date, amount, current_amount, last_four_digits, accepted_stores, reminder_enabled, days_before_reminder, redeemed)
VALUES ('INSURANCE', 'Car insurance', CURRENT_DATE + INTERVAL '90' DAY, 3200.00, NULL, NULL, NULL, TRUE, 30, FALSE);

INSERT INTO items (category, name, expiration_date, amount, current_amount, last_four_digits, accepted_stores, reminder_enabled, days_before_reminder, redeemed)
VALUES ('OTHER', 'Passport', CURRENT_DATE + INTERVAL '400' DAY, NULL, NULL, NULL, NULL, FALSE, NULL, FALSE);
