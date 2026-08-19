package com.portfolio.media.controller;

import com.portfolio.common.response.ApiResponse;
import com.portfolio.common.response.PageResponse;
import com.portfolio.media.dto.MediaResponse;
import com.portfolio.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Media endpoints per docs/07-api/endpoints.md.
 *
 * <p>Management lives under {@code /api/v1/admin/media} (JWT required); the bytes are served from
 * the public {@code /api/v1/media/{id}/content}, because the Public Site renders images for
 * anonymous visitors (D-005 — visitors never authenticate).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Media", description = "Uploaded images and documents (FR-08)")
public class MediaController {

	private static final int DEFAULT_PAGE_SIZE = 20;

	/** Stored bytes are immutable — a given id always serves the same file, so cache it hard. */
	private static final String CONTENT_CACHE_CONTROL = "public, max-age=31536000, immutable";

	private final MediaService mediaService;

	public MediaController(MediaService mediaService) {
		this.mediaService = mediaService;
	}

	@PostMapping(path = "/admin/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(
			summary = "Upload a file",
			description = "Type is determined by content sniffing, not by the declared Content-Type "
					+ "or the filename extension. Allowed: JPEG, PNG, GIF, WebP, PDF.")
	public ResponseEntity<ApiResponse<MediaResponse>> upload(
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "altText", required = false) String altText,
			Authentication authentication) {

		MediaResponse uploaded = mediaService.upload(file, altText, (Long) authentication.getPrincipal());
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(uploaded, "File uploaded"));
	}

	@GetMapping("/admin/media")
	@Operation(summary = "List media", description = "The admin media library, newest first.")
	public ApiResponse<PageResponse<MediaResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {

		return ApiResponse.of(mediaService.list(page, size));
	}

	@DeleteMapping("/admin/media/{id}")
	@Operation(
			summary = "Delete media",
			description = "Soft-deletes the record and removes the stored file. Content referencing "
					+ "it keeps its other fields and simply loses the image (D-019).")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		mediaService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/media/{id}/content")
	@Operation(summary = "Serve a file", description = "Public: the Public Site renders these to anonymous visitors.")
	public ResponseEntity<Resource> content(@PathVariable Long id) {
		MediaService.MediaContent content = mediaService.loadContent(id);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(content.mimeType()))
				.contentLength(content.sizeBytes())
				.header(HttpHeaders.CACHE_CONTROL, CONTENT_CACHE_CONTROL)
				// The type was verified by content sniffing on upload, so it is safe to declare —
				// but nosniff keeps the browser from second-guessing it into something executable.
				.header("X-Content-Type-Options", "nosniff")
				.header(
						HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.inline()
								.filename(content.originalFileName())
								.build()
								.toString())
				.body(content.resource());
	}
}
