package com.portfolio.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Issues and validates the two token types (docs/08-security/authentication-authorization.md):
 *
 * <ul>
 *   <li><b>access</b> — 15 min, claims {@code sub}/{@code iat}/{@code exp}, returned in the body.
 *   <li><b>refresh</b> — 7 days, additionally carries {@code jti}, which the Redis allowlist
 *       checks so a rotated-away (or replayed) refresh token is rejected.
 * </ul>
 *
 * Both are HS256-signed with a single externalized secret.
 */
@Component
public class JwtTokenProvider {

	private static final String CLAIM_TYPE = "typ";
	private static final String TYPE_ACCESS = "access";
	private static final String TYPE_REFRESH = "refresh";

	private final SecretKey key;
	private final Duration accessTokenTtl;
	private final Duration refreshTokenTtl;

	public JwtTokenProvider(JwtProperties properties) {
		// Before deriving the key: jjwt would otherwise reject an unset secret with a
		// WeakKeyException about "0 bits", which says nothing about what to actually configure.
		JwtSecretValidator.check(properties.secret());
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
		this.accessTokenTtl = properties.accessTokenTtl();
		this.refreshTokenTtl = properties.refreshTokenTtl();
	}

	public String createAccessToken(Long adminId) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(String.valueOf(adminId))
				.claim(CLAIM_TYPE, TYPE_ACCESS)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(accessTokenTtl)))
				.signWith(key)
				.compact();
	}

	/** Refresh tokens carry a unique {@code jti} so rotation can be enforced server-side. */
	public RefreshToken createRefreshToken(Long adminId) {
		Instant now = Instant.now();
		String jti = UUID.randomUUID().toString();
		String token = Jwts.builder()
				.subject(String.valueOf(adminId))
				.id(jti)
				.claim(CLAIM_TYPE, TYPE_REFRESH)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(refreshTokenTtl)))
				.signWith(key)
				.compact();
		return new RefreshToken(token, jti);
	}

	/** Parsed access-token subject, or empty when the token is absent/invalid/expired/wrong-type. */
	public Optional<Long> resolveAccessTokenSubject(String token) {
		return parse(token, TYPE_ACCESS).map(claims -> Long.valueOf(claims.getSubject()));
	}

	/** Parsed refresh token, or empty when invalid/expired/wrong-type. */
	public Optional<ParsedRefreshToken> parseRefreshToken(String token) {
		return parse(token, TYPE_REFRESH)
				.map(claims -> new ParsedRefreshToken(Long.valueOf(claims.getSubject()), claims.getId()));
	}

	public long accessTokenTtlSeconds() {
		return accessTokenTtl.toSeconds();
	}

	public Duration refreshTokenTtl() {
		return refreshTokenTtl;
	}

	private Optional<Claims> parse(String token, String expectedType) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		try {
			Claims claims = Jwts.parser()
					.verifyWith(key)
					.build()
					.parseSignedClaims(token)
					.getPayload();
			if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
				return Optional.empty();
			}
			return Optional.of(claims);
		}
		catch (JwtException | IllegalArgumentException ex) {
			// Malformed, tampered, expired, or wrong-key token — all indistinguishable to callers.
			return Optional.empty();
		}
	}

	/** A freshly minted refresh token plus the {@code jti} to store in the Redis allowlist. */
	public record RefreshToken(String token, String jti) {
	}

	/** The claims a presented refresh token carries. */
	public record ParsedRefreshToken(Long adminId, String jti) {
	}
}
