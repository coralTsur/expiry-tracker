package com.coral.expirytracker.item;

public class ItemNotFoundException extends RuntimeException {

	public ItemNotFoundException(Long id) {
		super("פריט " + id + " לא נמצא");
	}

}
