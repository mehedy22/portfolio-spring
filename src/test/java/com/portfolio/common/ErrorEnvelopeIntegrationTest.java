package com.portfolio.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.ContentModuleTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Malformed input must reach the documented error envelope rather than the generic 500 handler.
 * These are cross-cutting rather than per-module, so they live here instead of being repeated in
 * every content module's suite.
 */
class ErrorEnvelopeIntegrationTest extends ContentModuleTestBase {

	@Test
	@DisplayName("a value outside an enum's allowed set is a 400, not a 500")
	void invalidEnumInBodyIsBadRequest() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/skills")
						.header(HttpHeaders.AUTHORIZATION, bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Rust\",\"category\":\"Systems\",\"proficiency\":\"WIZARD\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	@DisplayName("malformed JSON is a 400, not a 500")
	void malformedJsonIsBadRequest() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/education")
						.header(HttpHeaders.AUTHORIZATION, bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"institution\": "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	@DisplayName("an invalid ?status= query value is a 400, not a 500")
	void invalidEnumInQueryIsBadRequest() throws Exception {
		mockMvc.perform(get("/api/v1/admin/projects")
						.param("status", "NOPE")
						.header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("status")));
	}

	@Test
	@DisplayName("a non-numeric path id is a 400, not a 500")
	void invalidPathVariableIsBadRequest() throws Exception {
		mockMvc.perform(get("/api/v1/admin/certifications/{id}", "not-a-number")
						.header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}
}
