package com.portfolio.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.ContentModuleTestBase;
import com.portfolio.blog.repository.ArticleRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

/**
 * Sprint 9 acceptance. The two things unique to Blog get direct tests: the editorial workflow
 * (including a scheduled article becoming public on its own) and sanitization of the one rich-text
 * field in the system.
 */
class BlogIntegrationTest extends ContentModuleTestBase {

	@Autowired
	private ArticleRepository articleRepository;

	@BeforeEach
	void clear() {
		truncate("article_tag", "article", "tag", "category");
	}

	// ------------------------------------------------------------ workflow

	@Test
	@DisplayName("an article is created as a draft and is invisible publicly")
	void draftsAreInvisible() throws Exception {
		mockMvc.perform(adminPost("/api/v1/admin/articles", article("First Post")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andExpect(jsonPath("$.data.slug").value("first-post"))
				.andExpect(jsonPath("$.data.readingTimeMinutes").value(1));

		mockMvc.perform(get("/api/v1/articles")).andExpect(jsonPath("$.data.totalElements").value(0));
		mockMvc.perform(get("/api/v1/articles/{slug}", "first-post")).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("publishing makes it public, with its category and tags")
	void publishing() throws Exception {
		Map<String, Object> body = article("Published Post");
		body.put("status", "PUBLISHED");
		body.put("category", "Engineering");
		body.put("tags", List.of("Java", "spring"));

		mockMvc.perform(adminPost("/api/v1/admin/articles", body)).andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/articles"))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].category").value("Engineering"))
				.andExpect(jsonPath("$.data.content[0].tags", org.hamcrest.Matchers.contains("Java", "spring")));

		mockMvc.perform(get("/api/v1/articles/{slug}", "published-post"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.title").value("Published Post"));
	}

	@Test
	@DisplayName("a scheduled article stays hidden until its time, then appears on its own")
	void scheduling() throws Exception {
		Map<String, Object> future = article("Tomorrow");
		future.put("status", "SCHEDULED");
		future.put("publishedAt", Instant.now().plus(1, ChronoUnit.DAYS).toString());
		mockMvc.perform(adminPost("/api/v1/admin/articles", future)).andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/articles")).andExpect(jsonPath("$.data.totalElements").value(0));

		// A PUBLISHED article whose time has passed is what "due" means; no job flips anything.
		Map<String, Object> past = article("Yesterday");
		past.put("status", "PUBLISHED");
		past.put("publishedAt", Instant.now().minus(1, ChronoUnit.DAYS).toString());
		mockMvc.perform(adminPost("/api/v1/admin/articles", past)).andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/articles"))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].title").value("Yesterday"));
	}

	@Test
	@DisplayName("scheduling into the past is rejected")
	void scheduleMustBeInTheFuture() throws Exception {
		Map<String, Object> body = article("Backdated");
		body.put("status", "SCHEDULED");
		body.put("publishedAt", Instant.now().minus(1, ChronoUnit.HOURS).toString());

		mockMvc.perform(adminPost("/api/v1/admin/articles", body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("future")));
	}

	// ------------------------------------------------------- sanitization

	@Test
	@DisplayName("dangerous markup is stripped on write, and safe formatting survives")
	void contentIsSanitized() throws Exception {
		Map<String, Object> body = article("Hostile Post");
		body.put(
				"content",
				"<p>Safe <strong>bold</strong> and <em>italic</em>.</p>"
						+ "<script>alert('xss')</script>"
						+ "<img src=x onerror=alert(1)>"
						+ "<a href=\"javascript:alert(1)\">bad link</a>"
						+ "<a href=\"https://example.com\">good link</a>"
						+ "<iframe src=\"https://evil.example\"></iframe>");
		body.put("status", "PUBLISHED");

		mockMvc.perform(adminPost("/api/v1/admin/articles", body)).andExpect(status().isCreated());

		String stored = articleRepository.findAll().get(0).getContent();
		assertThat(stored)
				.as("the stored value is already safe, not merely escaped at render time")
				.doesNotContain("<script")
				.doesNotContain("onerror")
				.doesNotContain("javascript:")
				.doesNotContain("<iframe");
		assertThat(stored).contains("<strong>bold</strong>").contains("https://example.com");
	}

	@Test
	@DisplayName("content consisting only of unsafe markup is rejected rather than stored empty")
	void contentThatSanitizesToNothingIsRejected() throws Exception {
		Map<String, Object> body = article("Empty After Cleaning");
		body.put("content", "<script>alert(1)</script>");

		mockMvc.perform(adminPost("/api/v1/admin/articles", body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("unsafe markup")));
	}

	// ------------------------------------------------------ filters / CRUD

	@Test
	@DisplayName("the public list filters by category and by tag")
	void filtering() throws Exception {
		Map<String, Object> a = article("Java Post");
		a.put("status", "PUBLISHED");
		a.put("category", "Engineering");
		a.put("tags", List.of("Java"));
		Map<String, Object> b = article("Design Post");
		b.put("status", "PUBLISHED");
		b.put("category", "Design");
		b.put("tags", List.of("UX"));
		mockMvc.perform(adminPost("/api/v1/admin/articles", a)).andExpect(status().isCreated());
		mockMvc.perform(adminPost("/api/v1/admin/articles", b)).andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/articles").param("category", "engineering"))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].title").value("Java Post"));
		mockMvc.perform(get("/api/v1/articles").param("tag", "ux"))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].title").value("Design Post"));

		mockMvc.perform(get("/api/v1/articles/categories"))
				.andExpect(jsonPath("$.data.length()").value(2));
	}

	@Test
	@DisplayName("a duplicate explicit slug is a 409; a derived one is suffixed")
	void slugs() throws Exception {
		mockMvc.perform(adminPost("/api/v1/admin/articles", article("Same Title")))
				.andExpect(jsonPath("$.data.slug").value("same-title"));
		mockMvc.perform(adminPost("/api/v1/admin/articles", article("Same Title")))
				.andExpect(jsonPath("$.data.slug").value("same-title-2"));

		Map<String, Object> explicit = article("Other");
		explicit.put("slug", "same-title");
		mockMvc.perform(adminPost("/api/v1/admin/articles", explicit)).andExpect(status().isConflict());
	}

	@Test
	@DisplayName("update replaces tags, and delete hides the article from both surfaces")
	void updateAndDelete() throws Exception {
		Map<String, Object> body = article("Lifecycle");
		body.put("status", "PUBLISHED");
		body.put("tags", List.of("One", "Two"));
		Long id = idOf(mockMvc.perform(adminPost("/api/v1/admin/articles", body))
				.andReturn().getResponse().getContentAsString());

		Map<String, Object> update = article("Lifecycle");
		update.put("status", "PUBLISHED");
		update.put("tags", List.of("One"));
		mockMvc.perform(adminPut("/api/v1/admin/articles/" + id, update))
				.andExpect(jsonPath("$.data.tags.length()").value(1));

		mockMvc.perform(delete("/api/v1/admin/articles/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/articles")).andExpect(jsonPath("$.data.totalElements").value(0));
		mockMvc.perform(get("/api/v1/admin/articles/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("admin routes need a token; the public ones do not")
	void authBoundary() throws Exception {
		mockMvc.perform(get("/api/v1/admin/articles")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/admin/tags")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/articles")).andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/articles/tags")).andExpect(status().isOk());
	}

	// ---------------------------------------------------------------- utils

	private Map<String, Object> article(String title) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("title", title);
		body.put("content", "<p>A perfectly ordinary paragraph of article content.</p>");
		return body;
	}
}
