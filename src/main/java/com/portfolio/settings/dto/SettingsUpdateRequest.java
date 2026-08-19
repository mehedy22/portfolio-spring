package com.portfolio.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Upserts the keys it carries and leaves every other key alone, so each admin screen can submit
 * only its own section. A key sent with a {@code null} value is reset to its registry default.
 * Unknown keys are rejected rather than silently stored.
 */
@Schema(description = "The settings to change; omitted keys are untouched")
public record SettingsUpdateRequest(@NotNull Map<String, String> settings) {
}
