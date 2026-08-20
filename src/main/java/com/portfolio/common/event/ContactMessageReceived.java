package com.portfolio.common.event;

/**
 * Published in-process when a visitor's message is stored (D-007 — application events, no broker).
 *
 * <p>Carries only what a notification needs. The message body is deliberately absent: the admin
 * reads it in the inbox, and copying a stranger's text into an outbound channel widens where it
 * lives for no benefit.
 */
public record ContactMessageReceived(Long messageId, String name, String email, String subject) {
}
