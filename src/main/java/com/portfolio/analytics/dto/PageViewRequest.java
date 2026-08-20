package com.portfolio.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What the browser reports. Deliberately small: device and browser are derived server-side from
 * the User-Agent and are never accepted from the client, so a caller cannot forge the breakdown.
 */
public record PageViewRequest(
		@NotBlank @Size(max = 500) String path,
		@Schema(description = "e.g. PROJECT", example = "PROJECT") @Size(max = 30) String entityType,
		Long entityId,
		@Size(max = 500) String referrer) {
}
