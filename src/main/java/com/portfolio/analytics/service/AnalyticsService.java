package com.portfolio.analytics.service;

import com.portfolio.analytics.dto.AnalyticsSummaryResponse;
import com.portfolio.analytics.dto.PageViewRequest;
import com.portfolio.analytics.dto.PageViewResponse;
import com.portfolio.common.response.PageResponse;
import java.time.LocalDate;

public interface AnalyticsService {

	/**
	 * Records one view.
	 *
	 * @param userAgent used only to derive coarse device/browser buckets; never stored
	 * @param clientIp used only as an in-memory rate-limit key; never stored
	 */
	void record(PageViewRequest request, String userAgent, String clientIp);

	AnalyticsSummaryResponse summary();

	PageResponse<PageViewResponse> pageViews(
			String entityType, LocalDate from, LocalDate to, int page, int size);
}
