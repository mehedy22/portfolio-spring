package com.portfolio.problemsolving.dto;

import com.portfolio.common.content.ContentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProblemSolvingProfileCreateRequest(
		@NotBlank @Size(max = 50) @Schema(example = "LeetCode") String platform,
		@NotBlank @Size(max = 100) @Schema(description = "Judge id / username") String handle,
		@Size(max = 500) String profileUrl,
		@Min(0) Integer problemsSolved,
		@Min(0) Integer rating,
		@Size(max = 100) @Schema(description = "The platform's own tier name", example = "Knight")
				String rankTitle,
		Integer displayOrder,
		@Schema(description = "Defaults to PUBLISHED") ContentStatus status,
		Boolean aiVisible) {
}
