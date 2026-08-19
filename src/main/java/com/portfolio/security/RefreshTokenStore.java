package com.portfolio.security;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis allowlist of the one currently-valid refresh-token {@code jti} per admin.
 *
 * <p>This is what makes refresh <b>rotation</b> enforceable: a refresh token that verifies
 * cryptographically but whose {@code jti} is no longer the stored one has been rotated away (or
 * is a replay of a leaked token) and is rejected. Logout deletes the key, so no refresh token
 * survives it.
 */
@Component
public class RefreshTokenStore {

	private static final String KEY_PREFIX = "auth:refresh:";

	private final StringRedisTemplate redis;

	public RefreshTokenStore(StringRedisTemplate redis) {
		this.redis = redis;
	}

	/** Stores (overwriting any previous value) the jti that is now the only valid one. */
	public void store(Long adminId, String jti, Duration ttl) {
		redis.opsForValue().set(key(adminId), jti, ttl);
	}

	public Optional<String> currentJti(Long adminId) {
		return Optional.ofNullable(redis.opsForValue().get(key(adminId)));
	}

	public boolean isCurrent(Long adminId, String jti) {
		return currentJti(adminId).filter(stored -> stored.equals(jti)).isPresent();
	}

	public void revoke(Long adminId) {
		redis.delete(key(adminId));
	}

	private String key(Long adminId) {
		return KEY_PREFIX + adminId;
	}
}
