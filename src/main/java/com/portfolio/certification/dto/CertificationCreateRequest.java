package com.portfolio.certification.dto;

import com.portfolio.common.content.ContentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CertificationCreateRequest(
		@NotBlank @Size(max = 200) String name,
		@NotBlank @Size(max = 200) String issuer,
		@Size(max = 200) String credentialId,
		@Size(max = 500) String credentialUrl,
		LocalDate issueDate,
		LocalDate expiryDate,
		String description,
		Long certificateImageMediaId,
		Integer displayOrder,
		@Schema(description = "Defaults to PUBLISHED") ContentStatus status,
		Boolean aiVisible) {
}
