package com.portfolio.experience.dto;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.experience.entity.EmploymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Whole-row replace; the technologies sent become the complete set. */
public record ExperienceUpdateRequest(
		@NotBlank @Size(max = 200) String company,
		@Size(max = 500) String companyUrl,
		@NotBlank @Size(max = 200) String position,
		EmploymentType employmentType,
		String description,
		String responsibilities,
		@NotNull LocalDate startDate,
		LocalDate endDate,
		Boolean currentlyWorking,
		Long companyLogoMediaId,
		Integer displayOrder,
		ContentStatus status,
		Boolean aiVisible,
		List<String> technologies) {
}
