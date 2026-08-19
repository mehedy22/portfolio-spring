package com.portfolio.media.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * An uploaded file. Column shape mirrors V2__create_media_table.sql exactly —
 * {@code ddl-auto=validate} fails the boot on any drift.
 *
 * <p>Soft-deleted per docs/06-database/constraints-and-indexes.md: {@code delete()} becomes an
 * UPDATE and {@link SQLRestriction} hides the row from every query, so a deleted file disappears
 * from the library and from any content that still references it, without the row being lost.
 */
@Entity
@Table(name = "media")
@SQLDelete(sql = "UPDATE media SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Media {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Server-generated storage name — never the user-supplied one. */
	@Column(name = "file_name", nullable = false, length = 255)
	private String fileName;

	@Column(name = "original_file_name", nullable = false, length = 255)
	private String originalFileName;

	/** The <em>detected</em> type, not the client-declared one. */
	@Column(name = "mime_type", nullable = false, length = 100)
	private String mimeType;

	@Column(name = "size_bytes", nullable = false)
	private Long sizeBytes;

	@Enumerated(EnumType.STRING)
	@Column(name = "storage_backend", nullable = false, length = 20)
	private StorageBackend storageBackend;

	@Column(name = "storage_path_or_url", nullable = false, length = 1000)
	private String storagePathOrUrl;

	@Column(name = "width")
	private Integer width;

	@Column(name = "height")
	private Integer height;

	@Column(name = "alt_text", length = 300)
	private String altText;

	/**
	 * Plain ID, not a {@code @ManyToOne}: Auth and Media are separate modules and the FK exists
	 * for referential integrity, not to make the uploader navigable from here.
	 */
	@Column(name = "uploaded_by_admin_id")
	private Long uploadedByAdminId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected Media() {
		// JPA
	}

	public Media(
			String fileName,
			String originalFileName,
			String mimeType,
			long sizeBytes,
			StorageBackend storageBackend,
			String storagePathOrUrl,
			Integer width,
			Integer height,
			String altText,
			Long uploadedByAdminId) {
		this.fileName = fileName;
		this.originalFileName = originalFileName;
		this.mimeType = mimeType;
		this.sizeBytes = sizeBytes;
		this.storageBackend = storageBackend;
		this.storagePathOrUrl = storagePathOrUrl;
		this.width = width;
		this.height = height;
		this.altText = altText;
		this.uploadedByAdminId = uploadedByAdminId;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public String getFileName() {
		return fileName;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public String getMimeType() {
		return mimeType;
	}

	public Long getSizeBytes() {
		return sizeBytes;
	}

	public StorageBackend getStorageBackend() {
		return storageBackend;
	}

	public String getStoragePathOrUrl() {
		return storagePathOrUrl;
	}

	public Integer getWidth() {
		return width;
	}

	public Integer getHeight() {
		return height;
	}

	public String getAltText() {
		return altText;
	}

	public Long getUploadedByAdminId() {
		return uploadedByAdminId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getDeletedAt() {
		return deletedAt;
	}
}
