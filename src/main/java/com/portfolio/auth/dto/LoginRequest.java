package com.portfolio.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Login credentials. Violations surface as 400 VALIDATION_ERROR with a per-field errors[] array. */
public record LoginRequest(

		@NotBlank(message = "must not be blank")
		@Email(message = "must be a well-formed email address")
		String email,

		@NotBlank(message = "must not be blank")
		String password) {
}
