package com.coral.expirytracker.item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class InvalidItemException extends RuntimeException {

	private final Map<String, String> fieldErrors;

	public InvalidItemException(Map<String, String> fieldErrors) {
		super("Invalid item: " + fieldErrors);
		this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
	}

	public Map<String, String> getFieldErrors() {
		return fieldErrors;
	}

}
