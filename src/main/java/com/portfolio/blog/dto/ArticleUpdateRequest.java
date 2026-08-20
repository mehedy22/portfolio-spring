package com.portfolio.blog.dto;

import com.portfolio.blog.entity.ArticleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Whole-row replace; the tags sent become the complete set. Content is sanitized on write. */
public record ArticleUpdateRequest(
		@NotBlank @Size(max = 250) String title,
		@Schema(description = "Optional. Derived from the title when omitted.") @Size(max = 270) String slug,
		@Size(max = 500) String excerpt,
		@NotBlank String content,
		Long thumbnailMediaId,
		Long ogImageMediaId,
		@Schema(description = "Category name; created if unknown") @Size(max = 100) String category,
		@Schema(description = "Tag names; unknown ones are created") List<String> tags,
		@Schema(description = "Defaults to DRAFT") ArticleStatus status,
		@Schema(description = "Required for SCHEDULED/PUBLISHED; a future value schedules it")
				Instant publishedAt,
		@Size(max = 200) String seoTitle,
		@Size(max = 300) String seoDescription,
		Boolean aiVisible) {
}
