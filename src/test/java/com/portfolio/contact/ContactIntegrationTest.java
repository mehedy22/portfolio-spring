package com.portfolio.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.ContentModuleTestBase;
import com.portfolio.contact.entity.ContactMessageStatus;
import com.portfolio.contact.repository.ContactMessageRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Sprint 5 acceptance. Both DoD properties get direct tests: a honeypot submission is rejected
 * <em>silently</em> (the bot sees the same 201 a human sees, and nothing is stored), and a
 * legitimate submission lands in the inbox as NEW.
 *
 * <p>Each test uses a distinct client IP so the per-IP allowance does not bleed between tests.
 */
class ContactIntegrationTest extends ContentModuleTestBase {

	@Autowired
	private ContactMessageRepository contactMessageRepository;

	@Autowired
	private StringRedisTemplate redis;

	@BeforeEach
	void clear() {
		truncate("contact_message");
		Objects.requireNonNull(redis.getConnectionFactory()).getConnection().serverCommands().flushAll();
	}

	// --------------------------------------------------------------- submit

	@Test
	@DisplayName("a legitimate submission is accepted anonymously and lands in the inbox as NEW")
	void submissionReachesTheInbox() throws Exception {
		mockMvc.perform(submit(message(), "10.1.0.1"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Message received"))
				// Nothing about the stored row comes back to an anonymous caller.
				.andExpect(jsonPath("$.data").doesNotExist());

		mockMvc.perform(get("/api/v1/admin/contact-messages").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].status").value("NEW"))
				.andExpect(jsonPath("$.data.content[0].email").value("visitor@example.com"))
				.andExpect(jsonPath("$.data.content[0].message").value("Hello, I would like to talk."));
	}

	@Test
	@DisplayName("a filled honeypot is rejected silently — same 201, nothing stored")
	void honeypotSubmissionIsSilentlyDropped() throws Exception {
		Map<String, Object> bot = message();
		bot.put("website", "http://spam.example.com");

		mockMvc.perform(submit(bot, "10.1.0.2"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Message received"));

		assertThat(contactMessageRepository.count()).as("spam never reaches the inbox").isZero();
	}

	@Test
	@DisplayName("the honeypot response is byte-identical to a real submission's")
	void honeypotIsIndistinguishable() throws Exception {
		String human = body(mockMvc.perform(submit(message(), "10.1.0.3")));
		Map<String, Object> bot = message();
		bot.put("website", "spam");
		String robot = body(mockMvc.perform(submit(bot, "10.1.0.4")));

		// The timestamp differs by design; everything a bot could probe must not.
		assertThat(stripTimestamp(robot))
				.as("a bot cannot tell whether its message was delivered")
				.isEqualTo(stripTimestamp(human));
	}

	@Test
	@DisplayName("the sixth submission from one IP within the window is refused with 429")
	void rateLimitIsEnforcedPerIp() throws Exception {
		for (int i = 0; i < 5; i++) {
			mockMvc.perform(submit(message(), "10.2.0.1")).andExpect(status().isCreated());
		}
		mockMvc.perform(submit(message(), "10.2.0.1"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("RATE_LIMITED"));

		// A different visitor is unaffected.
		mockMvc.perform(submit(message(), "10.2.0.2")).andExpect(status().isCreated());
		assertThat(contactMessageRepository.count()).isEqualTo(6);
	}

	@Test
	@DisplayName("spam consumes the allowance, so a bot cannot flood for free")
	void honeypotSubmissionsCountTowardsTheLimit() throws Exception {
		Map<String, Object> bot = message();
		bot.put("website", "spam");
		for (int i = 0; i < 5; i++) {
			mockMvc.perform(submit(bot, "10.3.0.1")).andExpect(status().isCreated());
		}

		mockMvc.perform(submit(message(), "10.3.0.1")).andExpect(status().isTooManyRequests());
		assertThat(contactMessageRepository.count()).isZero();
	}

	@Test
	@DisplayName("validation rejects a missing name and a malformed email address")
	void validation() throws Exception {
		Map<String, Object> invalid = new LinkedHashMap<>();
		invalid.put("email", "not-an-email");
		invalid.put("message", "hi");

		mockMvc.perform(submit(invalid, "10.4.0.1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors.length()").value(2));
	}

	// ---------------------------------------------------------------- inbox

	@Test
	@DisplayName("the inbox is admin-only and never exposed publicly")
	void inboxRequiresAuth() throws Exception {
		mockMvc.perform(submit(message(), "10.5.0.1")).andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/admin/contact-messages")).andExpect(status().isUnauthorized());
		mockMvc.perform(patch("/api/v1/admin/contact-messages/1/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"READ\"}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/v1/admin/contact-messages/1")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("status moves NEW -> READ -> REPLIED, and back again when the admin wants")
	void statusTransitions() throws Exception {
		mockMvc.perform(submit(message(), "10.6.0.1")).andExpect(status().isCreated());
		Long id = contactMessageRepository.findAll().get(0).getId();

		setStatus(id, "READ").andExpect(jsonPath("$.data.status").value("READ"));
		setStatus(id, "REPLIED").andExpect(jsonPath("$.data.status").value("REPLIED"));
		setStatus(id, "NEW").andExpect(jsonPath("$.data.status").value("NEW"));

		assertThat(contactMessageRepository.findById(id))
				.get()
				.extracting(m -> m.getStatus())
				.isEqualTo(ContactMessageStatus.NEW);
	}

	@Test
	@DisplayName("the inbox filters by status and lists newest first")
	void inboxFilteringAndOrdering() throws Exception {
		mockMvc.perform(submit(named("First"), "10.7.0.1")).andExpect(status().isCreated());
		mockMvc.perform(submit(named("Second"), "10.7.0.2")).andExpect(status().isCreated());
		Long first = contactMessageRepository.findAll().stream()
				.filter(m -> m.getName().equals("First"))
				.findFirst()
				.orElseThrow()
				.getId();
		setStatus(first, "REPLIED");

		mockMvc.perform(get("/api/v1/admin/contact-messages")
						.param("status", "NEW")
						.header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].name").value("Second"));

		mockMvc.perform(get("/api/v1/admin/contact-messages").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.content[0].name").value("Second"));
	}

	@Test
	@DisplayName("delete hides a message; unknown ids are 404")
	void deleteAndNotFound() throws Exception {
		mockMvc.perform(submit(message(), "10.8.0.1")).andExpect(status().isCreated());
		Long id = contactMessageRepository.findAll().get(0).getId();

		mockMvc.perform(delete("/api/v1/admin/contact-messages/{id}", id)
						.header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/admin/contact-messages").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(jsonPath("$.data.totalElements").value(0));

		mockMvc.perform(delete("/api/v1/admin/contact-messages/{id}", 999_999L)
						.header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNotFound());
		mockMvc.perform(patch("/api/v1/admin/contact-messages/{id}/status", 999_999L)
						.header(HttpHeaders.AUTHORIZATION, bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"READ\"}"))
				.andExpect(status().isNotFound());
	}

	// ---------------------------------------------------------------- utils

	private Map<String, Object> message() {
		return named("A Visitor");
	}

	private Map<String, Object> named(String name) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("name", name);
		body.put("email", "visitor@example.com");
		body.put("subject", "Hello");
		body.put("message", "Hello, I would like to talk.");
		return body;
	}

	private MockHttpServletRequestBuilder submit(Map<String, Object> body, String clientIp) throws Exception {
		return post("/api/v1/contact")
				.header("X-Forwarded-For", clientIp)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body));
	}

	private org.springframework.test.web.servlet.ResultActions setStatus(Long id, String status) throws Exception {
		return mockMvc.perform(patch("/api/v1/admin/contact-messages/{id}/status", id)
						.header(HttpHeaders.AUTHORIZATION, bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"" + status + "\"}"))
				.andExpect(status().isOk());
	}

	private String body(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
		return actions.andReturn().getResponse().getContentAsString();
	}

	private String stripTimestamp(String json) {
		return json.replaceAll("\"timestamp\":\"[^\"]*\"", "\"timestamp\":\"<any>\"");
	}
}
