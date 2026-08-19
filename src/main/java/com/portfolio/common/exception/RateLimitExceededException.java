package com.portfolio.common.exception;

import org.springframework.http.HttpStatus;

/** 429 RATE_LIMITED — e.g. Contact form, login, or Analytics endpoint throttling. */
public class RateLimitExceededException extends ApiException {

	public RateLimitExceededException(String message) {
		super(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", message);
	}
}
