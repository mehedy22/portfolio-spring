package com.portfolio.project.dto;

import com.portfolio.media.dto.MediaResponse;
import com.portfolio.project.entity.ProjectStatus;
import com.portfolio.project.entity.ProjectType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Full detail shape, backing both the public project page and the admin form.
 *
 * <p>{@code status} and {@code aiVisible} are included on the public surface too. Neither is
 * sensitive — public detail only ever resolves PUBLISHED rows, and {@code aiVisible} is a flag
 * about a FUTURE module, not about the reader — and one shape keeps the generated client simple.
 */
public record ProjectResponse(
		Long id,
		String title,
		String slug,
		String shortDescription,
		String detailedDescription,
		MediaResponse thumbnail,
		String githubUrl,
		String liveUrl,
		ProjectType projectType,
		LocalDate startDate,
		LocalDate endDate,
		boolean featured,
		ProjectStatus status,
		int displayOrder,
		String features,
		boolean aiVisible,
		List<String> technologies,
		List<ProjectChallengeResponse> challenges,
		List<MediaResponse> gallery,
		Instant createdAt,
		Instant updatedAt) {
}
