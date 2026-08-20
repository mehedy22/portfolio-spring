package com.portfolio.analytics.controller;

import com.portfolio.analytics.dto.AnalyticsSummaryResponse;
import com.portfolio.analytics.dto.PageViewRequest;
import com.portfolio.analytics.dto.PageViewResponse;
import com.portfolio.analytics.service.AnalyticsService;
import com.portfolio.common.response.ApiResponse;
import com.portfolio.common.response.PageResponse;
import com.portfolio.common.web.ClientIp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Analytics endpoints per docs/07-api/endpoints.md. Recording is public; reading is not. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Analytics", description = "Anonymous page-view tracking (FR-17)")
public class AnalyticsController {

	private static final int DEFAULT_PAGE_SIZE = 50;

	private final AnalyticsService analyticsService;

	public AnalyticsController(AnalyticsService analyticsService) {
		this.analyticsService = analyticsService;
	}

	@PostMapping("/analytics/page-view")
	@Operation(
			summary = "Record a page view",
			description = "Anonymous and heavily rate-limited. Device and browser are derived "
					+ "server-side from the User-Agent; nothing identifying is stored.")
	public ResponseEntity<ApiResponse<Void>> record(
			@Valid @RequestBody PageViewRequest request,
			@RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
			HttpServletRequest httpRequest) {

		analyticsService.record(request, userAgent, ClientIp.of(httpRequest));
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(null, "Recorded"));
	}

	@GetMapping("/admin/analytics/summary")
	@Operation(summary = "Dashboard figures", description = "Totals, trend and breakdowns, computed on read.")
	public ApiResponse<AnalyticsSummaryResponse> summary() {
		return ApiResponse.of(analyticsService.summary());
	}

	@GetMapping("/admin/analytics/page-views")
	@Operation(summary = "Raw log", description = "Newest first, filterable by entity type and date range.")
	public ApiResponse<PageResponse<PageViewResponse>> pageViews(
			@RequestParam(required = false) String entityType,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {

		return ApiResponse.of(analyticsService.pageViews(entityType, from, to, page, size));
	}
}
