package com.portfolio.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * The admin dashboard's figures, all computed on read.
 *
 * <p>There is deliberately no "unique visitors" or "average session": both require identifying a
 * visitor across requests, and this module stores nothing that can (D-026). Everything here is
 * derivable from an anonymous log.
 */
@Schema(description = "Analytics totals and breakdowns")
public record AnalyticsSummaryResponse(
		long totalViews,
		long viewsToday,
		long viewsLast7Days,
		long viewsLast30Days,
		@Schema(description = "One entry per day for the last 30 days, zero-filled")
				List<DailyPoint> dailyViews,
		List<LabelCount> topPages,
		List<LabelCount> topReferrers,
		List<EntityCount> topEntities,
		List<LabelCount> byDevice,
		List<LabelCount> byBrowser) {

	public record DailyPoint(LocalDate date, long views) {
	}

	public record LabelCount(String label, long views) {
	}

	public record EntityCount(String entityType, Long entityId, long views) {
	}
}
