package com.portfolio.security;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/**
 * Fails startup loudly when no usable JWT signing secret is configured.
 *
 * <p>This exists because the alternative — shipping a working default — fails <em>silently</em>:
 * a deploy that forgets {@code JWT_SECRET} would run happily while signing admin tokens with a
 * key committed to version control, so anyone who can read the repository could forge them.
 * Phase 8 requires the secret to come from the environment and never be committed
 * (docs/08-security/application-security.md, "Secrets management"), so the main configuration
 * carries no default at all: an unset secret is a crash, not a weak fallback.
 *
 * <p>Local development supplies the throwaway secret via the {@code dev} profile
 * ({@code application-dev.yml}); tests inject their own.
 *
 * <p>The check lives in a {@code static} method rather than only in {@link PostConstruct} because
 * bean creation order is not guaranteed: {@link JwtTokenProvider} is pulled in eagerly while the
 * servlet context registers {@code jwtAuthenticationFilter}, so it can be constructed <em>before</em>
 * this component's {@code @PostConstruct} ever runs. When that happened the operator saw jjwt's
 * {@code WeakKeyException} ("key byte array is 0 bits") instead of the actionable message below.
 * {@code JwtTokenProvider} now calls {@link #check(String)} directly, so the diagnosis no longer
 * depends on which bean Spring happens to instantiate first.
 */
@Component
class JwtSecretValidator {

	/** HS256 needs a key of at least 256 bits. */
	private static final int MIN_SECRET_BYTES = 32;

	private final String secret;

	JwtSecretValidator(JwtProperties properties) {
		this.secret = properties.secret();
	}

	@PostConstruct
	void validate() {
		check(secret);
	}

	/** Throws {@link IllegalStateException} unless {@code secret} can safely sign an HS256 token. */
	static void check(String secret) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException(
					"""
					No JWT signing secret configured. Set the JWT_SECRET environment variable to a \
					random value of at least %d bytes, e.g.

					    export JWT_SECRET="$(openssl rand -base64 48)"

					For local development you can instead run with the dev profile \
					(--spring.profiles.active=dev), which supplies a throwaway secret. \
					There is deliberately no default: a committed fallback secret would let anyone \
					who reads this repository forge admin tokens."""
							.formatted(MIN_SECRET_BYTES));
		}

		int length = secret.getBytes(StandardCharsets.UTF_8).length;
		if (length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"JWT signing secret is too short for HS256: %d bytes, need at least %d. Generate one with: openssl rand -base64 48"
							.formatted(length, MIN_SECRET_BYTES));
		}
	}
}
