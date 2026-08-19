package com.portfolio.contact.dto;

import com.portfolio.contact.entity.ContactMessageStatus;
import jakarta.validation.constraints.NotNull;

/** Body of {@code PATCH /api/v1/admin/contact-messages/{id}/status}. */
public record ContactMessageStatusRequest(@NotNull ContactMessageStatus status) {
}
