package com.portfolio.common.exception;

import org.springframework.http.HttpStatus;

/** 409 CONFLICT — e.g. a unique constraint violation such as a duplicate slug. */
public class ConflictException extends ApiException {

	public ConflictException(String message) {
		super(HttpStatus.CONFLICT, "CONFLICT", message);
	}
}
