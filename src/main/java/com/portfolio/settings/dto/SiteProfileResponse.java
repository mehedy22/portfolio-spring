package com.portfolio.settings.dto;

import com.portfolio.media.dto.MediaResponse;

/**
 * The profile photo and resume as media the client can fetch (D-015). Either may be null — the
 * sidebar renders without a photo, and the Resume button hides itself.
 */
public record SiteProfileResponse(MediaResponse profileImage, MediaResponse resume) {
}
