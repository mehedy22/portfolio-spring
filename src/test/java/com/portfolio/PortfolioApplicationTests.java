package com.portfolio;

import org.junit.jupiter.api.Test;

/**
 * Smoke test: verifies the Spring context loads against real PostgreSQL + Redis instances,
 * matching production wiring (ddl-auto=validate, Flyway, Redis auto-config, Spring Security).
 */
class PortfolioApplicationTests extends IntegrationTestBase {

	@Test
	void contextLoads() {
	}

}
