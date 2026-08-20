package com.portfolio.research.dto;

import com.portfolio.common.content.ContentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Whole-row replace; the tags sent become the complete set. */
public record ResearchUpdateRequest(
		@NotBlank @Size(max = 250) String title,
		@Schema(description = "Optional. Derived from the title when omitted.") @Size(max = 270) String slug,
		@NotBlank @Size(max = 600) String abstractText,
		@Size(max = 250) String publicationVenue,
		LocalDate publicationDate,
		@Size(max = 500) String externalUrl,
		Long pdfMediaId,
		@Schema(description = "Tag names, shared with Articles; unknown ones are created")
				List<String> tags,
		@Schema(description = "Defaults to DRAFT") ContentStatus status,
		Integer displayOrder,
		Boolean aiVisible) {
}
