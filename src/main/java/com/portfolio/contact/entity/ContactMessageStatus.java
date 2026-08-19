package com.portfolio.contact.entity;

/**
 * Inbox state, mirroring {@code ck_contact_message_status}. Distinct from
 * {@code common.content.ContentStatus}: a message is not published content, and these values
 * describe the admin's handling of it, not its visibility.
 */
public enum ContactMessageStatus {
	NEW,
	READ,
	REPLIED
}
