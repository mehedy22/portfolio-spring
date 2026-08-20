package com.portfolio.auth.controller;

import com.portfolio.auth.dto.AdminResponse;
import com.portfolio.auth.dto.LoginRequest;
import com.portfolio.auth.dto.PasswordResetConfirmRequest;
import com.portfolio.auth.dto.PasswordResetRequest;
import com.portfolio.auth.dto.TokenResponse;
import com.portfolio.auth.service.AuthService;
import com.portfolio.common.response.ApiResponse;
import com.portfolio.common.web.ClientIp;
import com.portfolio.security.AuthProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoints per docs/07-api/endpoints.md. The refresh token is carried by an
 * httpOnly/Secure/SameSite=Strict cookie scoped to {@code /api/v1/auth} — never in a response body,
 * so JS cannot read it.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Auth", description = "Admin authentication (single-admin platform — D-005)")
public class AuthController {

	static final String REFRESH_COOKIE = "refreshToken";
	private static final String COOKIE_PATH = "/api/v1/auth";

	private final AuthService authService;
	private final AuthProperties authProperties;

	public AuthController(AuthService authService, AuthProperties authProperties) {
		this.authService = authService;
		this.authProperties = authProperties;
	}

	@PostMapping("/auth/login")
	@Operation(summary = "Log in", description = "Returns an access token; sets the refresh cookie.")
	public ResponseEntity<ApiResponse<TokenResponse>> login(
			@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {

		AuthService.AuthTokens tokens = authService.login(request, ClientIp.of(httpRequest));
		return tokenResponse(tokens);
	}

	@PostMapping("/auth/refresh")
	@Operation(summary = "Rotate tokens", description = "Consumes the refresh cookie and issues a new pair.")
	public ResponseEntity<ApiResponse<TokenResponse>> refresh(
			@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {

		AuthService.AuthTokens tokens = authService.refresh(refreshToken);
		return tokenResponse(tokens);
	}

	@PostMapping("/auth/logout")
	@Operation(summary = "Log out", description = "Revokes the refresh token and clears the cookie.")
	public ResponseEntity<ApiResponse<Void>> logout(
			@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {

		authService.logout(refreshToken);
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, clearedRefreshCookie().toString())
				.body(ApiResponse.of(null, "Logged out"));
	}

	@PostMapping("/auth/password-reset/request")
	@Operation(
			summary = "Request a password reset",
			description = "Always 200, whether or not the address is known — this endpoint must not "
					+ "reveal the admin's email.")
	public ApiResponse<Void> requestPasswordReset(
			@Valid @RequestBody PasswordResetRequest request, HttpServletRequest httpRequest) {

		authService.requestPasswordReset(request.email(), ClientIp.of(httpRequest));
		return ApiResponse.of(null, "If that address is registered, a reset link has been sent.");
	}

	@PostMapping("/auth/password-reset/confirm")
	@Operation(summary = "Complete a password reset", description = "Single-use token; ends any live session.")
	public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
			@Valid @RequestBody PasswordResetConfirmRequest request) {

		authService.confirmPasswordReset(request.token(), request.newPassword());
		// Clear the refresh cookie too: the server has revoked it, and leaving a dead cookie in the
		// browser only produces a confusing 401 on the next silent refresh.
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, clearedRefreshCookie().toString())
				.body(ApiResponse.of(null, "Password updated. Please sign in again."));
	}

	/**
	 * The authenticated admin's own profile — backs the Admin Panel topbar, which shows the
	 * logged-in admin's email.
	 */
	@GetMapping("/admin/me")
	@Operation(summary = "Current admin", description = "Profile of the authenticated admin.")
	public ApiResponse<AdminResponse> me(Authentication authentication) {
		Long adminId = (Long) authentication.getPrincipal();
		return ApiResponse.of(authService.currentAdmin(adminId));
	}

	private ResponseEntity<ApiResponse<TokenResponse>> tokenResponse(AuthService.AuthTokens tokens) {
		TokenResponse body = TokenResponse.bearer(tokens.accessToken(), tokens.accessTokenTtlSeconds());
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken()).toString())
				.body(ApiResponse.of(body));
	}

	private ResponseCookie refreshCookie(String token) {
		return baseCookie(token).maxAge(Duration.ofDays(7)).build();
	}

	private ResponseCookie clearedRefreshCookie() {
		return baseCookie("").maxAge(Duration.ZERO).build();
	}

	private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
		return ResponseCookie.from(REFRESH_COOKIE, value)
				.httpOnly(true)
				.secure(authProperties.cookie().secure())
				.sameSite("Strict")
				.path(COOKIE_PATH);
	}

}
