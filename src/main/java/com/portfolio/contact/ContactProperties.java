package com.portfolio.contact;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spam protection for the public contact form (NFR-07,
 * docs/08-security/application-security.md's rate-limit table). Externalized so the limit can be
 * tightened after a spam wave without a release.
 *
 * <p>The honeypot field name is deliberately <em>not</em> configurable: it is part of the request
 * contract the generated client and the public form are built against, so it belongs in the DTO
 * and the OpenAPI schema rather than in a property nothing could change safely at runtime.
 *
 * @param rateLimit per-IP submission allowance
 */
@ConfigurationProperties(prefix = "app.contact")
public record ContactProperties(RateLimit rateLimit) {

	public record RateLimit(int maxAttempts, Duration window) {
	}
}
