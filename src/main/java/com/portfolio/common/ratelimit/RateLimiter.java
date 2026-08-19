package com.portfolio.common.ratelimit;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fixed-window counter in Redis, shared by every throttled endpoint
 * (docs/05-architecture/system-architecture.md §4 — login now, Contact and Analytics in later
 * sprints; no new infrastructure per endpoint).
 *
 * <p>The window starts at the first counted attempt and the key expires with it, so a caller who
 * goes quiet is not penalised indefinitely.
 */
@Component
public class RateLimiter {

	private final StringRedisTemplate redis;

	public RateLimiter(StringRedisTemplate redis) {
		this.redis = redis;
	}

	/**
	 * @return {@code true} when the caller is still within its allowance — checked without
	 *     incrementing, so a successful request never consumes an attempt.
	 */
	public boolean isWithinLimit(String key, int maxAttempts) {
		String current = redis.opsForValue().get(key);
		return current == null || Long.parseLong(current) < maxAttempts;
	}

	/** Counts one attempt, starting the window if this is the first. */
	public void recordAttempt(String key, Duration window) {
		Long count = redis.opsForValue().increment(key);
		if (count != null && count == 1L) {
			redis.expire(key, window);
		}
	}

	/** Clears the counter — e.g. after a successful login. */
	public void reset(String key) {
		redis.delete(key);
	}
}
