package com.coral.expirytracker.item;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

// spec section 8, errors: a 404 has no fieldErrors, so a null value drops the key from the JSON.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, String error, String message, Map<String, String> fieldErrors) {
}
