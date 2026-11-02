package com.coral.expirytracker.item;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ItemResponse create(@RequestBody ItemRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	public ItemResponse update(@PathVariable Long id, @RequestBody ItemRequest request) {
		return service.update(id, request);
	}

}
