package com.portfolio.analytics.dto;

import com.portfolio.analytics.entity.DeviceType;
import java.time.Instant;

/** One row of the raw log, for the admin's drill-down view. */
public record PageViewResponse(
		Long id,
		String path,
		String entityType,
		Long entityId,
		String referrer,
		DeviceType deviceType,
		String browser,
		Instant viewedAt) {
}
