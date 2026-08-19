package com.portfolio.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.IntegrationTestBase;
import com.portfolio.auth.AdminBootstrap;
import com.portfolio.auth.entity.Admin;
import com.portfolio.auth.repository.AdminRepository;
import com.portfolio.media.repository.MediaRepository;
import com.portfolio.security.JwtTokenProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * Sprint 2 media acceptance: happy path, the two NFR-06 rejections (type by content, size),
 * authorization, not-found, and the security properties that are easy to regress — a disguised
 * file is refused, the storage name is generated rather than user-supplied, and deleting a file
 * really does stop serving its bytes.
 */
@AutoConfigureMockMvc
class MediaIntegrationTest extends IntegrationTestBase {

	private static final Path STORAGE_ROOT = createTempStorageRoot();

	@Autowired
	private MockMvc mockMvc;

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

	/** The bootstrap runner must not race with the seeded fixture. */
	@MockitoBean
	private AdminBootstrap adminBootstrap;

	private String accessToken;

	@DynamicPropertySource
	static void mediaProperties(DynamicPropertyRegistry registry) {
		registry.add("app.media.storage-root", STORAGE_ROOT::toString);
	}

	@BeforeEach
	void seedAdmin() {
		purgeStorageRoot();
		mediaRepository.deleteAll();
		adminRepository.deleteAll();
		Admin admin = adminRepository.save(new Admin("media-admin@yourname.dev", passwordEncoder.encode("irrelevant")));
		accessToken = tokenProvider.createAccessToken(admin.getId());
	}

	// --------------------------------------------------------------- upload

