package com.portfolio.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT signing/lifetime configuration. The secret is externalized (env var) — never hardcoded,
 * per Portfolio.md's "no secrets in code" convention.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
}
