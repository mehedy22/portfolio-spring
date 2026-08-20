package com.portfolio.notification;

/**
 * Outbound notifications. One seam, because the delivery mechanism is genuinely undecided: the
 * architecture lists an email provider as "TBD" (OPEN_QUESTIONS #13), so the flows that need one
 * are built against this interface and a provider is dropped in behind it later without touching
 * a caller.
 */
public interface Notifier {

	/** A visitor has written in (FR-15). */
	void newContactMessage(String fromName, String fromEmail, String subject);

	/**
	 * Delivers a password-reset link (FR-16).
	 *
	 * <p>The token is a credential: whatever implements this must never log it, put it in a URL
	 * it does not control, or store it anywhere durable.
	 */
	void passwordReset(String toEmail, String token);
}
