package com.portfolio.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 400 VALIDATION_ERROR for rejections Bean Validation cannot express — an upload whose
 * <em>content</em> is not on the type allow-list, or whose size exceeds its per-type limit
 * (docs/07-api/api-conventions.md's error table has no 413/415 code; malformed input is a 400).
 */
public class ValidationException extends ApiException {

	public ValidationException(String message) {
		super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
	}
}
