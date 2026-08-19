package com.portfolio.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Populates the SecurityContext from a {@code Authorization: Bearer <accessToken>} header.
 *
 * <p>Invalid/expired tokens are simply not authenticated — the request continues unauthenticated
 * and {@link UnauthorizedEntryPoint} produces the 401 envelope if the target route required auth.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider tokenProvider;

	public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
		this.tokenProvider = tokenProvider;
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain)
			throws ServletException, IOException {

		bearerToken(request)
				.flatMap(tokenProvider::resolveAccessTokenSubject)
				.ifPresent(this::authenticate);

		filterChain.doFilter(request, response);
	}

	private void authenticate(Long adminId) {
		var authentication = new UsernamePasswordAuthenticationToken(
				adminId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private java.util.Optional<String> bearerToken(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			return java.util.Optional.empty();
		}
		return java.util.Optional.of(header.substring(BEARER_PREFIX.length()));
	}
}
