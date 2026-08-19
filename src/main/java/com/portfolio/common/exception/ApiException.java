package com.portfolio.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for exceptions that map directly to a stable API error code and HTTP status,
 * per docs/07-api/api-conventions.md's error model. Never expose stack traces or entity
 * internals through the message.
 */
public abstract class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	protected ApiException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
