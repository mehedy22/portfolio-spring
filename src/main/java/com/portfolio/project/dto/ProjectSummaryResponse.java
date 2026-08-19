package com.portfolio.project.dto;

import com.portfolio.media.dto.MediaResponse;
import com.portfolio.project.entity.ProjectStatus;
import com.portfolio.project.entity.ProjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** List-row shape — what a project card needs, without the detail-page body. */
@Schema(description = "A project as it appears in a list")
public record ProjectSummaryResponse(
		Long id,
		String title,
		String slug,
		String shortDescription,
		MediaResponse thumbnail,
		List<String> technologies,
		boolean featured,
		ProjectType projectType,
		LocalDate startDate,
		LocalDate endDate,
		int displayOrder,
		ProjectStatus status) {
}
