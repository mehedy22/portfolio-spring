package com.portfolio.skill.dto;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.skill.entity.Proficiency;
import java.time.Instant;

public record SkillResponse(
		Long id,
		String name,
		String category,
		Proficiency proficiency,
		String icon,
		int displayOrder,
		boolean featured,
		ContentStatus status,
		boolean aiVisible,
		Instant createdAt,
		Instant updatedAt) {
}
