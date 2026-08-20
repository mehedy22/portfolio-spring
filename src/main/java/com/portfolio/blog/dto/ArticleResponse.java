package com.portfolio.blog.dto;

import com.portfolio.blog.entity.ArticleStatus;
import com.portfolio.media.dto.MediaResponse;
import java.time.Instant;
import java.util.List;

/** Full article. {@code content} is sanitized HTML — safe to render as markup. */
public record ArticleResponse(
		Long id,
		String title,
		String slug,
		String excerpt,
		String content,
		MediaResponse thumbnail,
		MediaResponse ogImage,
		String category,
		List<String> tags,
		ArticleStatus status,
		Instant publishedAt,
		Integer readingTimeMinutes,
		String seoTitle,
		String seoDescription,
		boolean aiVisible,
		Instant createdAt,
		Instant updatedAt) {
}
