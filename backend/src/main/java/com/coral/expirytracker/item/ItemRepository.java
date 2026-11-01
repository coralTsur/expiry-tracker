package com.coral.expirytracker.item;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

	List<Item> findByRedeemedFalseAndExpirationDateBetweenOrderByExpirationDateAsc(LocalDate from, LocalDate to);

}
