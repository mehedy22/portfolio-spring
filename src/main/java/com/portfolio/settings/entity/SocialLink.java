package com.portfolio.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One social profile link, shown in the public sidebar and footer. No soft delete and no audit
 * columns — Phase 6 models this as a small ordered list the admin replaces wholesale, not as
 * content with a history.
 */
@Entity
@Table(name = "social_link")
public class SocialLink {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "platform", nullable = false, length = 50)
	private String platform;

	@Column(name = "url", nullable = false, length = 500)
	private String url;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "is_visible", nullable = false)
	private boolean visible = true;

	protected SocialLink() {
		// JPA
	}

	public SocialLink(String platform, String url, int displayOrder, boolean visible) {
		this.platform = platform;
		this.url = url;
		this.displayOrder = displayOrder;
		this.visible = visible;
	}

	public Long getId() {
		return id;
	}

	public String getPlatform() {
		return platform;
	}

	public String getUrl() {
		return url;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public boolean isVisible() {
		return visible;
	}
}
