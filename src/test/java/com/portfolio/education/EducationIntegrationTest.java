package com.portfolio.education;

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

class EducationIntegrationTest extends ContentModuleTestBase {


	@BeforeEach
	void clear() {
		truncate("education");
	}

	@Test
	@DisplayName("education defaults to PUBLISHED and is immediately public")
	void createDefaultsToPublished() throws Exception {
		Long logoId = seedMedia("uni.png");
		Map<String, Object> body = education("State University");
		body.put("logoMediaId", logoId);
		body.put("degree", "BSc");
		body.put("field", "Computer Science");

		mockMvc.perform(adminPost("/api/v1/admin/education", body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("PUBLISHED"))
				.andExpect(jsonPath("$.data.logo.id").value(logoId));

		mockMvc.perform(get("/api/v1/education"))
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].degree").value("BSc"));
	}

	@Test
	@DisplayName("a draft entry is hidden from the public list but visible to the admin")
	void draftsAreInvisible() throws Exception {
		Map<String, Object> body = education("Hidden Academy");
		body.put("status", "DRAFT");
		mockMvc.perform(adminPost("/api/v1/admin/education", body)).andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/education")).andExpect(jsonPath("$.data.length()").value(0));
		mockMvc.perform(get("/api/v1/admin/education")
						.param("status", "DRAFT")
						.header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(jsonPath("$.data.length()").value(1));
	}

	@Test
	@DisplayName("the public list is ordered by display order")
	void ordering() throws Exception {
		mockMvc.perform(adminPost("/api/v1/admin/education", ordered("Second", 20)));
		mockMvc.perform(adminPost("/api/v1/admin/education", ordered("First", 10)));

		mockMvc.perform(get("/api/v1/education"))
				.andExpect(jsonPath("$.data[0].institution").value("First"))
				.andExpect(jsonPath("$.data[1].institution").value("Second"));
	}

	@Test
	@DisplayName("validation: institution is required, and dates must be in order")
	void validation() throws Exception {
		mockMvc.perform(adminPost("/api/v1/admin/education", new LinkedHashMap<String, Object>()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("institution"));

		Map<String, Object> reversed = education("State University");
		reversed.put("startDate", "2020-01-01");
		reversed.put("endDate", "2019-01-01");
		mockMvc.perform(adminPost("/api/v1/admin/education", reversed))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	@DisplayName("update, delete, not-found and auth behave as documented")
	void lifecycleAndGuards() throws Exception {
		Long id = idOf(mockMvc.perform(adminPost("/api/v1/admin/education", education("State University")))
				.andReturn()
				.getResponse()
				.getContentAsString());

		mockMvc.perform(adminPut("/api/v1/admin/education/" + id, education("Renamed University")))
				.andExpect(jsonPath("$.data.institution").value("Renamed University"));

		mockMvc.perform(delete("/api/v1/admin/education/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/admin/education/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/v1/admin/education")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/education")).andExpect(status().isOk());
	}

	private Map<String, Object> education(String institution) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("institution", institution);
		return body;
	}

	private Map<String, Object> ordered(String institution, int displayOrder) {
		Map<String, Object> body = education(institution);
		body.put("displayOrder", displayOrder);
		return body;
	}
}
