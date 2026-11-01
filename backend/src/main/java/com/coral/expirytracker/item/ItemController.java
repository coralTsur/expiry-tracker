package com.coral.expirytracker.item;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
public class ItemController {

	private final ItemService service;

	public ItemController(ItemService service) {
		this.service = service;
	}

	@GetMapping
	public List<ItemResponse> findAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	public ItemResponse findById(@PathVariable Long id) {
		return service.findById(id);
	}

	@GetMapping("/expiring-soon")
	public List<ItemResponse> findExpiringSoon() {
		return service.findExpiringSoon();
	}

}
