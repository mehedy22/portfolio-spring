package com.portfolio.education.dto;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.media.dto.MediaResponse;
import java.time.Instant;
import java.time.LocalDate;

public record EducationResponse(
		Long id,
		String institution,
		String degree,
		String field,
		String description,
		LocalDate startDate,
		LocalDate endDate,
		boolean currentlyStudying,
		MediaResponse logo,
		int displayOrder,
		ContentStatus status,
		boolean aiVisible,
		Instant createdAt,
		Instant updatedAt) {
}
