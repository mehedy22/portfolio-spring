package com.portfolio.media.service;

import com.portfolio.common.response.PageResponse;
import com.portfolio.media.dto.MediaResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {

	/**
	 * Validates {@code file} by content, stores it under a generated name, and records the row.
	 *
	 * @param altText optional accessibility text for images (docs/10-frontend/ux-states-and-quality.md)
	 * @param adminId the uploading admin, recorded on the row
	 */
	MediaResponse upload(MultipartFile file, String altText, Long adminId);

	/** Admin media library, newest first. */
	PageResponse<MediaResponse> list(int page, int size);

	/** Soft-deletes the row and removes the stored file. */
	void delete(Long id);

	/** The stored bytes plus the metadata needed to serve them. */
	MediaContent loadContent(Long id);

	/** What {@link #loadContent(Long)} hands the controller — no entity leaves the service layer. */
	record MediaContent(Resource resource, String mimeType, long sizeBytes, String originalFileName) {
	}
}
