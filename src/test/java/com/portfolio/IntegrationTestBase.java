package com.portfolio;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared base for integration tests: one PostgreSQL and one Redis container per test JVM, reused
 * across every test class (singleton-container pattern, not one container per test — per
 * docs/11-technical-design/backend-design.md).
 *
 * <p>The containers are started manually in a static initialiser rather than via
 * {@code @Testcontainers}/{@code @Container}, because that annotation pair would stop them after
 * each class. Ryuk reaps them when the JVM exits.
 */
@SpringBootTest
public abstract class IntegrationTestBase {

	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
			.withDatabaseName("portfolio")
			.withUsername("portfolio")
			.withPassword("portfolio");

	static final RedisContainer REDIS = new RedisContainer(DockerImageName.parse("redis:7"));

	static {
		POSTGRES.start();
		REDIS.start();
	}

	/** Test-only signing secret. The application has no default and refuses to start without one. */
	private static final String TEST_JWT_SECRET = "test-only-jwt-signing-secret-at-least-32-bytes-long";

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
		registry.add("app.jwt.secret", () -> TEST_JWT_SECRET);
	}
}
