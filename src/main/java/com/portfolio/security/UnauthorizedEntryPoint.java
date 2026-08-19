package com.portfolio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Renders Spring Security's "not authenticated" outcome as the project's standard error envelope
 * (docs/07-api/api-conventions.md) instead of the framework default. The envelope contract applies
 * to auth failures exactly as it does to everything else (NFR-05).
 */
@Component
public class UnauthorizedEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public UnauthorizedEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(
			HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException {

		ErrorResponse body = ErrorResponse.of(
				HttpStatus.UNAUTHORIZED.value(),
				"UNAUTHORIZED",
				"Authentication required",
				request.getRequestURI());

		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
