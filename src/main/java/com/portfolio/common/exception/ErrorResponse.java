package com.portfolio.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * Standard error envelope, per docs/07-api/api-conventions.md:
 * {success, timestamp, status, code, message, path, errors}.
 */
public record ErrorResponse(
		boolean success,
		Instant timestamp,
		int status,
		String code,
		String message,
		String path,
		List<FieldViolation> errors) {

	public record FieldViolation(String field, String message) {
	}

	public static ErrorResponse of(int status, String code, String message, String path) {
		return new ErrorResponse(false, Instant.now(), status, code, message, path, List.of());
	}

	public static ErrorResponse of(int status, String code, String message, String path, List<FieldViolation> errors) {
		return new ErrorResponse(false, Instant.now(), status, code, message, path, errors);
	}
}
