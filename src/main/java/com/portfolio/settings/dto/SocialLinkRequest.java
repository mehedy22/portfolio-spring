package com.portfolio.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** One link in the ordered list. Order comes from array position, not from a field. */
public record SocialLinkRequest(
		@NotBlank @Size(max = 50) String platform,
		@NotBlank @Size(max = 500) String url,
		Boolean visible) {
}
