package com.portfolio.research.dto;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.media.dto.MediaResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ResearchResponse(
		Long id,
		String title,
		String slug,
		String abstractText,
		String publicationVenue,
		LocalDate publicationDate,
		String externalUrl,
		MediaResponse pdf,
		List<String> tags,
		ContentStatus status,
		int displayOrder,
		boolean aiVisible,
		Instant createdAt,
		Instant updatedAt) {
}
