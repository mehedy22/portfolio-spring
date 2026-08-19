package com.portfolio.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.IntegrationTestBase;
import com.portfolio.auth.AdminBootstrap;
import com.portfolio.auth.entity.Admin;
import com.portfolio.auth.repository.AdminRepository;
import com.portfolio.media.entity.Media;
import com.portfolio.media.entity.StorageBackend;
import com.portfolio.media.repository.MediaRepository;
import com.portfolio.project.repository.ProjectRepository;
import com.portfolio.security.JwtTokenProvider;
import com.portfolio.technology.repository.TechnologyRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Sprint 3 acceptance. The three DoD properties get direct tests — drafts are invisible to the
 * public API (404, not 403), challenge/solution blocks and technology tags round-trip, and the
 * detail payload carries everything the mockup's Project Detail page renders — alongside the
 * usual happy path / validation / conflict / not-found / auth cases.
 */
@AutoConfigureMockMvc
class ProjectIntegrationTest extends IntegrationTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private TechnologyRepository technologyRepository;

	@Autowired
	private MediaRepository mediaRepository;

	@Autowired
	private AdminRepository adminRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtTokenProvider tokenProvider;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AdminBootstrap adminBootstrap;

	private String accessToken;

	@BeforeEach
	void seed() {
		projectRepository.deleteAll();
		technologyRepository.deleteAll();
		mediaRepository.deleteAll();
		adminRepository.deleteAll();
		Admin admin = adminRepository.save(
				new Admin("project-admin@yourname.dev", passwordEncoder.encode("irrelevant")));
		accessToken = tokenProvider.createAccessToken(admin.getId());
	}

	// --------------------------------------------------------------- create

	@Test
	@DisplayName("creating a project stores its challenges, technologies and gallery in order")
	void createRoundTripsTheWholeAggregate() throws Exception {
		Long thumbnailId = seedMedia("thumb.png");
		Long shot1 = seedMedia("shot-1.png");
		Long shot2 = seedMedia("shot-2.png");

		Map<String, Object> body = projectBody("Real-Time Sync Engine");
		body.put("thumbnailMediaId", thumbnailId);
		body.put("galleryMediaIds", List.of(shot1, shot2));
		body.put("technologies", List.of("Spring Boot", "Redis"));
		body.put(
				"challenges",
				List.of(
						challenge("Ordering", "Events arrived out of order", "Added a vector clock"),
						challenge("Backpressure", "Consumers fell behind", "Bounded queue with shedding")));

		mockMvc.perform(adminPost("/api/v1/admin/projects", body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.slug").value("real-time-sync-engine"))
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andExpect(jsonPath("$.data.thumbnail.id").value(thumbnailId))
				.andExpect(jsonPath("$.data.technologies", org.hamcrest.Matchers.contains("Redis", "Spring Boot")))
				.andExpect(jsonPath("$.data.challenges.length()").value(2))
				.andExpect(jsonPath("$.data.challenges[0].title").value("Ordering"))
				.andExpect(jsonPath("$.data.challenges[0].solution").value("Added a vector clock"))
				.andExpect(jsonPath("$.data.challenges[1].displayOrder").value(1))
				.andExpect(jsonPath("$.data.gallery.length()").value(2))
				.andExpect(jsonPath("$.data.gallery[0].id").value(shot1))
				.andExpect(jsonPath("$.data.gallery[1].id").value(shot2));
	}

	@Test
	@DisplayName("a technology tag is reused, case-insensitively, rather than duplicated")
	void technologiesAreDeduplicated() throws Exception {
		Map<String, Object> first = projectBody("First");
		first.put("technologies", List.of("Spring Boot"));
		mockMvc.perform(adminPost("/api/v1/admin/projects", first)).andExpect(status().isCreated());

		Map<String, Object> second = projectBody("Second");
		second.put("technologies", List.of("spring boot", "SPRING BOOT", "Redis"));
		mockMvc.perform(adminPost("/api/v1/admin/projects", second))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.technologies.length()").value(2));

		assertThat(technologyRepository.count()).as("one row per technology").isEqualTo(2);
	}

	@Test
	@DisplayName("a derived slug that is already taken gets a numeric suffix")
	void derivedSlugsDoNotCollide() throws Exception {
		mockMvc.perform(adminPost("/api/v1/admin/projects", projectBody("Same Title")))
				.andExpect(jsonPath("$.data.slug").value("same-title"));
		mockMvc.perform(adminPost("/api/v1/admin/projects", projectBody("Same Title")))
				.andExpect(jsonPath("$.data.slug").value("same-title-2"));
	}

	@Test
	@DisplayName("an explicitly supplied slug that is taken is a 409, not a silent rename")
	void explicitSlugCollisionIsConflict() throws Exception {
		Map<String, Object> first = projectBody("First");
		first.put("slug", "chosen-slug");
		mockMvc.perform(adminPost("/api/v1/admin/projects", first)).andExpect(status().isCreated());

		Map<String, Object> second = projectBody("Second");
		second.put("slug", "chosen-slug");
		mockMvc.perform(adminPost("/api/v1/admin/projects", second))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CONFLICT"));
	}

	// ----------------------------------------------------------- validation

	@Test
	@DisplayName("a blank title collects into the field-level errors array")
	void validationErrorsAreFieldLevel() throws Exception {
		Map<String, Object> body = projectBody("valid");
		body.put("title", " ");

		mockMvc.perform(adminPost("/api/v1/admin/projects", body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors[0].field").value("title"));
	}

	@Test
	@DisplayName("a non-canonical slug and an end date before the start date are both rejected")
	void rejectsBadSlugAndDateRange() throws Exception {
		Map<String, Object> badSlug = projectBody("Title");
		badSlug.put("slug", "Not A Slug");
		mockMvc.perform(adminPost("/api/v1/admin/projects", badSlug))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		Map<String, Object> badDates = projectBody("Title");
		badDates.put("startDate", "2026-05-01");
		badDates.put("endDate", "2026-04-01");
		mockMvc.perform(adminPost("/api/v1/admin/projects", badDates))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("End date")));
	}

	@Test
	@DisplayName("referencing media that does not exist fails the whole write")
	void rejectsUnknownMedia() throws Exception {
		Map<String, Object> body = projectBody("Title");
		body.put("galleryMediaIds", List.of(999_999L));

		mockMvc.perform(adminPost("/api/v1/admin/projects", body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		assertThat(projectRepository.count()).isZero();
	}

	@Test
	@DisplayName("an unsortable field is rejected instead of quietly falling back")
	void rejectsUnknownSortField() throws Exception {
		mockMvc.perform(get("/api/v1/admin/projects").param("sort", "passwordHash").header(
						HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	// ------------------------------------------------------- publish / read

	@Test
	@DisplayName("a draft project is invisible to the public API — 404, never 403")
	void draftsAreInvisibleToThePublic() throws Exception {
		String slug = createProject("Hidden Project");

		mockMvc.perform(get("/api/v1/projects/{slug}", slug))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
		mockMvc.perform(get("/api/v1/projects")).andExpect(jsonPath("$.data.length()").value(0));

		// ...and the admin can still see it.
		mockMvc.perform(get("/api/v1/admin/projects").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	@DisplayName("publishing makes a project public; archiving hides it again")
	void publishAndArchiveFlipVisibility() throws Exception {
		String slug = createProject("Visible Project");
		Long id = idOfSlug(slug);

		setStatus(id, "PUBLISHED");
		mockMvc.perform(get("/api/v1/projects/{slug}", slug))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.title").value("Visible Project"));
		mockMvc.perform(get("/api/v1/projects")).andExpect(jsonPath("$.data.length()").value(1));

		setStatus(id, "ARCHIVED");
		mockMvc.perform(get("/api/v1/projects/{slug}", slug)).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/projects")).andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	@DisplayName("the public list is ordered by display order")
	void publicListIsOrdered() throws Exception {
		publish(createProjectWithOrder("Third", 30));
		publish(createProjectWithOrder("First", 10));
		publish(createProjectWithOrder("Second", 20));

		mockMvc.perform(get("/api/v1/projects"))
				.andExpect(jsonPath("$.data[0].title").value("First"))
				.andExpect(jsonPath("$.data[1].title").value("Second"))
				.andExpect(jsonPath("$.data[2].title").value("Third"));
	}

	@Test
	@DisplayName("the admin list filters by status")
	void adminListFiltersByStatus() throws Exception {
		publish(createProject("Published One"));
		createProject("Draft One");

		mockMvc.perform(get("/api/v1/admin/projects")
						.param("status", "PUBLISHED")
						.header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].title").value("Published One"));
	}

	// --------------------------------------------------------------- update

	@Test
	@DisplayName("update replaces the whole aggregate — omitted children are removed")
	void updateReplacesChildren() throws Exception {
		Long mediaId = seedMedia("shot.png");
		Map<String, Object> body = projectBody("Aggregate");
		body.put("technologies", List.of("Redis", "Kafka"));
		body.put("challenges", List.of(challenge("A", "c", "s"), challenge("B", "c", "s")));
		body.put("galleryMediaIds", List.of(mediaId));
		String created = mockMvc.perform(adminPost("/api/v1/admin/projects", body))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		long id = objectMapper.readTree(created).get("data").get("id").asLong();

		Map<String, Object> update = projectBody("Aggregate");
		update.put("technologies", List.of("Redis"));
		update.put("challenges", List.of(challenge("Only", "c", "s")));
		// challenges/gallery/technologies all shrink; gallery is omitted entirely
		mockMvc.perform(adminPut("/api/v1/admin/projects/" + id, update))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.technologies.length()").value(1))
				.andExpect(jsonPath("$.data.challenges.length()").value(1))
				.andExpect(jsonPath("$.data.challenges[0].title").value("Only"))
				.andExpect(jsonPath("$.data.gallery.length()").value(0));
	}

	@Test
	@DisplayName("update never changes status — publishing stays a deliberate separate call")
	void updateDoesNotChangeStatus() throws Exception {
		String slug = createProject("Stays Published");
		Long id = idOfSlug(slug);
		setStatus(id, "PUBLISHED");

		mockMvc.perform(adminPut("/api/v1/admin/projects/" + id, projectBody("Stays Published")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("PUBLISHED"));
	}

	@Test
	@DisplayName("a deleted thumbnail leaves the project readable, just without an image")
	void deletedMediaDoesNotBreakTheProject() throws Exception {
		Long mediaId = seedMedia("thumb.png");
		Map<String, Object> body = projectBody("Has Thumbnail");
		body.put("thumbnailMediaId", mediaId);
		body.put("galleryMediaIds", List.of(mediaId));
		mockMvc.perform(adminPost("/api/v1/admin/projects", body)).andExpect(status().isCreated());

		mockMvc.perform(delete("/api/v1/admin/media/{id}", mediaId).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNoContent());

		String slug = "has-thumbnail";
		setStatus(idOfSlug(slug), "PUBLISHED");
		mockMvc.perform(get("/api/v1/projects/{slug}", slug))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.thumbnail").doesNotExist())
				.andExpect(jsonPath("$.data.gallery.length()").value(0));
	}

	// ------------------------------------------------- delete / not-found / auth

	@Test
	@DisplayName("delete is a soft delete: the project disappears from both surfaces")
	void deleteHidesTheProject() throws Exception {
		String slug = createProject("Doomed");
		Long id = idOfSlug(slug);
		setStatus(id, "PUBLISHED");

		mockMvc.perform(delete("/api/v1/admin/projects/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/projects/{slug}", slug)).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/admin/projects/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("unknown ids and slugs are 404 on every route")
	void unknownProjectsAreNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/projects/{slug}", "no-such-project")).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/admin/projects/{id}", 999_999L).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/v1/admin/projects/{id}", 999_999L)
						.header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("every admin route requires a token while the public routes stay open")
	void adminRoutesRequireAuth() throws Exception {
		mockMvc.perform(get("/api/v1/admin/projects")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/admin/projects")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(projectBody("X"))))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/v1/admin/projects/1")).andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/projects")).andExpect(status().isOk());
	}

	// ---------------------------------------------------------------- utils

	private Map<String, Object> projectBody(String title) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("title", title);
		body.put("shortDescription", "A short description of " + title);
		return body;
	}

	private Map<String, Object> challenge(String title, String challenge, String solution) {
		return Map.of("title", title, "challenge", challenge, "solution", solution);
	}

	private String createProject(String title) throws Exception {
		String body = mockMvc.perform(adminPost("/api/v1/admin/projects", projectBody(title)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(body).get("data").get("slug").asText();
	}

	private String createProjectWithOrder(String title, int displayOrder) throws Exception {
		Map<String, Object> body = projectBody(title);
		body.put("displayOrder", displayOrder);
		String response = mockMvc.perform(adminPost("/api/v1/admin/projects", body))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(response).get("data").get("slug").asText();
	}

	private void publish(String slug) throws Exception {
		setStatus(idOfSlug(slug), "PUBLISHED");
	}

	private Long idOfSlug(String slug) throws Exception {
		String body = mockMvc.perform(get("/api/v1/admin/projects")
						.param("size", "100")
						.header(HttpHeaders.AUTHORIZATION, bearer()))
				.andReturn()
				.getResponse()
				.getContentAsString();
		for (JsonNode node : objectMapper.readTree(body).get("data").get("content")) {
			if (slug.equals(node.get("slug").asText())) {
				return node.get("id").asLong();
			}
		}
		throw new AssertionError("No project with slug " + slug);
	}

	private void setStatus(Long id, String status) throws Exception {
		mockMvc.perform(patch("/api/v1/admin/projects/{id}/status", id)
						.header(HttpHeaders.AUTHORIZATION, bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"" + status + "\"}"))
				.andExpect(status().isOk());
	}

	private Long seedMedia(String fileName) {
		Media media = mediaRepository.save(new Media(
				"stored-" + fileName, fileName, "image/png", 100L, StorageBackend.LOCAL,
				"2026/08/stored-" + fileName, 10, 10, null, null));
		return media.getId();
	}

	private MockHttpServletRequestBuilder adminPost(String path, Object body) throws Exception {
		return post(path)
				.header(HttpHeaders.AUTHORIZATION, bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body));
	}

	private MockHttpServletRequestBuilder adminPut(String path, Object body) throws Exception {
		return put(path)
				.header(HttpHeaders.AUTHORIZATION, bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body));
	}

	private String bearer() {
		return "Bearer " + accessToken;
	}
}
