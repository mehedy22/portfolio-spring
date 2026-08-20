package com.portfolio.blog.entity;

import com.portfolio.media.entity.Media;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * A blog article. Column shape mirrors V13__create_blog_tables.sql exactly.
 *
 * <p>{@code content} is the only rich-text field in the system; it is sanitized on write
 * (D-027) so what is stored here is already safe to render.
 */
@Entity
@Table(name = "article")
@SQLDelete(sql = "UPDATE article SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Article {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title", nullable = false, length = 250)
	private String title;

	// Uniqueness is the partial index uq_article_slug_active (live rows only) — see D-021.
	@Column(name = "slug", nullable = false, length = 270)
	private String slug;

	@Column(name = "excerpt", length = 500)
	private String excerpt;

	@Column(name = "content", nullable = false, columnDefinition = "text")
	private String content;

	@ManyToOne(fetch = FetchType.LAZY)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "thumbnail_media_id")
	private Media thumbnail;

	@ManyToOne(fetch = FetchType.LAZY)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "og_image_media_id")
	private Media ogImage;

	/** Plain id: Auth is a separate module and the author is not navigated from here. */
	@Column(name = "author_admin_id")
	private Long authorAdminId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "category_id")
	private Category category;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ArticleStatus status = ArticleStatus.DRAFT;

	/** For SCHEDULED, a future instant; the article becomes public when it passes. */
	@Column(name = "published_at")
	private Instant publishedAt;

	@Column(name = "reading_time_minutes")
	private Integer readingTimeMinutes;

	@Column(name = "seo_title", length = 200)
	private String seoTitle;

	@Column(name = "seo_description", length = 300)
	private String seoDescription;

	@Column(name = "ai_visible", nullable = false)
	private boolean aiVisible;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "article_tag",
			joinColumns = @JoinColumn(name = "article_id"),
			inverseJoinColumns = @JoinColumn(name = "tag_id"))
	private Set<Tag> tags = new LinkedHashSet<>();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public Article() {
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

	public void replaceTags(Set<Tag> replacements) {
		tags.clear();
		tags.addAll(replacements);
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

	public String getExcerpt() {
		return excerpt;
	}

	public void setExcerpt(String excerpt) {
		this.excerpt = excerpt;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Media getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(Media thumbnail) {
		this.thumbnail = thumbnail;
	}

	public Media getOgImage() {
		return ogImage;
	}

	public void setOgImage(Media ogImage) {
		this.ogImage = ogImage;
	}

	public Long getAuthorAdminId() {
		return authorAdminId;
	}

	public void setAuthorAdminId(Long authorAdminId) {
		this.authorAdminId = authorAdminId;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public ArticleStatus getStatus() {
		return status;
	}

	public void setStatus(ArticleStatus status) {
		this.status = status;
	}

	public Instant getPublishedAt() {
		return publishedAt;
	}

	public void setPublishedAt(Instant publishedAt) {
		this.publishedAt = publishedAt;
	}

	public Integer getReadingTimeMinutes() {
		return readingTimeMinutes;
	}

	public void setReadingTimeMinutes(Integer readingTimeMinutes) {
		this.readingTimeMinutes = readingTimeMinutes;
	}

	public String getSeoTitle() {
		return seoTitle;
	}

	public void setSeoTitle(String seoTitle) {
		this.seoTitle = seoTitle;
	}

	public String getSeoDescription() {
		return seoDescription;
	}

	public void setSeoDescription(String seoDescription) {
		this.seoDescription = seoDescription;
	}

	public boolean isAiVisible() {
		return aiVisible;
	}

	public void setAiVisible(boolean aiVisible) {
		this.aiVisible = aiVisible;
	}

	public Set<Tag> getTags() {
		return tags;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
