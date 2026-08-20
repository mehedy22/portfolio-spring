package com.portfolio.problemsolving.dto;

import com.portfolio.common.content.ContentStatus;
import java.time.Instant;

public record ProblemSolvingProfileResponse(
		Long id,
		String platform,
		String handle,
		String profileUrl,
		Integer problemsSolved,
		Integer rating,
		String rankTitle,
		int displayOrder,
		ContentStatus status,
		boolean aiVisible,
		Instant createdAt,
		Instant updatedAt) {
}
