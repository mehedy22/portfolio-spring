package com.portfolio.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.ContentModuleTestBase;
import com.portfolio.analytics.repository.PageViewRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Sprint 7 acceptance. The DoD's two hard requirements get direct tests: counts increment per
 * entity, and nothing identifying is persisted.
 */
class AnalyticsIntegrationTest extends ContentModuleTestBase {

	private static final String CHROME_DESKTOP =
			"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";
	private static final String SAFARI_IPHONE =
			"Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) "
					+ "Version/17.0 Mobile/15E148 Safari/604.1";

	@Autowired
	private PageViewRepository pageViewRepository;

	@Autowired
	private StringRedisTemplate redis;

	@BeforeEach
	void clear() {
		truncate("page_view");
		Objects.requireNonNull(redis.getConnectionFactory()).getConnection().serverCommands().flushAll();
	}

	@Test
	@DisplayName("a page view is recorded anonymously and counted per entity")
	void countsIncrementPerEntity() throws Exception {
		record(view("/projects/alpha", "PROJECT", 1L), CHROME_DESKTOP, "10.0.1.1");
		record(view("/projects/alpha", "PROJECT", 1L), CHROME_DESKTOP, "10.0.1.2");
		record(view("/projects/beta", "PROJECT", 2L), CHROME_DESKTOP, "10.0.1.3");
		record(view("/about", null, null), CHROME_DESKTOP, "10.0.1.4");

		mockMvc.perform(get("/api/v1/admin/analytics/summary").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalViews").value(4))
				.andExpect(jsonPath("$.data.viewsToday").value(4))
				.andExpect(jsonPath("$.data.topPages[0].label").value("/projects/alpha"))
				.andExpect(jsonPath("$.data.topPages[0].views").value(2))
				.andExpect(jsonPath("$.data.topEntities[0].entityId").value(1))
				.andExpect(jsonPath("$.data.topEntities[0].views").value(2));
	}

	@Test
	@DisplayName("nothing identifying is stored — no IP, no session, no raw User-Agent")
	void storesNoPii() throws Exception {
		record(view("/", null, null), SAFARI_IPHONE, "203.0.113.55");

		var stored = pageViewRepository.findAll().get(0);
		assertThat(stored.getDeviceType().name()).isEqualTo("MOBILE");
		assertThat(stored.getBrowser()).isEqualTo("Safari");

		// Whatever the row holds, none of it may contain the address or the agent string.
		String rowContents = String.join(
				"|",
				String.valueOf(stored.getPath()),
				String.valueOf(stored.getReferrer()),
				String.valueOf(stored.getBrowser()),
				String.valueOf(stored.getEntityType()));
		assertThat(rowContents).doesNotContain("203.0.113.55").doesNotContain("AppleWebKit");
	}

	@Test
	@DisplayName("device and browser are derived server-side, not taken from the client")
	void clientCannotForgeTheBreakdown() throws Exception {
		Map<String, Object> forged = view("/", null, null);
		forged.put("deviceType", "TABLET");
		forged.put("browser", "TotallyFakeBrowser");

		record(forged, CHROME_DESKTOP, "10.0.2.1");

		var stored = pageViewRepository.findAll().get(0);
		assertThat(stored.getDeviceType().name()).isEqualTo("DESKTOP");
		assertThat(stored.getBrowser()).isEqualTo("Chrome");
	}

	@Test
	@DisplayName("the summary breaks views down by device and browser")
	void breakdowns() throws Exception {
		record(view("/", null, null), CHROME_DESKTOP, "10.0.3.1");
		record(view("/", null, null), SAFARI_IPHONE, "10.0.3.2");

		mockMvc.perform(get("/api/v1/admin/analytics/summary").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(jsonPath("$.data.byDevice.length()").value(2))
				.andExpect(jsonPath("$.data.byBrowser.length()").value(2))
				.andExpect(jsonPath("$.data.dailyViews.length()").value(31))
				.andExpect(jsonPath("$.data.dailyViews[30].views").value(2));
	}

	@Test
	@DisplayName("a referrer is recorded and ranked when the browser supplies one")
	void referrers() throws Exception {
		Map<String, Object> withReferrer = view("/", null, null);
		withReferrer.put("referrer", "https://news.example.com/story");
		record(withReferrer, CHROME_DESKTOP, "10.0.4.1");

		mockMvc.perform(get("/api/v1/admin/analytics/summary").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(jsonPath("$.data.topReferrers[0].label").value("https://news.example.com/story"));
	}

	@Test
	@DisplayName("the raw log is filterable by entity type and paginated newest first")
	void rawLog() throws Exception {
		record(view("/projects/alpha", "PROJECT", 1L), CHROME_DESKTOP, "10.0.5.1");
		record(view("/about", null, null), CHROME_DESKTOP, "10.0.5.2");

		mockMvc.perform(get("/api/v1/admin/analytics/page-views")
						.param("entityType", "PROJECT")
						.header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].path").value("/projects/alpha"));

		mockMvc.perform(get("/api/v1/admin/analytics/page-views").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.content[0].path").value("/about"));
	}

	@Test
	@DisplayName("the endpoint is throttled per IP, and one visitor cannot throttle another")
	void rateLimited() throws Exception {
		for (int i = 0; i < 60; i++) {
			record(view("/", null, null), CHROME_DESKTOP, "10.0.6.1");
		}
		mockMvc.perform(request(view("/", null, null), CHROME_DESKTOP, "10.0.6.1"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("RATE_LIMITED"));

		record(view("/", null, null), CHROME_DESKTOP, "10.0.6.2");
		assertThat(pageViewRepository.count()).isEqualTo(61);
	}

	@Test
	@DisplayName("recording is public; reading the analytics is not")
	void authBoundary() throws Exception {
		record(view("/", null, null), CHROME_DESKTOP, "10.0.7.1");

		mockMvc.perform(get("/api/v1/admin/analytics/summary")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/admin/analytics/page-views")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("a path is required")
	void validation() throws Exception {
		mockMvc.perform(request(new LinkedHashMap<>(), CHROME_DESKTOP, "10.0.8.1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("path"));
	}

	// ---------------------------------------------------------------- utils

	private Map<String, Object> view(String path, String entityType, Long entityId) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("path", path);
		if (entityType != null) body.put("entityType", entityType);
		if (entityId != null) body.put("entityId", entityId);
		return body;
	}

	private void record(Map<String, Object> body, String userAgent, String ip) throws Exception {
		mockMvc.perform(request(body, userAgent, ip)).andExpect(status().isCreated());
	}

	private MockHttpServletRequestBuilder request(Map<String, Object> body, String userAgent, String ip)
			throws Exception {
		return post("/api/v1/analytics/page-view")
				.header(HttpHeaders.USER_AGENT, userAgent)
				.header("X-Forwarded-For", ip)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body));
	}
}
