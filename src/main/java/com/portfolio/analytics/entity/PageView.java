package com.portfolio.analytics.entity;

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

/**
 * One recorded page view. Column shape mirrors V11__create_page_view_table.sql exactly.
 *
 * <p>Immutable by construction: there are no setters and no {@code @PreUpdate}, because a log
 * entry is never edited.
 */
@Entity
@Table(name = "page_view")
public class PageView {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "path", nullable = false, length = 500)
	private String path;

	@Column(name = "entity_type", length = 30)
	private String entityType;

	/** No FK: the log must survive the deletion of whatever it refers to. */
	@Column(name = "entity_id")
	private Long entityId;

	@Column(name = "referrer", length = 500)
	private String referrer;

	@Enumerated(EnumType.STRING)
	@Column(name = "device_type", length = 30)
	private DeviceType deviceType;

	@Column(name = "browser", length = 50)
	private String browser;

	@Column(name = "viewed_at", nullable = false)
	private Instant viewedAt;

	protected PageView() {
		// JPA
	}

	public PageView(
			String path,
			String entityType,
			Long entityId,
			String referrer,
			DeviceType deviceType,
			String browser) {
		this.path = path;
		this.entityType = entityType;
		this.entityId = entityId;
		this.referrer = referrer;
		this.deviceType = deviceType;
		this.browser = browser;
	}

	@PrePersist
	void onCreate() {
		if (viewedAt == null) {
			viewedAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public String getPath() {
		return path;
	}

	public String getEntityType() {
		return entityType;
	}

	public Long getEntityId() {
		return entityId;
	}

	public String getReferrer() {
		return referrer;
	}

	public DeviceType getDeviceType() {
		return deviceType;
	}

	public String getBrowser() {
		return browser;
	}

	public Instant getViewedAt() {
		return viewedAt;
	}
}
