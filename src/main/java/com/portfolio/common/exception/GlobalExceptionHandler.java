package com.portfolio.common.exception;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Maps every exception to the stable error envelope in docs/07-api/api-conventions.md.
 * Never leaks stack traces or entity internals in the response body (NFR-05).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, WebRequest request) {
		ErrorResponse body = ErrorResponse.of(
				ex.getStatus().value(), ex.getCode(), ex.getMessage(), path(request));
		return ResponseEntity.status(ex.getStatus()).body(body);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
		List<ErrorResponse.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toViolation)
				.toList();
		ErrorResponse body = ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", "Invalid request", path(request), violations);
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, WebRequest request) {
		log.error("Unhandled exception", ex);
		ErrorResponse body = ErrorResponse.of(
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"INTERNAL_ERROR",
				"An unexpected error occurred",
				path(request));
		return ResponseEntity.internalServerError().body(body);
	}

	private ErrorResponse.FieldViolation toViolation(FieldError error) {
		return new ErrorResponse.FieldViolation(error.getField(), error.getDefaultMessage());
	}

	private String path(WebRequest request) {
		return request.getDescription(false).replace("uri=", "");
	}
}
