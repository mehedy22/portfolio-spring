package com.portfolio.settings.entity;

import com.portfolio.media.entity.Media;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.Instant;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

/**
 * The singleton profile row (D-015): the photo shown in the Home hero and sidebar, and the resume
 * the download button serves. One row expected; the service creates it on first write.
 *
 * <p>Both references tolerate a deleted media row (D-019) — the photo simply disappears rather
 * than breaking every page that renders the sidebar.
 */
@Entity
@Table(name = "site_profile")
public class SiteProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "profile_image_media_id")
	private Media profileImage;

	@ManyToOne(fetch = FetchType.LAZY)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "resume_media_id")
	private Media resume;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public SiteProfile() {
		// JPA / first write
	}

	@PrePersist
	@PreUpdate
	void touch() {
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Media getProfileImage() {
		return profileImage;
	}

	public void setProfileImage(Media profileImage) {
		this.profileImage = profileImage;
	}

	public Media getResume() {
		return resume;
	}

	public void setResume(Media resume) {
		this.resume = resume;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
