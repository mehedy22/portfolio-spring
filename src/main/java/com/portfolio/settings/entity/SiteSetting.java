package com.portfolio.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One stored configuration value. The key is the primary key — there is no surrogate id, matching
 * V10 and the EAV shape Phase 6 chose for this table.
 *
 * <p>Rows only exist for settings that have been explicitly set; anything else reads as its
 * registry default (see {@code SettingKey}).
 */
@Entity
@Table(name = "site_setting")
public class SiteSetting {

	@Id
	@Column(name = "key", nullable = false, length = 100)
	private String key;

	@Column(name = "value", columnDefinition = "text")
	private String value;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected SiteSetting() {
		// JPA
	}

	public SiteSetting(String key, String value) {
		this.key = key;
		this.value = value;
	}

	@PrePersist
	@PreUpdate
	void touch() {
		updatedAt = Instant.now();
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
