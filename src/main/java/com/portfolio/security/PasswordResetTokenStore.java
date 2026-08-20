package com.portfolio.security;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Password-reset tokens, held in Redis with a TTL rather than in a table
 * (docs/05-architecture/system-architecture.md §4: short-lived, non-critical values do not need a
 * durable home, and one that expires on its own cannot be forgotten about).
 *
 * <p>Tokens are single-use: {@link #consume(String)} deletes as it reads, so a link that has
 * already been used — or that leaked from a mailbox afterwards — is worthless.
 */
@Component
public class PasswordResetTokenStore {

	private static final String KEY_PREFIX = "auth:reset:";
	private static final int TOKEN_BYTES = 32;

	private final StringRedisTemplate redis;
	private final SecureRandom random = new SecureRandom();

	public PasswordResetTokenStore(StringRedisTemplate redis) {
		this.redis = redis;
	}

	/** Issues a token for {@code adminId}, valid for {@code ttl}. */
	public String issue(Long adminId, Duration ttl) {
		byte[] bytes = new byte[TOKEN_BYTES];
		random.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		redis.opsForValue().set(KEY_PREFIX + token, String.valueOf(adminId), ttl);
		return token;
	}

	/** The admin the token belongs to, consuming it. Empty when unknown, expired or already used. */
	public Optional<Long> consume(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		String key = KEY_PREFIX + token;
		String adminId = redis.opsForValue().getAndDelete(key);
		return Optional.ofNullable(adminId).map(Long::valueOf);
	}
}
