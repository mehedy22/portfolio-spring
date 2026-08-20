package com.portfolio.education.dto;

import com.portfolio.common.content.ContentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record EducationCreateRequest(
		@NotBlank @Size(max = 200) String institution,
		@Size(max = 200) String degree,
		@Size(max = 200) String field,
		@Schema(description = "GPA, CGPA, class or grade, as awarded") @Size(max = 50) String result,
		String description,
		LocalDate startDate,
		LocalDate endDate,
		Boolean currentlyStudying,
		Long logoMediaId,
		Integer displayOrder,
		@Schema(description = "Defaults to PUBLISHED") ContentStatus status,
		Boolean aiVisible) {
}
