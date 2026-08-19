package com.portfolio.experience.dto;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.experience.entity.EmploymentType;
import com.portfolio.media.dto.MediaResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ExperienceResponse(
		Long id,
		String company,
		String position,
		EmploymentType employmentType,
		String description,
		String responsibilities,
		LocalDate startDate,
		LocalDate endDate,
		boolean currentlyWorking,
		MediaResponse companyLogo,
		int displayOrder,
		ContentStatus status,
		boolean aiVisible,
		List<String> technologies,
		Instant createdAt,
		Instant updatedAt) {
}
