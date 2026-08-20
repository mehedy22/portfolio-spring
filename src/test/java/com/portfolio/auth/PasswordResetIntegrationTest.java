package com.portfolio.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.ContentModuleTestBase;
import com.portfolio.auth.repository.AdminRepository;
import com.portfolio.security.PasswordResetTokenStore;
import java.time.Duration;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Sprint 10 acceptance for FR-16. The security properties get direct tests, not the happy path alone. */
class PasswordResetIntegrationTest extends ContentModuleTestBase {

	@Autowired
	private AdminRepository adminRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private PasswordResetTokenStore tokenStore;

	@Autowired
	private StringRedisTemplate redis;

	@BeforeEach
	void flushRedis() {
		Objects.requireNonNull(redis.getConnectionFactory()).getConnection().serverCommands().flushAll();
	}

	private String adminEmail() {
		return adminRepository.findAll().get(0).getEmail();
	}

	private Long adminId() {
		return adminRepository.findAll().get(0).getId();
	}

	@Test
	@DisplayName("a request for a known and an unknown address are indistinguishable")
	void noAccountEnumeration() throws Exception {
		String known = body(request(adminEmail(), "10.9.0.1"));
		String unknown = body(request("nobody@example.com", "10.9.0.2"));

		assertThat(strip(known))
				.as("the response must not reveal whether the address exists")
				.isEqualTo(strip(unknown));
	}

	@Test
	@DisplayName("a valid token sets the new password, and the old one stops working")
	void resetChangesThePassword() throws Exception {
		String token = tokenStore.issue(adminId(), Duration.ofMinutes(30));

		mockMvc.perform(confirm(token, "a-brand-new-password"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		var admin = adminRepository.findById(adminId()).orElseThrow();
		assertThat(passwordEncoder.matches("a-brand-new-password", admin.getPasswordHash())).isTrue();
		assertThat(passwordEncoder.matches("irrelevant", admin.getPasswordHash())).isFalse();
	}

	@Test
	@DisplayName("a token works exactly once")
	void tokensAreSingleUse() throws Exception {
		String token = tokenStore.issue(adminId(), Duration.ofMinutes(30));

		mockMvc.perform(confirm(token, "first-new-password")).andExpect(status().isOk());
		mockMvc.perform(confirm(token, "second-new-password"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("an unknown token is rejected")
	void unknownTokenRejected() throws Exception {
		mockMvc.perform(confirm("not-a-real-token", "some-new-password"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("resetting revokes any live refresh token, so an open session ends")
	void resetEndsLiveSessions() throws Exception {
		// Log in to establish a refresh token in the allowlist.
		String loginBody = mockMvc.perform(post("/api/v1/auth/login")
						.header("X-Forwarded-For", "10.9.1.1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + adminEmail() + "\",\"password\":\"irrelevant\"}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(loginBody).contains("accessToken");
		assertThat(redis.hasKey("auth:refresh:" + adminId())).isTrue();

		mockMvc.perform(confirm(tokenStore.issue(adminId(), Duration.ofMinutes(30)), "another-new-password"))
				.andExpect(status().isOk());

		assertThat(redis.hasKey("auth:refresh:" + adminId()))
				.as("the live session's refresh token is gone")
				.isFalse();
	}

	@Test
	@DisplayName("a short password is rejected with a field-level message")
	void weakPasswordRejected() throws Exception {
		mockMvc.perform(confirm(tokenStore.issue(adminId(), Duration.ofMinutes(30)), "short"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("newPassword"));
	}

	@Test
	@DisplayName("requests are throttled per IP")
	void throttled() throws Exception {
		for (int i = 0; i < 5; i++) {
			mockMvc.perform(request(adminEmail(), "10.9.2.1")).andExpect(status().isOk());
		}
		mockMvc.perform(request(adminEmail(), "10.9.2.1"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("RATE_LIMITED"));
	}

	// ---------------------------------------------------------------- utils

	private MockHttpServletRequestBuilder request(String email, String ip) {
		return post("/api/v1/auth/password-reset/request")
				.header("X-Forwarded-For", ip)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\"}");
	}

	private MockHttpServletRequestBuilder confirm(String token, String password) {
		return post("/api/v1/auth/password-reset/confirm")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"" + token + "\",\"newPassword\":\"" + password + "\"}");
	}

	private String body(MockHttpServletRequestBuilder builder) throws Exception {
		return mockMvc.perform(builder).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
	}

	private String strip(String json) {
		return json.replaceAll("\"timestamp\":\"[^\"]*\"", "");
	}
}
