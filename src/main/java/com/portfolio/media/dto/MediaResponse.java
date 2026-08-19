package com.portfolio.media.dto;

import com.portfolio.media.entity.StorageBackend;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * A media item as the Admin Panel and the Public Site see it.
 *
 * <p>{@code storagePathOrUrl} is deliberately absent: callers get {@link #url}, which routes
 * through the storage abstraction, so no filesystem path is ever exposed
 * (docs/08-security/application-security.md, "Serving uploaded files").
 */
@Schema(description = "An uploaded file")
public record MediaResponse(
		Long id,
		@Schema(description = "API path that serves the bytes", example = "/api/v1/media/1/content") String url,
		String fileName,
		String originalFileName,
		String mimeType,
		Long sizeBytes,
		StorageBackend storageBackend,
		Integer width,
		Integer height,
		String altText,
		Instant createdAt) {

	/** The one place the public content path is constructed. */
	public static String urlFor(Long id) {
		return "/api/v1/media/" + id + "/content";
	}
}
