package com.portfolio.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * Settings as a flat key → value map. Every known key in the group is always present, filled from
 * its registry default when nothing has been stored — so a client never has to guess whether a
 * missing key means "unset" or "empty".
 */
@Schema(description = "Settings keyed by name")
public record SettingsResponse(Map<String, String> settings) {
}
