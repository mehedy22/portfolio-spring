package com.portfolio.config;

import com.portfolio.security.AuthProperties;
import com.portfolio.security.JwtAuthenticationFilter;
import com.portfolio.security.JwtProperties;
import com.portfolio.security.UnauthorizedEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Authorization is deliberately trivial (D-002/D-005): the only question is "is this the Admin?".
 *
 * <ul>
 *   <li>{@code /api/v1/admin/**} — requires a valid access token.
 *   <li>everything else — public. Visitors are always anonymous; the public content endpoints
 *       arriving in later sprints must never require auth.
 * </ul>
 *
 * <p>Stateless (no session), CSRF disabled — a bearer-token API with the refresh cookie pinned to
 * {@code SameSite=Strict} on a POST-only endpoint, per the conscious trade-off recorded in
 * docs/08-security/authentication-authorization.md.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, AuthProperties.class})
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final UnauthorizedEntryPoint unauthorizedEntryPoint;

	public SecurityConfig(
			JwtAuthenticationFilter jwtAuthenticationFilter, UnauthorizedEntryPoint unauthorizedEntryPoint) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.unauthorizedEntryPoint = unauthorizedEntryPoint;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http
				// Cookie-free, bearer-token API: nothing to protect with a CSRF token, and the
				// refresh cookie is SameSite=Strict on a POST-only endpoint.
				.csrf(csrf -> csrf.disable())
				// Delegates to the MVC CORS configuration in CorsConfig — origins stay in one place.
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/v1/admin/**").authenticated()
						.anyRequest().permitAll())
				.exceptionHandling(handling -> handling.authenticationEntryPoint(unauthorizedEntryPoint))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	/** BCrypt strength 12, per docs/08-security/authentication-authorization.md. */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}
}
