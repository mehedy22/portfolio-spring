package com.portfolio.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.IntegrationTestBase;
import com.portfolio.auth.entity.Admin;
import com.portfolio.auth.repository.AdminRepository;
import jakarta.servlet.http.Cookie;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

/**
 * Sprint 1 auth acceptance: happy path, validation, authorization, and the two subtle
 * security behaviours — refresh rotation (Redis jti allowlist) and no account enumeration.
 *
 * <p>Each test uses a distinct {@code X-Forwarded-For} value so the per-IP login rate limiter does
 * not bleed between tests.
 */
@AutoConfigureMockMvc
class AuthIntegrationTest extends IntegrationTestBase {

	private static final String EMAIL = "admin@yourname.dev";
	private static final String PASSWORD = "correct-horse-battery-staple";
	private static final String REFRESH_COOKIE = "refreshToken";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminRepository adminRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private StringRedisTemplate redis;

	@Autowired
	private ObjectMapper objectMapper;

	/** The bootstrap runner must not race with the seeded fixture. */
	@MockitoBean
	private AdminBootstrap adminBootstrap;

	@BeforeEach
	void seedAdmin() {
		adminRepository.deleteAll();
		adminRepository.save(new Admin(EMAIL, passwordEncoder.encode(PASSWORD)));
		Objects.requireNonNull(redis.getConnectionFactory()).getConnection().serverCommands().flushAll();
	}

	// ---------------------------------------------------------------- login

	@Test
	@DisplayName("login with valid credentials returns an access token and sets an httpOnly refresh cookie")
	void loginSucceeds() throws Exception {
		mockMvc.perform(login(EMAIL, PASSWORD, "10.0.0.1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.data.expiresInSeconds").value(900))
				.andExpect(cookie().exists(REFRESH_COOKIE))
				.andExpect(cookie().httpOnly(REFRESH_COOKIE, true));
	}

