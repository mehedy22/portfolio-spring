package com.portfolio.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * The placeholder delivery mechanism: it writes to the log instead of sending anything.
 *
 * <p>This exists so the password-reset and contact-notification <em>flows</em> are complete and
 * testable while the provider question is open. It is deliberately not silent — an operator
 * reading the log can see that a notification was due and was not delivered, which is the honest
 * state of affairs rather than a flow that appears to work.
 *
 * <p>{@code @ConditionalOnMissingBean} means adding a real provider is exactly one new bean.
 */
@Component
@ConditionalOnMissingBean(name = "productionNotifier")
public class LoggingNotifier implements Notifier {

	private static final Logger log = LoggerFactory.getLogger(LoggingNotifier.class);

	@Override
	public void newContactMessage(String fromName, String fromEmail, String subject) {
		log.info(
				"NOTIFICATION (not delivered — no email provider configured): new contact message "
						+ "from {} <{}> subject='{}'",
				fromName,
				fromEmail,
				subject);
	}

	@Override
	public void passwordReset(String toEmail, String token) {
		// The token is never logged: it is a credential, and a log file is not a mailbox.
		log.warn(
				"NOTIFICATION (not delivered — no email provider configured): password reset requested "
						+ "for {}. The token was generated but cannot be sent; configure a provider "
						+ "(OPEN_QUESTIONS #13) before relying on self-service reset.",
				toEmail);
	}
}
