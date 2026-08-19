package com.portfolio.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * One ordered slot in a project's gallery.
 *
 * <p>It holds the media <em>id</em> rather than a {@code @ManyToOne Media}: media is soft-deleted
 * (D-019), and on a collection {@code @NotFound(IGNORE)} would leave null holes in an ordered list.
 * The service resolves the ids in one batch and simply omits any media that no longer exists, so a
 * deleted image drops out of the gallery instead of breaking the page.
 */
@Entity
@Table(name = "project_gallery")
public class ProjectGalleryItem {

	@EmbeddedId
	private ProjectGalleryId id;

	@MapsId("projectId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	protected ProjectGalleryItem() {
		// JPA
	}

	public ProjectGalleryItem(Project project, Long mediaId, int displayOrder) {
		this.project = project;
		this.id = new ProjectGalleryId(null, mediaId);
		this.displayOrder = displayOrder;
	}

	public Long getMediaId() {
		return id.getMediaId();
	}

	public Project getProject() {
		return project;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}
}
