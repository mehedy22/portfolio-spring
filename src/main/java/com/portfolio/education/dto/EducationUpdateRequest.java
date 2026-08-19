package com.portfolio.education.dto;

import com.portfolio.common.content.ContentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record EducationUpdateRequest(
		@NotBlank @Size(max = 200) String institution,
		@Size(max = 200) String degree,
		@Size(max = 200) String field,
		String description,
		LocalDate startDate,
		LocalDate endDate,
		Boolean currentlyStudying,
		Long logoMediaId,
		Integer displayOrder,
		ContentStatus status,
		Boolean aiVisible) {
}