	@Test
	@DisplayName("uploading a valid image returns 201 with its metadata and a public content URL")
	void uploadImageSucceeds() throws Exception {
		mockMvc.perform(upload(MediaTestFiles.png(), "diagram.png", "image/png").param("altText", "  A diagram  "))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.mimeType").value("image/png"))
				.andExpect(jsonPath("$.data.originalFileName").value("diagram.png"))
				.andExpect(jsonPath("$.data.storageBackend").value("LOCAL"))
				.andExpect(jsonPath("$.data.width").value(MediaTestFiles.PNG_WIDTH))
				.andExpect(jsonPath("$.data.height").value(MediaTestFiles.PNG_HEIGHT))
				.andExpect(jsonPath("$.data.altText").value("A diagram"))
				.andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.endsWith("/content")));
	}

	@Test
	@DisplayName("a PDF is accepted and its dimensions stay null")
	void uploadPdfSucceeds() throws Exception {
		mockMvc.perform(upload(MediaTestFiles.pdf(), "resume.pdf", "application/pdf"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.mimeType").value("application/pdf"))
				.andExpect(jsonPath("$.data.width").doesNotExist())
				.andExpect(jsonPath("$.data.height").doesNotExist());
	}

	@Test
	@DisplayName("the stored file is named by the server, never by the client")
	void storedNameIsGenerated() throws Exception {
		String body = mockMvc.perform(upload(MediaTestFiles.png(), "../../escape.png", "image/png"))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		JsonNode data = objectMapper.readTree(body).get("data");

		assertThat(data.get("fileName").asText())
				.as("generated storage name")
				.doesNotContain("escape")
				.doesNotContain("..")
				.endsWith(".png");
		assertThat(data.get("originalFileName").asText())
				.as("display name keeps no directory component")
				.isEqualTo("escape.png");
		assertThat(storedFiles())
				.as("nothing was written outside the configured storage root")
				.allSatisfy(path -> assertThat(path.normalize()).startsWith(STORAGE_ROOT));
	}

	// ------------------------------------------------------- upload rejected

	@Test
	@DisplayName("a script renamed .png with an image Content-Type is rejected on its content")
	void rejectsDisguisedFile() throws Exception {
		byte[] script = "<?php system($_GET['c']); ?>".getBytes(StandardCharsets.UTF_8);

		mockMvc.perform(upload(script, "innocent.png", "image/png"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		assertThat(mediaRepository.count()).as("nothing persisted").isZero();
		assertThat(storedFiles()).as("nothing written to disk").isEmpty();
	}

	@Test
	@DisplayName("an image over the configured limit is rejected with the documented error code")
	void rejectsOversizedImage() throws Exception {
		byte[] tooBig = MediaTestFiles.oversizedPng(6 * 1024 * 1024);

		mockMvc.perform(upload(tooBig, "huge.png", "image/png"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("limit")));

		assertThat(storedFiles()).as("rejected before anything is written").isEmpty();
	}

	@Test
	@DisplayName("an empty file part is rejected")
	void rejectsEmptyFile() throws Exception {
		mockMvc.perform(upload(new byte[0], "empty.png", "image/png"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	// -------------------------------------------------------- authorization

	@Test
	@DisplayName("upload and list require a valid token")
	void adminEndpointsRequireAuth() throws Exception {
		mockMvc.perform(multipart("/api/v1/admin/media")
						.file(new MockMultipartFile("file", "a.png", "image/png", MediaTestFiles.png())))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		mockMvc.perform(get("/api/v1/admin/media")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("serving media content stays public — the site renders images to anonymous visitors")
	void contentEndpointIsPublic() throws Exception {
		long id = uploadPng();

		mockMvc.perform(get("/api/v1/media/{id}/content", id))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"));
	}

	// ----------------------------------------------------------- list/serve

	@Test
	@DisplayName("the library lists uploads newest first, in the documented page envelope")
	void listReturnsPagedLibrary() throws Exception {
		uploadPng();
		uploadPng();

		mockMvc.perform(get("/api/v1/admin/media").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content.length()").value(2))
				.andExpect(jsonPath("$.data.page").value(0))
				.andExpect(jsonPath("$.data.size").value(20))
				.andExpect(jsonPath("$.data.totalElements").value(2));
	}

	@Test
	@DisplayName("an uploaded file is retrievable byte-for-byte")
	void uploadedFileIsRetrievable() throws Exception {
		byte[] original = MediaTestFiles.png();
		long id = uploadPng();

		byte[] served = mockMvc.perform(get("/api/v1/media/{id}/content", id))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsByteArray();

		assertThat(served).isEqualTo(original);
	}

	@Test
	@DisplayName("unknown media is a 404 on both the public and admin surfaces")
	void unknownMediaIsNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/media/{id}/content", 999_999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));

		mockMvc.perform(delete("/api/v1/admin/media/{id}", 999_999L).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNotFound());
	}

	// --------------------------------------------------------------- delete

	@Test
	@DisplayName("deleting media stops serving it, drops it from the library, and removes the bytes")
	void deleteRemovesRowAndFile() throws Exception {
		long id = uploadPng();
		assertThat(storedFiles()).hasSize(1);

		mockMvc.perform(delete("/api/v1/admin/media/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/media/{id}/content", id)).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/admin/media").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(jsonPath("$.data.totalElements").value(0));
		assertThat(storedFiles()).as("the bytes are gone, not just hidden").isEmpty();
	}

	// ---------------------------------------------------------------- utils

	private long uploadPng() throws Exception {
		String body = mockMvc.perform(upload(MediaTestFiles.png(), "image.png", "image/png"))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(body).get("data").get("id").asLong();
	}

	private MockMultipartHttpServletRequestBuilder upload(byte[] content, String fileName, String declaredType) {
		MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/admin/media");
		request.file(new MockMultipartFile("file", fileName, declaredType, content));
		request.header(HttpHeaders.AUTHORIZATION, bearer());
		return request;
	}

	private String bearer() {
		return "Bearer " + accessToken;
	}

	/** Files outlive a soft-deleted row, so each test starts from an empty storage root. */
	private void purgeStorageRoot() {
		storedFiles().forEach(path -> {
			try {
				Files.delete(path);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		});
	}

	/** Every regular file currently under the storage root. */
	private List<Path> storedFiles() {
		try (Stream<Path> paths = Files.walk(STORAGE_ROOT)) {
			return paths.filter(Files::isRegularFile).toList();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static Path createTempStorageRoot() {
		try {
			return Files.createTempDirectory("portfolio-media-test").toAbsolutePath().normalize();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
}
