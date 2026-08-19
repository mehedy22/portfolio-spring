package com.portfolio.project.entity;

import com.portfolio.media.entity.Media;
import com.portfolio.technology.entity.Technology;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * A portfolio project. Column shape mirrors V4__create_project_tables.sql exactly —
 * {@code ddl-auto=validate} fails the boot on any drift.
 *
 * <p>Challenges, gallery and technologies are owned by the project and replaced wholesale on
 * update: the API treats a project as one aggregate (docs/07-api/endpoints.md).
 */
@Entity
@Table(name = "project")
@SQLDelete(sql = "UPDATE project SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	// Uniqueness is the partial index uq_project_slug_active (live rows only) — see V4 (D-021).
	@Column(name = "slug", nullable = false, length = 220)
	private String slug;

	@Column(name = "short_description", nullable = false, length = 500)
	private String shortDescription;

	@Column(name = "detailed_description", columnDefinition = "text")
	private String detailedDescription;

	/**
	 * Media is soft-deleted, so this can point at a row that {@code @SQLRestriction} hides.
	 * {@code NotFoundAction.IGNORE} makes that null instead of an {@code EntityNotFoundException} —
	 * a deleted thumbnail must render as absent, never as a failed request
	 * (docs/11-technical-design/backend-design.md, D-019).
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "thumbnail_media_id")
	private Media thumbnail;

	@Column(name = "github_url", length = 500)
	private String githubUrl;

	@Column(name = "live_url", length = 500)
	private String liveUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "project_type", length = 30)
	private ProjectType projectType;

	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Column(name = "featured", nullable = false)
	private boolean featured;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ProjectStatus status = ProjectStatus.DRAFT;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "features", columnDefinition = "text")
	private String features;

	@Column(name = "ai_visible", nullable = false)
	private boolean aiVisible;

	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("displayOrder ASC, id ASC")
	private List<ProjectChallenge> challenges = new ArrayList<>();

	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("displayOrder ASC")
	private List<ProjectGalleryItem> gallery = new ArrayList<>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "project_technology",
			joinColumns = @JoinColumn(name = "project_id"),
			inverseJoinColumns = @JoinColumn(name = "technology_id"))
	private Set<Technology> technologies = new LinkedHashSet<>();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public Project() {
		// JPA / new draft
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	/** Replaces the challenge blocks, renumbering them into the order they arrived in. */
	public void replaceChallenges(List<ProjectChallenge> replacements) {
		challenges.clear();
		challenges.addAll(replacements);
	}

	/** Replaces the gallery slots, in the order the ids arrived in. */
	public void replaceGallery(List<ProjectGalleryItem> replacements) {
		gallery.clear();
		gallery.addAll(replacements);
	}

	public void replaceTechnologies(Set<Technology> replacements) {
		technologies.clear();
		technologies.addAll(replacements);
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	public String getDetailedDescription() {
		return detailedDescription;
	}

	public void setDetailedDescription(String detailedDescription) {
		this.detailedDescription = detailedDescription;
	}

	public Media getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(Media thumbnail) {
		this.thumbnail = thumbnail;
	}

	public String getGithubUrl() {
		return githubUrl;
	}

	public void setGithubUrl(String githubUrl) {
		this.githubUrl = githubUrl;
	}

	public String getLiveUrl() {
		return liveUrl;
	}

	public void setLiveUrl(String liveUrl) {
		this.liveUrl = liveUrl;
	}

	public ProjectType getProjectType() {
		return projectType;
	}

	public void setProjectType(ProjectType projectType) {
		this.projectType = projectType;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public boolean isFeatured() {
		return featured;
	}

	public void setFeatured(boolean featured) {
		this.featured = featured;
	}

	public ProjectStatus getStatus() {
		return status;
	}

	public void setStatus(ProjectStatus status) {
		this.status = status;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}

	public String getFeatures() {
		return features;
	}

	public void setFeatures(String features) {
		this.features = features;
	}

	public boolean isAiVisible() {
		return aiVisible;
	}

	public void setAiVisible(boolean aiVisible) {
		this.aiVisible = aiVisible;
	}

	public List<ProjectChallenge> getChallenges() {
		return challenges;
	}

	public List<ProjectGalleryItem> getGallery() {
		return gallery;
	}

	public Set<Technology> getTechnologies() {
		return technologies;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
