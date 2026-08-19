package com.portfolio.auth;

import com.portfolio.auth.entity.Admin;
import com.portfolio.auth.repository.AdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provisions the single admin account from environment variables at startup.
 *
 * <p>Credentials are never committed to a migration or defaulted in code
 * (docs/06-database/migration-strategy.md). Idempotent: if an admin already exists this does
 * nothing — it will not overwrite a password. If no admin exists and the variables are absent it
 * warns and carries on rather than crashing, so the app still boots for a fresh deployment.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final AdminRepository adminRepository;
	private final PasswordEncoder passwordEncoder;
	private final String bootstrapEmail;
	private final String bootstrapPassword;

	public AdminBootstrap(
			AdminRepository adminRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.auth.bootstrap.email:}") String bootstrapEmail,
			@Value("${app.auth.bootstrap.password:}") String bootstrapPassword) {
		this.adminRepository = adminRepository;
		this.passwordEncoder = passwordEncoder;
		this.bootstrapEmail = bootstrapEmail;
		this.bootstrapPassword = bootstrapPassword;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (adminRepository.count() > 0) {
			log.debug("Admin account already present — bootstrap skipped");
			return;
		}

		if (bootstrapEmail.isBlank() || bootstrapPassword.isBlank()) {
			log.warn(
					"No admin account exists and no bootstrap credentials were provided. "
							+ "Set ADMIN_EMAIL and ADMIN_PASSWORD environment variables and restart to "
							+ "provision the single admin account (D-005). The API will start, but no one "
							+ "can log in until this is done.");
			return;
		}

		Admin admin = new Admin(bootstrapEmail.trim(), passwordEncoder.encode(bootstrapPassword));
		adminRepository.save(admin);
		// The password is never logged.
		log.info("Bootstrapped the admin account for email={}", admin.getEmail());
	}
}
