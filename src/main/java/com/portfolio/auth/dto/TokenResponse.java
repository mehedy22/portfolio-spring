package com.portfolio.auth.dto;

/**
 * Access token payload. The refresh token is deliberately absent — it travels only as an
 * httpOnly/Secure/SameSite=Strict cookie so JS (and any XSS payload) can never read it
 * (docs/08-security/authentication-authorization.md).
 */
public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {

	public static TokenResponse bearer(String accessToken, long expiresInSeconds) {
		return new TokenResponse(accessToken, "Bearer", expiresInSeconds);
	}
}
