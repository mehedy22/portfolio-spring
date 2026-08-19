package com.portfolio.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.ContentModuleTestBase;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Sprint 6 acceptance.
 *
 * <p>The sprint's DoD — "changing a setting in the Admin Panel reflects on the Public Site without
 * a redeploy" — is cross-app and cannot be demonstrated end to end while the frontends are
 * deferred. What is verifiable, and is tested here, is the backend half of that promise: an admin
 * write is immediately visible on the public endpoint the Public Site reads, with no restart, no
 * cache to invalidate, and no value baked into the response at build time.
 */
class SettingsIntegrationTest extends ContentModuleTestBase {

	@BeforeEach
	void clear() {
		truncate("site_setting", "social_link", "site_profile");
	}

	// ------------------------------------------------------------ the DoD

	@Test
	@DisplayName("an admin change is visible on the public endpoint on the very next request")
	void adminChangeReflectsPublicly() throws Exception {
		mockMvc.perform(get("/api/v1/settings"))
				.andExpect(jsonPath("$.data.settings['site.title']").value("My Portfolio"));

		mockMvc.perform(putSettings(Map.of("site.title", "Nurul's Portfolio", "site.tagline", "Backend engineer")))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/settings"))
				.andExpect(jsonPath("$.data.settings['site.title']").value("Nurul's Portfolio"))
				.andExpect(jsonPath("$.data.settings['site.tagline']").value("Backend engineer"));
	}

	// -------------------------------------------------------- the registry

