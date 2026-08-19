package com.portfolio.skill.dto;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.skill.entity.Proficiency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SkillUpdateRequest(
		@NotBlank @Size(max = 100) String name,
		@NotBlank @Size(max = 100) String category,
		Proficiency proficiency,
		@Size(max = 200) String icon,
		Integer displayOrder,
		Boolean featured,
		ContentStatus status,
		Boolean aiVisible) {
}
