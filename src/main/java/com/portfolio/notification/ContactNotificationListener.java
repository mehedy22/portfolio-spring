package com.portfolio.notification;

import com.portfolio.common.event.ContactMessageReceived;
import com.portfolio.settings.SettingKey;
import com.portfolio.settings.repository.SiteSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Notifies the admin that a message arrived (FR-15).
 *
 * <p>Asynchronous and failure-swallowing on purpose: a visitor's submission has already been
 * stored by the time this runs, and a notification problem must never turn their successful
 * submission into an error they cannot act on.
 *
 * <p>The destination is the {@code contact.notification_email} setting — the deliberately private
 * key added in Sprint 6, which is why it never appears on the public settings endpoint.
 */
@Component
public class ContactNotificationListener {

	private static final Logger log = LoggerFactory.getLogger(ContactNotificationListener.class);

	private final Notifier notifier;
	private final SiteSettingRepository settingRepository;

	public ContactNotificationListener(Notifier notifier, SiteSettingRepository settingRepository) {
		this.notifier = notifier;
		this.settingRepository = settingRepository;
	}

	@Async
	@EventListener
	public void onContactMessage(ContactMessageReceived event) {
		try {
			String destination = settingRepository
					.findById(SettingKey.CONTACT_NOTIFICATION_EMAIL.key())
					.map(setting -> setting.getValue())
					.filter(value -> value != null && !value.isBlank())
					.orElse(null);

			if (destination == null) {
				log.debug("No contact.notification_email configured; skipping notification");
				return;
			}
			notifier.newContactMessage(event.name(), event.email(), event.subject());
		}
		catch (RuntimeException ex) {
			log.error("Could not notify about contact message {}", event.messageId(), ex);
		}
	}
}
