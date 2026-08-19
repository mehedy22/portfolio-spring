package com.portfolio.project.dto;

import com.portfolio.project.entity.ProjectStatus;
import jakarta.validation.constraints.NotNull;

/** Body of {@code PATCH /api/v1/admin/projects/{id}/status}. */
public record ProjectStatusRequest(@NotNull ProjectStatus status) {
}