	@Test
	@DisplayName("private settings never appear on the public endpoint")
	void privateSettingsStayPrivate() throws Exception {
		mockMvc.perform(putSettings(Map.of("contact.notification_email", "admin@example.com")))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/settings"))
				.andExpect(jsonPath("$.data.settings['contact.notification_email']").doesNotExist())
				.andExpect(jsonPath("$.data.seo['contact.notification_email']").doesNotExist());

		// ...but the admin still sees it.
		mockMvc.perform(get("/api/v1/admin/settings").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(jsonPath("$.data.settings['contact.notification_email']").value("admin@example.com"));
	}

	@Test
	@DisplayName("every known key is present with its default before anything is stored")
	void defaultsFillInWithoutSeeding() throws Exception {
		mockMvc.perform(get("/api/v1/admin/settings").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(jsonPath("$.data.settings['site.title']").value("My Portfolio"))
				.andExpect(jsonPath("$.data.settings['nav.show_articles']").value("false"))
				.andExpect(jsonPath("$.data.settings['site.tagline']").value(""));
	}

	@Test
	@DisplayName("an unknown key is rejected instead of being silently stored")
	void unknownKeysAreRejected() throws Exception {
		mockMvc.perform(putSettings(Map.of("site.colour_scheme", "neon")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unknown setting")));
	}

	@Test
	@DisplayName("a key belonging to another group cannot be changed through the wrong screen")
	void groupsAreEnforced() throws Exception {
		mockMvc.perform(putSettings(Map.of("seo.default_title", "Nope")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SEO group")));

		mockMvc.perform(put("/api/v1/admin/settings/seo")
						.header(HttpHeaders.AUTHORIZATION, bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"settings\":{\"seo.default_title\":\"Works here\"}}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.settings['seo.default_title']").value("Works here"));
	}

	@Test
	@DisplayName("a boolean setting rejects a non-boolean value")
	void booleanSettingsAreTypeChecked() throws Exception {
		mockMvc.perform(putSettings(Map.of("nav.show_articles", "yes")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("true or false")));

		mockMvc.perform(putSettings(Map.of("nav.show_articles", "true"))).andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/settings"))
				.andExpect(jsonPath("$.data.settings['nav.show_articles']").value("true"));
	}

	@Test
	@DisplayName("a partial update leaves other keys alone, and a null resets one to its default")
	void partialUpdateAndReset() throws Exception {
		mockMvc.perform(putSettings(Map.of("site.title", "Set", "site.footer_text", "Footer")))
				.andExpect(status().isOk());
		mockMvc.perform(putSettings(Map.of("site.footer_text", "Changed")))
				.andExpect(jsonPath("$.data.settings['site.title']").value("Set"))
				.andExpect(jsonPath("$.data.settings['site.footer_text']").value("Changed"));

		Map<String, String> reset = new HashMap<>();
		reset.put("site.title", null);
		mockMvc.perform(putSettings(reset))
				.andExpect(jsonPath("$.data.settings['site.title']").value("My Portfolio"));
	}

	// ---------------------------------------------------------- social links

	@Test
	@DisplayName("social links replace wholesale, keep the order sent, and hide invisible ones publicly")
	void socialLinks() throws Exception {
		mockMvc.perform(putSocialLinks(List.of(
						link("GitHub", "https://github.com/example", null),
						link("LinkedIn", "https://linkedin.com/in/example", null),
						link("Draft Network", "https://example.com", false))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(3))
				.andExpect(jsonPath("$.data[0].platform").value("GitHub"))
				.andExpect(jsonPath("$.data[0].displayOrder").value(0))
				.andExpect(jsonPath("$.data[2].visible").value(false));

		mockMvc.perform(get("/api/v1/settings"))
				.andExpect(jsonPath("$.data.socialLinks.length()").value(2))
				.andExpect(jsonPath("$.data.socialLinks[0].platform").value("GitHub"))
				.andExpect(jsonPath("$.data.socialLinks[1].platform").value("LinkedIn"));

		// Replacing with a shorter list drops the rest.
        mockMvc.perform(putSocialLinks(List.of(link("GitHub", "https://github.com/example", null))))
				.andExpect(jsonPath("$.data.length()").value(1));
		mockMvc.perform(get("/api/v1/admin/settings/social-links").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(jsonPath("$.data.length()").value(1));
	}

	@Test
	@DisplayName("a social link needs a platform and a url")
	void socialLinkValidation() throws Exception {
		mockMvc.perform(putSocialLinks(List.of(link("", "", null))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors.length()").value(2));
	}

	// --------------------------------------------------------------- profile

	@Test
	@DisplayName("the profile is empty rather than 404 before anything is set")
	void profileStartsEmpty() throws Exception {
		mockMvc.perform(get("/api/v1/settings/profile"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.profileImage").doesNotExist())
				.andExpect(jsonPath("$.data.resume").doesNotExist());
	}

	@Test
	@DisplayName("profile photo and resume are set from uploaded media and served publicly")
	void profileReferencesMedia() throws Exception {
		Long photo = seedMedia("me.png");
		Long resume = seedMedia("cv.pdf");

		mockMvc.perform(putProfile(photo, resume))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.profileImage.id").value(photo))
				.andExpect(jsonPath("$.data.resume.id").value(resume));

		mockMvc.perform(get("/api/v1/settings/profile"))
				.andExpect(jsonPath("$.data.profileImage.url").value("/api/v1/media/" + photo + "/content"))
				.andExpect(jsonPath("$.data.resume.url").value("/api/v1/media/" + resume + "/content"));

		// Clearing one keeps the underlying file; it only drops the reference.
		mockMvc.perform(putProfile(null, resume))
				.andExpect(jsonPath("$.data.profileImage").doesNotExist())
				.andExpect(jsonPath("$.data.resume.id").value(resume));
		assertThat(mediaRepository.findById(photo)).as("the file itself survives").isPresent();
	}

	@Test
	@DisplayName("a deleted profile photo leaves the endpoint working, just without a photo")
	void deletedMediaDoesNotBreakTheProfile() throws Exception {
		Long photo = seedMedia("me.png");
		mockMvc.perform(putProfile(photo, null)).andExpect(status().isOk());

		mockMvc.perform(delete("/api/v1/admin/media/{id}", photo).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/settings/profile"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.profileImage").doesNotExist());
	}

	@Test
	@DisplayName("referencing media that does not exist is rejected")
	void unknownMediaRejected() throws Exception {
		mockMvc.perform(putProfile(999_999L, null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	// ------------------------------------------------------------------ auth

	@Test
	@DisplayName("reads are public; every write needs a token")
	void authBoundary() throws Exception {
		mockMvc.perform(get("/api/v1/settings")).andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/settings/profile")).andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/admin/settings")).andExpect(status().isUnauthorized());
		mockMvc.perform(put("/api/v1/admin/settings")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"settings\":{\"site.title\":\"Hijacked\"}}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(put("/api/v1/admin/settings/social-links")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"links\":[]}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(put("/api/v1/admin/settings/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/settings"))
				.andExpect(jsonPath("$.data.settings['site.title']").value("My Portfolio"));
	}

	// ----------------------------------------------------------------- utils

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder putSettings(
			Map<String, String> settings) throws Exception {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("settings", settings);
		return put("/api/v1/admin/settings")
				.header(HttpHeaders.AUTHORIZATION, bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body));
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder putSocialLinks(
			List<Map<String, Object>> links) throws Exception {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("links", links);
		return put("/api/v1/admin/settings/social-links")
				.header(HttpHeaders.AUTHORIZATION, bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body));
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder putProfile(
			Long photoId, Long resumeId) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("profileImageMediaId", photoId);
		body.put("resumeMediaId", resumeId);
		return put("/api/v1/admin/settings/profile")
				.header(HttpHeaders.AUTHORIZATION, bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body));
	}

	private Map<String, Object> link(String platform, String url, Boolean visible) {
		Map<String, Object> body = new HashMap<>();
		body.put("platform", platform);
		body.put("url", url);
		body.put("visible", visible);
		return body;
	}
}
