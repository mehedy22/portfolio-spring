package com.portfolio.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * References media uploaded earlier through the Media module. Sending null for either clears it,
 * which is how the admin removes a photo or resume without deleting the underlying file.
 */
public record SiteProfileUpdateRequest(
		@Schema(description = "Media id, or null to clear") Long profileImageMediaId,
		@Schema(description = "Media id, or null to clear") Long resumeMediaId) {
}
