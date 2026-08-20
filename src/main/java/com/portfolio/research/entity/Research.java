package com.portfolio.research.entity;

import com.portfolio.blog.entity.Tag;
import com.portfolio.common.content.ContentStatus;
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
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * A research entry (D-014). Column shape mirrors V15__create_research_tables.sql exactly.
 *
 * <p>Unlike an article, this hosts no content: it points OUT to a paper via {@code externalUrl} or
 * an uploaded PDF. That is why there is no rich-text body, no sanitizer and no detail route.
 */
@Entity
@Table(name = "research")
@SQLDelete(sql = "UPDATE research SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Research {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title", nullable = false, length = 250)
	private String title;

	// Uniqueness is the partial index uq_research_slug_active (live rows only) — see D-021.
	@Column(name = "slug", nullable = false, length = 270)
	private String slug;

	@Column(name = "abstract", nullable = false, length = 600)
	private String abstractText;

	@Column(name = "publication_venue", length = 250)
	private String publicationVenue;

	@Column(name = "publication_date")
	private LocalDate publicationDate;

	@Column(name = "external_url", length = 500)
	private String externalUrl;

	/** Null rather than an exception when the PDF has since been deleted (D-019). */
	@ManyToOne(fetch = FetchType.LAZY)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "pdf_media_id")
	private Media pdf;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ContentStatus status = ContentStatus.DRAFT;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "ai_visible", nullable = false)
	private boolean aiVisible;

	/** Shares Blog's {@code tag} table, so a tag used on both sides is one row. */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "research_tag",
			joinColumns = @JoinColumn(name = "research_id"),
			inverseJoinColumns = @JoinColumn(name = "tag_id"))
	private Set<Tag> tags = new LinkedHashSet<>();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public Research() {
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

	public String getAbstractText() {
		return abstractText;
	}

	public void setAbstractText(String abstractText) {
		this.abstractText = abstractText;
	}

	public String getPublicationVenue() {
		return publicationVenue;
	}

	public void setPublicationVenue(String publicationVenue) {
		this.publicationVenue = publicationVenue;
	}

	public LocalDate getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(LocalDate publicationDate) {
		this.publicationDate = publicationDate;
	}

	public String getExternalUrl() {
		return externalUrl;
	}

	public void setExternalUrl(String externalUrl) {
		this.externalUrl = externalUrl;
	}

	public Media getPdf() {
		return pdf;
	}

	public void setPdf(Media pdf) {
		this.pdf = pdf;
	}

	public ContentStatus getStatus() {
		return status;
	}

	public void setStatus(ContentStatus status) {
		this.status = status;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
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
