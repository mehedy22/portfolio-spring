package com.portfolio.media.service;

import com.portfolio.common.exception.ValidationException;
import com.portfolio.media.entity.Media;
import com.portfolio.media.repository.MediaRepository;
import org.springframework.stereotype.Component;

/**
 * Resolves a media id a content module wants to point at, rejecting the write when it does not
 * exist. Every content module (project thumbnail, experience logo, education logo, certificate
 * image) needs exactly this, so the "unknown media is a 400, not a dangling FK" rule lives here
 * once instead of in each service.
 */
@Component
public class MediaReferenceResolver {

	private final MediaRepository mediaRepository;

	public MediaReferenceResolver(MediaRepository mediaRepository) {
		this.mediaRepository = mediaRepository;
	}

	/**
	 * @param mediaId the referenced media, or null for "no image"
	 * @param field the request field name, so the error names what the admin actually sent
	 * @return the media row, or null when {@code mediaId} is null
	 */
	public Media resolve(Long mediaId, String field) {
		if (mediaId == null) {
			return null;
		}
		return mediaRepository
				.findById(mediaId)
				.orElseThrow(() -> new ValidationException(
						"%s references media %d, which does not exist".formatted(field, mediaId)));
	}
}
