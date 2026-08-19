package com.portfolio.certification.dto;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.media.dto.MediaResponse;
import java.time.Instant;
import java.time.LocalDate;

public record CertificationResponse(
		Long id,
		String name,
		String issuer,
		String credentialId,
		String credentialUrl,
		LocalDate issueDate,
		LocalDate expiryDate,
		String description,
		MediaResponse certificateImage,
		int displayOrder,
		ContentStatus status,
		boolean aiVisible,
		Instant createdAt,
		Instant updatedAt) {
}
