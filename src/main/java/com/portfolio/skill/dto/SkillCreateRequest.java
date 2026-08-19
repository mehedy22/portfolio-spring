package com.portfolio.skill.dto;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.skill.entity.Proficiency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code category} is the category <em>name</em>, created if unknown — Phase 7 defines only a GET
 * for skill categories, so there is no id for the admin to send (D-022, same reasoning as D-020).
 */
public record SkillCreateRequest(
		@NotBlank @Size(max = 100) String name,
		@NotBlank @Size(max = 100) @Schema(example = "Backend") String category,
		Proficiency proficiency,
		@Schema(description = "Icon class name") @Size(max = 200) String icon,
		Integer displayOrder,
		Boolean featured,
		@Schema(description = "Defaults to PUBLISHED") ContentStatus status,
		Boolean aiVisible) {
}
