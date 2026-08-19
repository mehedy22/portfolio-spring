package com.portfolio.project.dto;

import com.portfolio.project.entity.ProjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Whole-aggregate replace: the challenges, gallery and technologies sent here become the
 * project's complete set — anything omitted is removed. Status is not settable here; it moves
 * only through {@code PATCH /status}, so a routine content edit can never publish by accident.
 */
public record ProjectUpdateRequest(
		@NotBlank @Size(max = 200) String title,
		@Schema(description = "Optional. Derived from the title when omitted.") @Size(max = 220) String slug,
		@NotBlank @Size(max = 500) String shortDescription,
		String detailedDescription,
		Long thumbnailMediaId,
		@Size(max = 500) String githubUrl,
		@Size(max = 500) String liveUrl,
		ProjectType projectType,
		LocalDate startDate,
		LocalDate endDate,
		Boolean featured,
		Integer displayOrder,
		String features,
		Boolean aiVisible,
		List<String> technologies,
		@Valid List<ProjectChallengeRequest> challenges,
		List<Long> galleryMediaIds) {
}
