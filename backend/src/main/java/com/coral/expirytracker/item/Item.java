package com.coral.expirytracker.item;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "items")
public class Item {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Category category;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private LocalDate expirationDate;

	private BigDecimal amount;

	private BigDecimal currentAmount;

	private String lastFourDigits;

	private String acceptedStores;

	@Column(nullable = false)
	private boolean reminderEnabled;

	private Integer daysBeforeReminder = 30;

	@Column(nullable = false)
	private boolean redeemed = false;

	protected Item() {
	}

	public Item(Category category, String name, LocalDate expirationDate, boolean reminderEnabled) {
		this.category = category;
		this.name = name;
		this.expirationDate = expirationDate;
		this.reminderEnabled = reminderEnabled;
	}

	public Long getId() {
		return id;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDate getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(LocalDate expirationDate) {
		this.expirationDate = expirationDate;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getCurrentAmount() {
		return currentAmount;
	}

	public void setCurrentAmount(BigDecimal currentAmount) {
		this.currentAmount = currentAmount;
	}

	public String getLastFourDigits() {
		return lastFourDigits;
	}

	public void setLastFourDigits(String lastFourDigits) {
		this.lastFourDigits = lastFourDigits;
	}

	public String getAcceptedStores() {
		return acceptedStores;
	}

	public void setAcceptedStores(String acceptedStores) {
		this.acceptedStores = acceptedStores;
	}

	public boolean isReminderEnabled() {
		return reminderEnabled;
	}

	public void setReminderEnabled(boolean reminderEnabled) {
		this.reminderEnabled = reminderEnabled;
	}

	public Integer getDaysBeforeReminder() {
		return daysBeforeReminder;
	}

	public void setDaysBeforeReminder(Integer daysBeforeReminder) {
		this.daysBeforeReminder = daysBeforeReminder;
	}

	public boolean isRedeemed() {
		return redeemed;
	}

	public void setRedeemed(boolean redeemed) {
		this.redeemed = redeemed;
	}

}
