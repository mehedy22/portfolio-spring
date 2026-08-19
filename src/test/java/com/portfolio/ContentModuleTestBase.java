package com.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.auth.AdminBootstrap;
import com.portfolio.auth.entity.Admin;
import com.portfolio.auth.repository.AdminRepository;
import com.portfolio.media.entity.Media;
import com.portfolio.media.entity.StorageBackend;
import com.portfolio.media.repository.MediaRepository;
import com.portfolio.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Shared fixture for the Sprint 4 content modules: a seeded admin, its access token, and the two
 * things every one of these tests needs (an authenticated JSON write, and a media row to
 * reference). Each module's own test class stays about that module's behaviour.
 */
@AutoConfigureMockMvc
public abstract class ContentModuleTestBase extends IntegrationTestBase {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected MediaRepository mediaRepository;

	@Autowired
	private AdminRepository adminRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtTokenProvider tokenProvider;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/** The bootstrap runner must not race with the seeded fixture. */
	@MockitoBean
	private AdminBootstrap adminBootstrap;

	private String accessToken;

	@BeforeEach
	void seedAdminAccount() {
		adminRepository.deleteAll();
		Admin admin = adminRepository.save(
				new Admin("content-admin@yourname.dev", passwordEncoder.encode("irrelevant")));
		accessToken = tokenProvider.createAccessToken(admin.getId());
	}

	/**
	 * Physically empties the named tables. {@code deleteAll()} is not usable here: these entities
	 * are soft-deleted, so it leaves rows behind that still hold FK references (a soft-deleted
	 * skill keeps its RESTRICT reference to a category, which is the FK working as designed).
	 */
	protected void truncate(String... tables) {
		jdbcTemplate.execute("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
	}

	protected String bearer() {
		return "Bearer " + accessToken;
	}

	protected MockHttpServletRequestBuilder adminPost(String path, Object body) throws Exception {
		return post(path)
				.header(HttpHeaders.AUTHORIZATION, bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body));
	}

	protected MockHttpServletRequestBuilder adminPut(String path, Object body) throws Exception {
		return put(path)
				.header(HttpHeaders.AUTHORIZATION, bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body));
	}

	protected Long seedMedia(String fileName) {
		return mediaRepository
				.save(new Media(
						"stored-" + fileName, fileName, "image/png", 100L, StorageBackend.LOCAL,
						"2026/08/stored-" + fileName, 10, 10, null, null))
				.getId();
	}

	protected Long idOf(String responseBody) throws Exception {
		return objectMapper.readTree(responseBody).get("data").get("id").asLong();
	}
}
