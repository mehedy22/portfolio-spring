package com.portfolio.problemsolving.entity;

import com.portfolio.common.content.ContentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * A competitive-programming or judge profile. Column shape mirrors
 * V16__create_problem_solving_profile_table.sql exactly.
 *
 * <p>Nothing here is fetched from the platform: the figures are what the admin chose to publish.
 * A number this site claims and never refreshes would be worse than no number.
 */
@Entity
@Table(name = "problem_solving_profile")
@SQLDelete(sql = "UPDATE problem_solving_profile SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ProblemSolvingProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "platform", nullable = false, length = 50)
	private String platform;

	/** The judge id / username on that platform. */
	@Column(name = "handle", nullable = false, length = 100)
	private String handle;

	@Column(name = "profile_url", length = 500)
	private String profileUrl;

	@Column(name = "problems_solved")
	private Integer problemsSolved;

	@Column(name = "rating")
	private Integer rating;

	/** The platform's own word for a tier — "Knight", "Expert", "5 star". */
	@Column(name = "rank_title", length = 100)
	private String rankTitle;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ContentStatus status = ContentStatus.PUBLISHED;

	@Column(name = "ai_visible", nullable = false)
	private boolean aiVisible;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public ProblemSolvingProfile() {
		// JPA / new row
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

	public Long getId() {
		return id;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public String getHandle() {
		return handle;
	}

	public void setHandle(String handle) {
		this.handle = handle;
	}

	public String getProfileUrl() {
		return profileUrl;
	}

	public void setProfileUrl(String profileUrl) {
		this.profileUrl = profileUrl;
	}

	public Integer getProblemsSolved() {
		return problemsSolved;
	}

	public void setProblemsSolved(Integer problemsSolved) {
		this.problemsSolved = problemsSolved;
	}

	public Integer getRating() {
		return rating;
	}

	public void setRating(Integer rating) {
		this.rating = rating;
	}

	public String getRankTitle() {
		return rankTitle;
	}

	public void setRankTitle(String rankTitle) {
		this.rankTitle = rankTitle;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}

	public ContentStatus getStatus() {
		return status;
	}

	public void setStatus(ContentStatus status) {
		this.status = status;
	}

	public boolean isAiVisible() {
		return aiVisible;
	}

	public void setAiVisible(boolean aiVisible) {
		this.aiVisible = aiVisible;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
