package com.portfolio.project;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.ContentModuleTestBase;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sprint 11 acceptance for FR-18 — public search across projects and articles. */
class ProjectSearchIntegrationTest extends ContentModuleTestBase {

	@BeforeEach
	void clear() {
		truncate("project_technology", "project_gallery", "project_challenge", "project", "technology",
				"article_tag", "article", "tag", "category");
	}

	@Test
	@DisplayName("project search matches title, description and technology name")
	void projectSearch() throws Exception {
		publishProject("Realtime Sync", "Keeps clients consistent", List.of("Redis", "Kafka"));
		publishProject("Static Site", "A small marketing page", List.of("Astro"));

		mockMvc.perform(get("/api/v1/projects").param("search", "realtime"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].title").value("Realtime Sync"));

		mockMvc.perform(get("/api/v1/projects").param("search", "marketing"))
				.andExpect(jsonPath("$.data[0].title").value("Static Site"));

		// By technology, which is the search a visitor most often actually wants.
		mockMvc.perform(get("/api/v1/projects").param("search", "kafka"))
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].title").value("Realtime Sync"));

		mockMvc.perform(get("/api/v1/projects").param("search", "nothing-matches-this"))
				.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	@DisplayName("an empty search term behaves like no search at all")
	void blankSearchReturnsEverything() throws Exception {
		publishProject("One", "First", List.of());
		publishProject("Two", "Second", List.of());

		mockMvc.perform(get("/api/v1/projects").param("search", "   "))
				.andExpect(jsonPath("$.data.length()").value(2));
		mockMvc.perform(get("/api/v1/projects")).andExpect(jsonPath("$.data.length()").value(2));
	}

	@Test
	@DisplayName("search never reveals a draft project")
	void searchRespectsPublication() throws Exception {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("title", "Secret Prototype");
		body.put("shortDescription", "Not for public eyes");
		mockMvc.perform(adminPost("/api/v1/admin/projects", body)).andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/projects").param("search", "secret"))
				.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	@DisplayName("article search matches title and excerpt")
	void articleSearch() throws Exception {
		publishArticle("Understanding Backpressure", "A note on flow control");
		publishArticle("A Design Diary", "Sketches and dead ends");

		mockMvc.perform(get("/api/v1/articles").param("search", "backpressure"))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].title").value("Understanding Backpressure"));

		mockMvc.perform(get("/api/v1/articles").param("search", "sketches"))
				.andExpect(jsonPath("$.data.totalElements").value(1));

		mockMvc.perform(get("/api/v1/articles")).andExpect(jsonPath("$.data.totalElements").value(2));
	}

	private void publishProject(String title, String description, List<String> technologies) throws Exception {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("title", title);
		body.put("shortDescription", description);
		body.put("technologies", technologies);
		String created = mockMvc.perform(adminPost("/api/v1/admin/projects", body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Long id = idOf(created);
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.patch("/api/v1/admin/projects/{id}/status", id)
						.header(org.springframework.http.HttpHeaders.AUTHORIZATION, bearer())
						.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
						.content("{\"status\":\"PUBLISHED\"}"))
				.andExpect(status().isOk());
	}

	private void publishArticle(String title, String excerpt) throws Exception {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("title", title);
		body.put("excerpt", excerpt);
		body.put("content", "<p>Body text.</p>");
		body.put("status", "PUBLISHED");
		mockMvc.perform(adminPost("/api/v1/admin/articles", body)).andExpect(status().isCreated());
	}
}
