package com.portfolio.common.exception;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

	/**
	 * The container rejects an oversized body before the Media service ever sees it. Without this
	 * the request would surface as a 500, which is misleading: the client sent something invalid.
	 * docs/07-api/api-conventions.md defines no 413 code, so it maps to the 400 the error table has.
	 */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorResponse> handleUploadTooLarge(
			MaxUploadSizeExceededException ex, WebRequest request) {
		return badRequest("Uploaded file exceeds the maximum allowed size", request);
	}

	/** A missing or malformed multipart part is bad input, not a server fault. */
	@ExceptionHandler({
		MultipartException.class,
		MissingServletRequestPartException.class,
		MissingServletRequestParameterException.class
	})
	public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex, WebRequest request) {
		log.warn("Rejected malformed request: {}", ex.getMessage());
		return badRequest("Malformed or incomplete request", request);
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

	private ResponseEntity<ErrorResponse> badRequest(String message, WebRequest request) {
		ErrorResponse body = ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", message, path(request));
		return ResponseEntity.badRequest().body(body);
	}

	private ErrorResponse.FieldViolation toViolation(FieldError error) {
		return new ErrorResponse.FieldViolation(error.getField(), error.getDefaultMessage());
	}

	private String path(WebRequest request) {
		return request.getDescription(false).replace("uri=", "");
	}
}
