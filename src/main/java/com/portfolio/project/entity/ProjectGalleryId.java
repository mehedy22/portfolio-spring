package com.portfolio.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Composite key of {@code project_gallery} — the PK Phase 6 specifies, no surrogate id added. */
@Embeddable
public class ProjectGalleryId implements Serializable {

	@Column(name = "project_id")
	private Long projectId;

	@Column(name = "media_id")
	private Long mediaId;

	protected ProjectGalleryId() {
		// JPA
	}

	public ProjectGalleryId(Long projectId, Long mediaId) {
		this.projectId = projectId;
		this.mediaId = mediaId;
	}

	public Long getProjectId() {
		return projectId;
	}

	public Long getMediaId() {
		return mediaId;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ProjectGalleryId that)) {
			return false;
		}
		return Objects.equals(projectId, that.projectId) && Objects.equals(mediaId, that.mediaId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(projectId, mediaId);
	}
}
