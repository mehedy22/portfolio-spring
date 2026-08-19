package com.portfolio.common.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The caller's address as the rate limiters should see it.
 *
 * <p>The application sits behind a reverse proxy (docs/05-architecture/deployment-view.md), so
 * {@code getRemoteAddr()} alone would report the proxy and throttle every visitor as one client.
 * {@code X-Forwarded-For} is a client-settable header and therefore spoofable — acceptable here
 * because the trust boundary is the proxy, which overwrites it, and because the consequence of a
 * forged value is only that an attacker throttles themselves under a different key.
 */
public final class ClientIp {

	private static final String FORWARDED_FOR = "X-Forwarded-For";

	private ClientIp() {
	}

	public static String of(HttpServletRequest request) {
		String forwarded = request.getHeader(FORWARDED_FOR);
		if (forwarded != null && !forwarded.isBlank()) {
			// Left-most entry is the original client; the rest are proxies it passed through.
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
