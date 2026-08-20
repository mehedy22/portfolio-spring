package com.portfolio.achievement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.ContentModuleTestBase;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/** Sprint 11 acceptance for the Achievements module (FR-12). */
class AchievementIntegrationTest extends ContentModuleTestBase {

	@BeforeEach
	void clear() {
		truncate("achievement");
	}

	@Test
	@DisplayName("an achievement defaults to PUBLISHED and is immediately public")
	void createDefaultsToPublished() throws Exception {
		Long imageId = seedMedia("award.png");
		Map<String, Object> body = achievement("Best Engineer 2026");
		body.put("imageMediaId", imageId);
		body.put("achievedOn", "2026-05-01");

		mockMvc.perform(adminPost("/api/v1/admin/achievements", body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("PUBLISHED"))
				.andExpect(jsonPath("$.data.image.id").value(imageId));

		mockMvc.perform(get("/api/v1/achievements"))
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].title").value("Best Engineer 2026"));
	}

	@Test
	@DisplayName("a draft is hidden publicly and ordering follows display order")
	void visibilityAndOrdering() throws Exception {
		Map<String, Object> draft = achievement("Hidden");
		draft.put("status", "DRAFT");
		mockMvc.perform(adminPost("/api/v1/admin/achievements", draft)).andExpect(status().isCreated());

		Map<String, Object> second = achievement("Second");
		second.put("displayOrder", 20);
		Map<String, Object> first = achievement("First");
		first.put("displayOrder", 10);
		mockMvc.perform(adminPost("/api/v1/admin/achievements", second));
		mockMvc.perform(adminPost("/api/v1/admin/achievements", first));

		mockMvc.perform(get("/api/v1/achievements"))
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].title").value("First"));
	}

	@Test
	@DisplayName("update, delete, not-found and auth behave as documented")
	void lifecycleAndGuards() throws Exception {
		Long id = idOf(mockMvc.perform(adminPost("/api/v1/admin/achievements", achievement("Award")))
				.andReturn().getResponse().getContentAsString());

		mockMvc.perform(adminPut("/api/v1/admin/achievements/" + id, achievement("Renamed award")))
				.andExpect(jsonPath("$.data.title").value("Renamed award"));

		mockMvc.perform(delete("/api/v1/admin/achievements/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/achievements")).andExpect(jsonPath("$.data.length()").value(0));
		mockMvc.perform(get("/api/v1/admin/achievements/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/v1/admin/achievements")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/achievements")).andExpect(status().isOk());
	}

	@Test
	@DisplayName("a title is required")
	void validation() throws Exception {
		mockMvc.perform(adminPost("/api/v1/admin/achievements", new LinkedHashMap<String, Object>()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("title"));
	}

	private Map<String, Object> achievement(String title) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("title", title);
		return body;
	}
}
