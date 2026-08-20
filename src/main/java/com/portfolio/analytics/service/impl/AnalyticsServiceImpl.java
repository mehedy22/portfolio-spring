package com.portfolio.analytics.service.impl;

import com.portfolio.analytics.AnalyticsProperties;
import com.portfolio.analytics.dto.AnalyticsSummaryResponse;
import com.portfolio.analytics.dto.AnalyticsSummaryResponse.DailyPoint;
import com.portfolio.analytics.dto.AnalyticsSummaryResponse.EntityCount;
import com.portfolio.analytics.dto.AnalyticsSummaryResponse.LabelCount;
import com.portfolio.analytics.dto.PageViewRequest;
import com.portfolio.analytics.dto.PageViewResponse;
import com.portfolio.analytics.entity.PageView;
import com.portfolio.analytics.repository.PageViewRepository;
import com.portfolio.analytics.service.AnalyticsService;
import com.portfolio.analytics.service.UserAgentParser;
import com.portfolio.common.exception.RateLimitExceededException;
import com.portfolio.common.response.PageResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.portfolio.common.ratelimit.RateLimiter;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

	private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

	private static final String RATE_LIMIT_KEY_PREFIX = "analytics:view:";
	private static final int TOP_N = 10;
	private static final int TREND_DAYS = 30;
	private static final int MAX_PAGE_SIZE = 200;

	private final PageViewRepository pageViewRepository;
	private final UserAgentParser userAgentParser;
	private final RateLimiter rateLimiter;
	private final AnalyticsProperties properties;

	public AnalyticsServiceImpl(
			PageViewRepository pageViewRepository,
			UserAgentParser userAgentParser,
			RateLimiter rateLimiter,
			AnalyticsProperties properties) {
		this.pageViewRepository = pageViewRepository;
		this.userAgentParser = userAgentParser;
		this.rateLimiter = rateLimiter;
		this.properties = properties;
	}

	@Override
	public void record(PageViewRequest request, String userAgent, String clientIp) {
		String key = RATE_LIMIT_KEY_PREFIX + clientIp;
		if (!rateLimiter.isWithinLimit(key, properties.rateLimit().maxAttempts())) {
			// Silently dropping would corrupt the counts invisibly; a 429 is honest and the
			// browser tracker treats it as a no-op.
			throw new RateLimitExceededException("Too many page views recorded. Please slow down.");
		}
		rateLimiter.recordAttempt(key, properties.rateLimit().window());

		pageViewRepository.save(new PageView(
				request.path(),
				blankToNull(request.entityType()),
				request.entityId(),
				blankToNull(request.referrer()),
				userAgentParser.deviceType(userAgent),
				userAgentParser.browser(userAgent)));
	}

	@Override
	@Transactional(readOnly = true)
	public AnalyticsSummaryResponse summary() {
		Instant now = Instant.now();
		Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
		Instant thirtyDaysAgo = now.minus(TREND_DAYS, ChronoUnit.DAYS);
		Pageable top = PageRequest.of(0, TOP_N);

		return new AnalyticsSummaryResponse(
				pageViewRepository.count(),
				pageViewRepository.countByViewedAtAfter(startOfToday),
				pageViewRepository.countByViewedAtAfter(sevenDaysAgo),
				pageViewRepository.countByViewedAtAfter(thirtyDaysAgo),
				dailySeries(thirtyDaysAgo),
				labels(pageViewRepository.topPaths(thirtyDaysAgo, top)),
				labels(pageViewRepository.topReferrers(thirtyDaysAgo, top)),
				pageViewRepository.topEntities(thirtyDaysAgo, top).stream()
						.map(row -> new EntityCount(row.getEntityType(), row.getEntityId(), row.getTotal()))
						.toList(),
				labels(pageViewRepository.byDevice(thirtyDaysAgo)),
				labels(pageViewRepository.byBrowser(thirtyDaysAgo)));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<PageViewResponse> pageViews(
			String entityType, LocalDate from, LocalDate to, int page, int size) {

		Pageable pageable = PageRequest.of(
				Math.max(page, 0),
				Math.clamp(size, 1, MAX_PAGE_SIZE),
				Sort.by(Sort.Order.desc("viewedAt"), Sort.Order.desc("id")));

		boolean hasRange = from != null || to != null;
		// An open-ended range still needs concrete bounds for the query; "to" is inclusive of the
		// whole day the admin picked, which is what a date filter is expected to mean.
		Instant start = from != null
				? from.atStartOfDay(ZoneOffset.UTC).toInstant()
				: Instant.EPOCH;
		Instant end = to != null
				? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
				: Instant.now().plus(1, ChronoUnit.DAYS);

		Page<PageView> found;
		if (entityType != null && !entityType.isBlank() && hasRange) {
			found = pageViewRepository.findByEntityTypeAndViewedAtBetween(entityType, start, end, pageable);
		}
		else if (entityType != null && !entityType.isBlank()) {
			found = pageViewRepository.findByEntityType(entityType, pageable);
		}
		else if (hasRange) {
			found = pageViewRepository.findByViewedAtBetween(start, end, pageable);
		}
		else {
			found = pageViewRepository.findAll(pageable);
		}

		return PageResponse.from(found.map(this::toResponse));
	}

	/** Zero-fills days with no traffic, so the chart shows a gap rather than skipping a date. */
	private List<DailyPoint> dailySeries(Instant since) {
		Map<LocalDate, Long> counts = new HashMap<>();
		pageViewRepository.dailyCounts(since).forEach(row ->
				counts.put(LocalDate.ofInstant(row.getDay(), ZoneOffset.UTC), row.getTotal()));

		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		List<DailyPoint> series = new ArrayList<>(TREND_DAYS + 1);
		for (int offset = TREND_DAYS; offset >= 0; offset--) {
			LocalDate day = today.minusDays(offset);
			series.add(new DailyPoint(day, counts.getOrDefault(day, 0L)));
		}
		return series;
	}

	private List<LabelCount> labels(List<PageViewRepository.CountByLabel> rows) {
		return rows.stream().map(row -> new LabelCount(row.getLabel(), row.getTotal())).toList();
	}

	private PageViewResponse toResponse(PageView view) {
		return new PageViewResponse(
				view.getId(),
				view.getPath(),
				view.getEntityType(),
				view.getEntityId(),
				view.getReferrer(),
				view.getDeviceType(),
				view.getBrowser(),
				view.getViewedAt());
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
