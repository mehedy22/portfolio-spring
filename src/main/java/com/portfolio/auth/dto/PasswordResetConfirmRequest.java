package com.portfolio.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Completes a reset.
 *
 * <p>The minimum length is enforced here because this is the one place a password is chosen
 * without the old one — the login endpoint has no business opinion on length, but this does.
 */
public record PasswordResetConfirmRequest(
		@NotBlank String token,
		@NotBlank @Size(min = 12, max = 200, message = "Password must be at least 12 characters")
				String newPassword) {
}
