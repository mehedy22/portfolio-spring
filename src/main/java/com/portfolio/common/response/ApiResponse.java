package com.portfolio.common.response;

import java.time.Instant;

/**
 * Standard success envelope for every API response, per
 * docs/07-api/api-conventions.md: {success, data, message, timestamp}.
 */
public record ApiResponse<T>(boolean success, T data, String message, Instant timestamp) {

	public static <T> ApiResponse<T> of(T data) {
		return new ApiResponse<>(true, data, null, Instant.now());
	}

	public static <T> ApiResponse<T> of(T data, String message) {
		return new ApiResponse<>(true, data, message, Instant.now());
	}
}
