package com.portfolio.analytics;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Throttling for the public page-view endpoint. The allowance is deliberately generous —
 * legitimate browsing generates many views, and the limit exists to blunt flooding, not to police
 * readers (docs/08-security/application-security.md's rate-limit table).
 */
@ConfigurationProperties(prefix = "app.analytics")
public record AnalyticsProperties(RateLimit rateLimit) {

	public record RateLimit(int maxAttempts, Duration window) {
	}
}
