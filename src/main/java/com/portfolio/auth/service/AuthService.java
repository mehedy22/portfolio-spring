package com.portfolio.auth.service;

import com.portfolio.auth.dto.AdminResponse;
import com.portfolio.auth.dto.LoginRequest;

/**
 * Authentication for the single admin account (D-005). There is no registration, no visitor
 * account, and no permission matrix.
 */
public interface AuthService {

	/**
	 * Verifies credentials and issues a fresh token pair.
	 *
	 * @param clientIp source IP, used for the brute-force counter and the WARN audit line
	 * @throws com.portfolio.common.exception.RateLimitExceededException when the IP is over its
	 *     login allowance
	 * @throws com.portfolio.common.exception.UnauthorizedException on bad credentials — identical
	 *     for unknown-email and wrong-password (no account enumeration)
	 */
	AuthTokens login(LoginRequest request, String clientIp);

	/**
	 * Rotates a refresh token: the presented token must be the one currently in the Redis
	 * allowlist, otherwise it has been rotated away, revoked by logout, or replayed.
	 *
	 * @throws com.portfolio.common.exception.UnauthorizedException when the token is
	 *     invalid/expired/superseded
	 */
	AuthTokens refresh(String refreshToken);

	/** Revokes the admin's refresh token. Safe to call with a token that is already invalid. */
	void logout(String refreshToken);

	/** The authenticated admin's own profile. */
	AdminResponse currentAdmin(Long adminId);

	/**
	 * Starts a self-service password reset (FR-16).
	 *
	 * <p>Returns nothing and never signals whether the address is known: the caller is anonymous,
	 * and the endpoint must not become a way to discover the admin's email.
	 */
	void requestPasswordReset(String email, String clientIp);

	/**
	 * Completes a reset with a single-use token.
	 *
	 * @throws com.portfolio.common.exception.UnauthorizedException when the token is unknown,
	 *     expired, or already used
	 */
	void confirmPasswordReset(String token, String newPassword);

	/** A freshly issued token pair — the refresh token is set as a cookie by the controller. */
	record AuthTokens(String accessToken, long accessTokenTtlSeconds, String refreshToken) {
	}
}
