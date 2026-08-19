package com.portfolio.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Login brute-force protection and refresh-cookie flags.
 *
 * <p>Since exactly one account exists (D-005), a successful brute-force is total compromise — hence
 * the deliberately tight default of 5 attempts per IP per 15 minutes.
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(LoginRateLimit loginRateLimit, Cookie cookie) {

	public record LoginRateLimit(int maxAttempts, Duration window) {
	}

	/** {@code secure=false} only for local http development; env-overridden to true in prod. */
	public record Cookie(boolean secure) {
	}
}
