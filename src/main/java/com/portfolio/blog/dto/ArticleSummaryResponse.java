package com.portfolio.blog.dto;

import com.portfolio.blog.entity.ArticleStatus;
import com.portfolio.media.dto.MediaResponse;
import java.time.Instant;
import java.util.List;

/** List-row shape — deliberately without {@code content}, which is the whole article. */
public record ArticleSummaryResponse(
		Long id,
		String title,
		String slug,
		String excerpt,
		MediaResponse thumbnail,
		String category,
		List<String> tags,
		ArticleStatus status,
		Instant publishedAt,
		Integer readingTimeMinutes) {
}
