package com.portfolio.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Asks for a reset link. The response is identical whether or not the address is known. */
public record PasswordResetRequest(@NotBlank @Email @Size(max = 255) String email) {
}
