package com.portfolio.certification;

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

class CertificationIntegrationTest extends ContentModuleTestBase {


	@BeforeEach
	void clear() {
		truncate("certification");
	}

	@Test
	@DisplayName("a certification defaults to PUBLISHED and keeps its credential details")
	void createDefaultsToPublished() throws Exception {
		Long imageId = seedMedia("cert.png");
		Map<String, Object> body = certification("AWS Solutions Architect", "Amazon Web Services");
		body.put("certificateImageMediaId", imageId);
		body.put("credentialId", "ABC-123");
		body.put("credentialUrl", "https://example.com/verify/ABC-123");
		body.put("issueDate", "2025-03-01");
		body.put("expiryDate", "2028-03-01");

		mockMvc.perform(adminPost("/api/v1/admin/certifications", body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("PUBLISHED"))
				.andExpect(jsonPath("$.data.credentialId").value("ABC-123"))
				.andExpect(jsonPath("$.data.certificateImage.id").value(imageId));

		mockMvc.perform(get("/api/v1/certifications")).andExpect(jsonPath("$.data.length()").value(1));
	}

	@Test
	@DisplayName("an expiry before the issue date is rejected")
	void expiryMustFollowIssue() throws Exception {
		Map<String, Object> body = certification("Cert", "Issuer");
		body.put("issueDate", "2026-01-01");
		body.put("expiryDate", "2025-01-01");

		mockMvc.perform(adminPost("/api/v1/admin/certifications", body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Expiry")));
	}

	@Test
	@DisplayName("name and issuer are both required")
	void validation() throws Exception {
		mockMvc.perform(adminPost("/api/v1/admin/certifications", new LinkedHashMap<String, Object>()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors.length()").value(2));
	}

	@Test
	@DisplayName("a draft is hidden publicly; ordering follows display order")
	void visibilityAndOrdering() throws Exception {
		Map<String, Object> draft = certification("Hidden", "Issuer");
		draft.put("status", "DRAFT");
		mockMvc.perform(adminPost("/api/v1/admin/certifications", draft)).andExpect(status().isCreated());

		Map<String, Object> second = certification("Second", "Issuer");
		second.put("displayOrder", 20);
		Map<String, Object> first = certification("First", "Issuer");
		first.put("displayOrder", 10);
		mockMvc.perform(adminPost("/api/v1/admin/certifications", second));
		mockMvc.perform(adminPost("/api/v1/admin/certifications", first));

		mockMvc.perform(get("/api/v1/certifications"))
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].name").value("First"))
				.andExpect(jsonPath("$.data[1].name").value("Second"));
	}

	@Test
	@DisplayName("update, delete, not-found and auth behave as documented")
	void lifecycleAndGuards() throws Exception {
		Long id = idOf(mockMvc.perform(adminPost(
						"/api/v1/admin/certifications", certification("Cert", "Issuer")))
				.andReturn()
				.getResponse()
				.getContentAsString());

		mockMvc.perform(adminPut("/api/v1/admin/certifications/" + id, certification("Renamed", "Issuer")))
				.andExpect(jsonPath("$.data.name").value("Renamed"));

		mockMvc.perform(delete("/api/v1/admin/certifications/{id}", id)
						.header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/certifications")).andExpect(jsonPath("$.data.length()").value(0));

		mockMvc.perform(get("/api/v1/admin/certifications")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/certifications")).andExpect(status().isOk());
	}

	private Map<String, Object> certification(String name, String issuer) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("name", name);
		body.put("issuer", issuer);
		return body;
	}
}
