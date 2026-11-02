package com.coral.expirytracker.item;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	// spec section 8, errors: the message of every 400.
	private static final String ITEM_NOT_SAVED = "הפריט לא נשמר";

	@ExceptionHandler(ItemNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleItemNotFound(ItemNotFoundException exception) {
		HttpStatus status = HttpStatus.NOT_FOUND;
		ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), exception.getMessage(), null);
		return ResponseEntity.status(status).body(body);
	}

	@ExceptionHandler(InvalidItemException.class)
	public ResponseEntity<ErrorResponse> handleInvalidItem(InvalidItemException exception) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), ITEM_NOT_SAVED,
				exception.getFieldErrors());
		return ResponseEntity.status(status).body(body);
	}

	// spec section 8, errors: a body that cannot be read (broken JSON, a category not in the list, another date format)
	// is a 400 without fieldErrors. Jackson fails before the controller method runs, so the service is never called.
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), ITEM_NOT_SAVED, null);
		return ResponseEntity.status(status).body(body);
	}

}
