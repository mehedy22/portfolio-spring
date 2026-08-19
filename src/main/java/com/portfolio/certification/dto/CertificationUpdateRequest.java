package com.portfolio.certification.dto;

import com.portfolio.common.content.ContentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CertificationUpdateRequest(
		@NotBlank @Size(max = 200) String name,
		@NotBlank @Size(max = 200) String issuer,
		@Size(max = 200) String credentialId,
		@Size(max = 500) String credentialUrl,
		LocalDate issueDate,
		LocalDate expiryDate,
		String description,
		Long certificateImageMediaId,
		Integer displayOrder,
		ContentStatus status,
		Boolean aiVisible) {
}
