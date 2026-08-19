package com.portfolio.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** One challenge → solution block in a project's create/update payload. */
public record ProjectChallengeRequest(
		@NotBlank @Size(max = 200) String title,
		@NotBlank String challenge,
		@NotBlank String solution) {
}
