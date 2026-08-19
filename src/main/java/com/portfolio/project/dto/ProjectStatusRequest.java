package com.portfolio.project.dto;

import com.portfolio.common.content.ContentStatus;
import jakarta.validation.constraints.NotNull;

/** Body of {@code PATCH /api/v1/admin/projects/{id}/status}. */
public record ProjectStatusRequest(@NotNull ContentStatus status) {
}
