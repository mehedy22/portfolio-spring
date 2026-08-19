package com.portfolio.project.dto;

import com.portfolio.project.entity.ProjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Whole-aggregate create: challenges, gallery and technologies arrive with the project rather
 * than through separate endpoints (docs/07-api/endpoints.md).
 *
 * <p>New projects are always created as DRAFT — publishing is a deliberate second step through
 * {@code PATCH /status} (docs/11-technical-design/sequence-diagrams.md).
 */
public record ProjectCreateRequest(
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
		@Schema(description = "Freeform list, e.g. markdown bullets") String features,
		Boolean aiVisible,
		@Schema(description = "Technology names; unknown ones are created") List<String> technologies,
		@Valid List<ProjectChallengeRequest> challenges,
		@Schema(description = "Media ids, in gallery order") List<Long> galleryMediaIds) {
}
