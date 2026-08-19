package com.portfolio.contact.dto;

import com.portfolio.contact.entity.ContactMessageStatus;
import java.time.Instant;

/** Admin-only: a visitor's message is never exposed on a public endpoint. */
public record ContactMessageResponse(
		Long id,
		String name,
		String email,
		String subject,
		String message,
		ContactMessageStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
