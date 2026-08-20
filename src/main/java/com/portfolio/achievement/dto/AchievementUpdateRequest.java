package com.portfolio.achievement.dto;

import com.portfolio.common.content.ContentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AchievementUpdateRequest(
		@NotBlank @Size(max = 200) String title,
		String description,
		LocalDate achievedOn,
		Long imageMediaId,
		Integer displayOrder,
		ContentStatus status,
		Boolean aiVisible) {
}
