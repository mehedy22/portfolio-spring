package com.portfolio.achievement.dto;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.media.dto.MediaResponse;
import java.time.Instant;
import java.time.LocalDate;

public record AchievementResponse(
		Long id,
		String title,
		String description,
		LocalDate achievedOn,
		MediaResponse image,
		int displayOrder,
		ContentStatus status,
		boolean aiVisible,
		Instant createdAt,
		Instant updatedAt) {
}
