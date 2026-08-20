package com.portfolio.auth.service.impl;

import com.portfolio.auth.dto.AdminResponse;
import com.portfolio.auth.dto.LoginRequest;
import com.portfolio.auth.entity.Admin;
import com.portfolio.auth.repository.AdminRepository;
import com.portfolio.auth.service.AuthService;
import com.portfolio.common.exception.RateLimitExceededException;
import com.portfolio.common.exception.UnauthorizedException;
import com.portfolio.common.ratelimit.RateLimiter;
import com.portfolio.security.AuthProperties;
import com.portfolio.security.JwtTokenProvider;
import com.portfolio.notification.Notifier;
import com.portfolio.security.PasswordResetTokenStore;
import com.portfolio.security.RefreshTokenStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

	/** Identical for every credential failure — no account enumeration. */
	private static final String INVALID_CREDENTIALS = "Invalid email or password";

	private static final String RATE_LIMIT_KEY_PREFIX = "auth:login:attempts:";
	private static final String RESET_RATE_LIMIT_KEY_PREFIX = "auth:reset:attempts:";

	/** Long enough to find the mail, short enough that a leaked link goes stale quickly. */
	private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);

	private final AdminRepository adminRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider tokenProvider;
	private final RefreshTokenStore refreshTokenStore;
	private final RateLimiter rateLimiter;
	private final AuthProperties authProperties;
	private final PasswordResetTokenStore passwordResetTokenStore;
	private final Notifier notifier;

	public AuthServiceImpl(
			AdminRepository adminRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenProvider tokenProvider,
			RefreshTokenStore refreshTokenStore,
			RateLimiter rateLimiter,
			AuthProperties authProperties,
			PasswordResetTokenStore passwordResetTokenStore,
			Notifier notifier) {
		this.adminRepository = adminRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenProvider = tokenProvider;
		this.refreshTokenStore = refreshTokenStore;
		this.rateLimiter = rateLimiter;
		this.authProperties = authProperties;
		this.passwordResetTokenStore = passwordResetTokenStore;
		this.notifier = notifier;
	}

	@Override
	@Transactional
	public AuthTokens login(LoginRequest request, String clientIp) {
		String rateLimitKey = RATE_LIMIT_KEY_PREFIX + clientIp;
		int maxAttempts = authProperties.loginRateLimit().maxAttempts();

		// Checked before any DB work so a throttled request never touches Postgres.
		if (!rateLimiter.isWithinLimit(rateLimitKey, maxAttempts)) {
			log.warn("Login rate limit exceeded for ip={}", clientIp);
			throw new RateLimitExceededException("Too many login attempts. Try again later.");
		}

		Optional<Admin> admin = adminRepository.findByEmail(request.email());
		if (admin.isEmpty() || !passwordEncoder.matches(request.password(), admin.get().getPasswordHash())) {
			rateLimiter.recordAttempt(rateLimitKey, authProperties.loginRateLimit().window());
			log.warn("Failed login attempt for email={} from ip={}", request.email(), clientIp);
			throw new UnauthorizedException(INVALID_CREDENTIALS);
		}

		rateLimiter.reset(rateLimitKey);

		Admin authenticated = admin.get();
		authenticated.setLastLoginAt(Instant.now());
		adminRepository.save(authenticated);

		log.info("Successful login for adminId={} from ip={}", authenticated.getId(), clientIp);
		return issueTokens(authenticated.getId());
	}

	@Override
	@Transactional(readOnly = true)
	public AuthTokens refresh(String refreshToken) {
		var parsed = tokenProvider
				.parseRefreshToken(refreshToken)
				.orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

		// Rotation check: only the jti currently in the allowlist is accepted. A token that was
		// already rotated, or revoked by logout, fails here even though it verifies cryptographically.
		if (!refreshTokenStore.isCurrent(parsed.adminId(), parsed.jti())) {
			log.warn("Refresh token replay or reuse after rotation for adminId={}", parsed.adminId());
			throw new UnauthorizedException("Invalid or expired refresh token");
		}

		if (!adminRepository.existsById(parsed.adminId())) {
			throw new UnauthorizedException("Invalid or expired refresh token");
		}

		return issueTokens(parsed.adminId());
	}

	@Override
	@Transactional
	public void logout(String refreshToken) {
		// Best-effort: an already-invalid token simply has nothing to revoke.
		tokenProvider
				.parseRefreshToken(refreshToken)
				.ifPresent(parsed -> refreshTokenStore.revoke(parsed.adminId()));
	}

	@Override
	@Transactional(readOnly = true)
	public AdminResponse currentAdmin(Long adminId) {
		Admin admin = adminRepository
				.findById(adminId)
				.orElseThrow(() -> new UnauthorizedException("Authentication required"));
		return new AdminResponse(admin.getId(), admin.getEmail(), admin.getLastLoginAt());
	}

	/** Issues a new pair and makes the new refresh jti the only valid one. */
	@Override
	@Transactional(readOnly = true)
	public void requestPasswordReset(String email, String clientIp) {
		// Throttled on the same terms as login: without it this endpoint is an unmetered way to
		// probe addresses and to generate mail on someone else's behalf.
		String rateLimitKey = RESET_RATE_LIMIT_KEY_PREFIX + clientIp;
		if (!rateLimiter.isWithinLimit(rateLimitKey, authProperties.loginRateLimit().maxAttempts())) {
			log.warn("Password-reset rate limit exceeded for ip={}", clientIp);
			throw new RateLimitExceededException("Too many reset requests. Try again later.");
		}
		rateLimiter.recordAttempt(rateLimitKey, authProperties.loginRateLimit().window());

		adminRepository
				.findByEmail(email)
				.ifPresentOrElse(
						admin -> {
							String token = passwordResetTokenStore.issue(admin.getId(), RESET_TOKEN_TTL);
							notifier.passwordReset(admin.getEmail(), token);
							log.info("Password reset requested for adminId={}", admin.getId());
						},
						// Deliberately silent: the caller gets the same answer either way, so this
						// endpoint cannot be used to learn the admin's address.
						() -> log.info("Password reset requested for an unknown address from ip={}", clientIp));
	}

	@Override
	@Transactional
	public void confirmPasswordReset(String token, String newPassword) {
		Long adminId = passwordResetTokenStore
				.consume(token)
				.orElseThrow(() -> new UnauthorizedException("This reset link is invalid or has expired"));

		Admin admin = adminRepository
				.findById(adminId)
				.orElseThrow(() -> new UnauthorizedException("This reset link is invalid or has expired"));

		admin.setPasswordHash(passwordEncoder.encode(newPassword));
		adminRepository.save(admin);

		// Whoever prompted the reset may have had a live session; revoking the refresh token means
		// changing the password actually ends it rather than only changing what logs in next time.
		refreshTokenStore.revoke(adminId);
		log.info("Password reset completed for adminId={}", adminId);
	}

	private AuthTokens issueTokens(Long adminId) {
		String accessToken = tokenProvider.createAccessToken(adminId);
		JwtTokenProvider.RefreshToken refresh = tokenProvider.createRefreshToken(adminId);
		refreshTokenStore.store(adminId, refresh.jti(), tokenProvider.refreshTokenTtl());
		return new AuthTokens(accessToken, tokenProvider.accessTokenTtlSeconds(), refresh.token());
	}
}
