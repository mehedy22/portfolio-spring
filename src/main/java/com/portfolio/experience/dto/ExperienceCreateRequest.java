package com.portfolio.experience.dto;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.experience.entity.EmploymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Status is settable here, unlike Projects: Phase 7 defines no {@code PATCH /status} for this
 * module, so create/update is the only place it can move (D-022).
 */
public record ExperienceCreateRequest(
		@NotBlank @Size(max = 200) String company,
		@NotBlank @Size(max = 200) String position,
		EmploymentType employmentType,
		String description,
		String responsibilities,
		@NotNull LocalDate startDate,
		LocalDate endDate,
		Boolean currentlyWorking,
		Long companyLogoMediaId,
		Integer displayOrder,
		@Schema(description = "Defaults to DRAFT") ContentStatus status,
		Boolean aiVisible,
		@Schema(description = "Technology names; unknown ones are created") List<String> technologies) {
}
