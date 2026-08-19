package com.portfolio.contact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A visitor's submission. Anonymous — no account exists for a visitor to have (D-005).
 *
 * @param website the honeypot. It is hidden from humans by the form's CSS, so anything in it came
 *     from a bot filling every input it found. Named plausibly on purpose: a field called
 *     "honeypot" tells a bot exactly what to skip.
 */
public record ContactMessageRequest(
		@NotBlank @Size(max = 200) String name,
		@NotBlank @Email @Size(max = 255) String email,
		@Size(max = 300) String subject,
		@NotBlank @Size(max = 5000) String message,
		@Schema(description = "Leave empty. Hidden anti-spam field.", accessMode = Schema.AccessMode.WRITE_ONLY)
				String website) {
}