	@Test
	@DisplayName("login response never carries the refresh token in the body")
	void loginBodyOmitsRefreshToken() throws Exception {
		String body = mockMvc.perform(login(EMAIL, PASSWORD, "10.0.0.2"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(body).doesNotContain("refreshToken");
	}

	@Test
	@DisplayName("login with the wrong password returns 401 UNAUTHORIZED")
	void loginWrongPassword() throws Exception {
		mockMvc.perform(login(EMAIL, "wrong-password", "10.0.0.3"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("unknown email and wrong password are indistinguishable — no account enumeration")
	void loginDoesNotEnumerateAccounts() throws Exception {
		JsonNode wrongPassword = errorBody(login(EMAIL, "wrong-password", "10.0.0.4"));
		JsonNode unknownEmail = errorBody(login("nobody@example.com", PASSWORD, "10.0.0.5"));

		assertThat(unknownEmail.get("status").asInt()).isEqualTo(wrongPassword.get("status").asInt());
		assertThat(unknownEmail.get("code").asText()).isEqualTo(wrongPassword.get("code").asText());
		// The message is the discriminator an attacker would probe — it must be byte-identical.
		assertThat(unknownEmail.get("message").asText()).isEqualTo(wrongPassword.get("message").asText());
		assertThat(unknownEmail.get("message").asText().toLowerCase())
				.doesNotContain("not found")
				.doesNotContain("no such");
	}

	@Test
	@DisplayName("login with a malformed email returns 400 VALIDATION_ERROR with a populated errors[]")
	void loginValidationFailure() throws Exception {
		mockMvc.perform(login("not-an-email", "", "10.0.0.6"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors").isNotEmpty())
				.andExpect(jsonPath("$.errors[*].field").exists());
	}

	@Test
	@DisplayName("a 6th failed login from the same IP within the window returns 429 RATE_LIMITED")
	void loginRateLimited() throws Exception {
		String ip = "10.0.0.7";
		for (int attempt = 1; attempt <= 5; attempt++) {
			mockMvc.perform(login(EMAIL, "wrong-password", ip)).andExpect(status().isUnauthorized());
		}

		mockMvc.perform(login(EMAIL, "wrong-password", ip))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("RATE_LIMITED"));
	}

	@Test
	@DisplayName("a successful login clears the failed-attempt counter")
	void successfulLoginResetsRateLimitCounter() throws Exception {
		String ip = "10.0.0.8";
		for (int attempt = 1; attempt <= 4; attempt++) {
			mockMvc.perform(login(EMAIL, "wrong-password", ip)).andExpect(status().isUnauthorized());
		}
		mockMvc.perform(login(EMAIL, PASSWORD, ip)).andExpect(status().isOk());

		// Without a reset the next four failures would trip the limit; they must not.
		for (int attempt = 1; attempt <= 4; attempt++) {
			mockMvc.perform(login(EMAIL, "wrong-password", ip)).andExpect(status().isUnauthorized());
		}
	}

	// -------------------------------------------------------------- refresh

	@Test
	@DisplayName("refresh with a valid cookie issues a new token pair")
	void refreshSucceeds() throws Exception {
		Cookie refreshCookie = refreshCookieFrom(login(EMAIL, PASSWORD, "10.0.1.1"));

		MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
				.andExpect(cookie().exists(REFRESH_COOKIE))
				.andReturn();

		String rotated = Objects.requireNonNull(refreshed.getResponse().getCookie(REFRESH_COOKIE)).getValue();
		assertThat(rotated).isNotBlank().isNotEqualTo(refreshCookie.getValue());
	}

	@Test
	@DisplayName("reusing a rotated-away refresh token returns 401 — the Redis jti allowlist rejects it")
	void refreshRotationRejectsReuse() throws Exception {
		Cookie original = refreshCookieFrom(login(EMAIL, PASSWORD, "10.0.1.2"));

		// First use rotates the allowlisted jti to the newly issued token.
		mockMvc.perform(post("/api/v1/auth/refresh").cookie(original)).andExpect(status().isOk());

		// The original is now stale: cryptographically valid, but no longer the allowlisted jti.
		mockMvc.perform(post("/api/v1/auth/refresh").cookie(original))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("refresh without a cookie returns 401")
	void refreshWithoutCookie() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("an access token is not accepted as a refresh token")
	void accessTokenIsNotAValidRefreshToken() throws Exception {
		String accessToken = accessTokenFrom(login(EMAIL, PASSWORD, "10.0.1.3"));

		mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, accessToken)))
				.andExpect(status().isUnauthorized());
	}

	// --------------------------------------------------------------- logout

	@Test
	@DisplayName("after logout the refresh token no longer works and the cookie is cleared")
	void logoutRevokesRefreshToken() throws Exception {
		Cookie refreshCookie = refreshCookieFrom(login(EMAIL, PASSWORD, "10.0.2.1"));

		mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshCookie))
				.andExpect(status().isOk())
				.andExpect(cookie().maxAge(REFRESH_COOKIE, 0));

		mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	// ------------------------------------------------------- authorization

	@Test
	@DisplayName("GET /api/v1/admin/me without a token returns 401 in the project's error envelope")
	void meRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/admin/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.path").value("/api/v1/admin/me"));
	}

	@Test
	@DisplayName("GET /api/v1/admin/me with a garbage token returns 401")
	void meRejectsInvalidToken() throws Exception {
		mockMvc.perform(get("/api/v1/admin/me").header("Authorization", "Bearer not-a-real-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("GET /api/v1/admin/me with a valid access token returns the admin's profile")
	void meReturnsAdminProfile() throws Exception {
		String accessToken = accessTokenFrom(login(EMAIL, PASSWORD, "10.0.3.1"));

		mockMvc.perform(get("/api/v1/admin/me").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.email").value(EMAIL))
				.andExpect(jsonPath("$.data.id").isNumber())
				.andExpect(jsonPath("$.data.lastLoginAt").isNotEmpty());
	}

	@Test
	@DisplayName("public routes stay reachable without a token — the admin rule did not lock down the app")
	void publicRoutesRemainOpen() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	// -------------------------------------------------------------- helpers

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(
			String email, String password, String clientIp) throws Exception {

		String payload = objectMapper.writeValueAsString(new LoginPayload(email, password));
		return post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Forwarded-For", clientIp)
				.content(payload);
	}

	private Cookie refreshCookieFrom(
			org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
		Cookie cookie = mockMvc.perform(request)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getCookie(REFRESH_COOKIE);
		return Objects.requireNonNull(cookie, "login did not set a refresh cookie");
	}

	private String accessTokenFrom(
			org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
		String body = mockMvc.perform(request)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(body).path("data").path("accessToken").asText();
	}

	private JsonNode errorBody(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
			throws Exception {
		String body = mockMvc.perform(request)
				.andExpect(status().isUnauthorized())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(body);
	}

	private record LoginPayload(String email, String password) {
	}
}
