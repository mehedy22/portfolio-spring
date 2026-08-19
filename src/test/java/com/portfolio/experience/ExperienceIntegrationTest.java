package com.portfolio.experience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.ContentModuleTestBase;
import com.portfolio.experience.repository.ExperienceRepository;
import com.portfolio.technology.repository.TechnologyRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

class ExperienceIntegrationTest extends ContentModuleTestBase {

	@Autowired
	private ExperienceRepository experienceRepository;

	@Autowired
	private TechnologyRepository technologyRepository;

	@BeforeEach
	void clear() {
		truncate("experience_technology", "experience", "project_technology", "project", "technology");
	}

	@Test
	@DisplayName("creating a role stores its logo and technologies, and defaults to DRAFT")
	void createRoundTrips() throws Exception {
		Long logoId = seedMedia("acme.png");
		Map<String, Object> body = role("Acme", "Staff Engineer");
		body.put("companyLogoMediaId", logoId);
		body.put("employmentType", "FULL_TIME");
		body.put("technologies", List.of("Spring Boot", "Kafka"));

		mockMvc.perform(adminPost("/api/v1/admin/experience", body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andExpect(jsonPath("$.data.employmentType").value("FULL_TIME"))
				.andExpect(jsonPath("$.data.companyLogo.id").value(logoId))
				.andExpect(jsonPath("$.data.technologies", org.hamcrest.Matchers.contains("Kafka", "Spring Boot")));
	}

	@Test
	@DisplayName("experience shares the technology lookup with projects rather than duplicating rows")
	void technologyLookupIsShared() throws Exception {
		Map<String, Object> project = new LinkedHashMap<>();
		project.put("title", "Some Project");
		project.put("shortDescription", "desc");
		project.put("technologies", List.of("Kafka"));
		mockMvc.perform(adminPost("/api/v1/admin/projects", project)).andExpect(status().isCreated());

		Map<String, Object> body = role("Acme", "Engineer");
		body.put("technologies", List.of("kafka"));
		mockMvc.perform(adminPost("/api/v1/admin/experience", body)).andExpect(status().isCreated());

		assertThat(technologyRepository.count()).as("one Kafka row, shared").isEqualTo(1);
	}

	@Test
	@DisplayName("a draft role is invisible publicly; publishing reveals it")
	void draftsAreInvisible() throws Exception {
		Long id = idOf(mockMvc.perform(adminPost("/api/v1/admin/experience", role("Acme", "Engineer")))
				.andReturn()
				.getResponse()
				.getContentAsString());

		mockMvc.perform(get("/api/v1/experience")).andExpect(jsonPath("$.data.length()").value(0));

		Map<String, Object> published = role("Acme", "Engineer");
		published.put("status", "PUBLISHED");
		mockMvc.perform(adminPut("/api/v1/admin/experience/" + id, published)).andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/experience"))
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].company").value("Acme"));
	}

	@Test
	@DisplayName("a role marked current cannot also have an end date")
	void currentRoleCannotHaveEndDate() throws Exception {
		Map<String, Object> body = role("Acme", "Engineer");
		body.put("currentlyWorking", true);
		body.put("endDate", "2026-01-01");

		mockMvc.perform(adminPost("/api/v1/admin/experience", body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("current")));
	}

	@Test
	@DisplayName("an end date before the start date is rejected, and a start date is required")
	void datesAreValidated() throws Exception {
		Map<String, Object> reversed = role("Acme", "Engineer");
		reversed.put("endDate", "2019-01-01");
		mockMvc.perform(adminPost("/api/v1/admin/experience", reversed))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		Map<String, Object> noStart = new LinkedHashMap<>();
		noStart.put("company", "Acme");
		noStart.put("position", "Engineer");
		mockMvc.perform(adminPost("/api/v1/admin/experience", noStart))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("startDate"));
	}

	@Test
	@DisplayName("update replaces the technology set, and delete hides the row from both surfaces")
	void updateAndDelete() throws Exception {
		Map<String, Object> body = role("Acme", "Engineer");
		body.put("status", "PUBLISHED");
		body.put("technologies", List.of("Redis", "Kafka"));
		Long id = idOf(mockMvc.perform(adminPost("/api/v1/admin/experience", body))
				.andReturn()
				.getResponse()
				.getContentAsString());

		Map<String, Object> update = role("Acme", "Senior Engineer");
		update.put("status", "PUBLISHED");
		update.put("technologies", List.of("Redis"));
		mockMvc.perform(adminPut("/api/v1/admin/experience/" + id, update))
				.andExpect(jsonPath("$.data.position").value("Senior Engineer"))
				.andExpect(jsonPath("$.data.technologies.length()").value(1));

		mockMvc.perform(delete("/api/v1/admin/experience/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/experience")).andExpect(jsonPath("$.data.length()").value(0));
		mockMvc.perform(get("/api/v1/admin/experience/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("unknown media is rejected, and admin routes need a token while the public one does not")
	void mediaAndAuthGuards() throws Exception {
		Map<String, Object> body = role("Acme", "Engineer");
		body.put("companyLogoMediaId", 999_999L);
		mockMvc.perform(adminPost("/api/v1/admin/experience", body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(get("/api/v1/admin/experience")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/experience")).andExpect(status().isOk());
	}

	private Map<String, Object> role(String company, String position) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("company", company);
		body.put("position", position);
		body.put("startDate", "2020-01-01");
		return body;
	}
}
