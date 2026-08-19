package com.portfolio.common.exception;

import org.springframework.http.HttpStatus;

/** 404 NOT_FOUND — the resource doesn't exist, or (on public endpoints) isn't published. */
public class ResourceNotFoundException extends ApiException {

	public ResourceNotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
	}
}
