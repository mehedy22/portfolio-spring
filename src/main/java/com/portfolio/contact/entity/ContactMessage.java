package com.portfolio.contact.entity;

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
 * One submission from the public contact form. Column shape mirrors
 * V9__create_contact_message_table.sql exactly.
 *
 * <p>The body is stored verbatim, not sanitized: it is plain text that only ever renders inside
 * the Admin Panel, which escapes it. Phase 8's allow-list sanitization requirement targets
 * rich-text fields the public site renders as HTML — this is not one, and stripping characters
 * out of a stranger's message would corrupt legitimate mail (code snippets, angle brackets)
 * without closing an actual hole.
 */
@Entity
@Table(name = "contact_message")
@SQLDelete(sql = "UPDATE contact_message SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ContactMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@Column(name = "email", nullable = false, length = 255)
	private String email;

	@Column(name = "subject", length = 300)
	private String subject;

	@Column(name = "message", nullable = false, columnDefinition = "text")
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ContactMessageStatus status = ContactMessageStatus.NEW;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected ContactMessage() {
		// JPA
	}

	public ContactMessage(String name, String email, String subject, String message) {
		this.name = name;
		this.email = email;
		this.subject = subject;
		this.message = message;
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

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getSubject() {
		return subject;
	}

	public String getMessage() {
		return message;
	}

	public ContactMessageStatus getStatus() {
		return status;
	}

	public void setStatus(ContactMessageStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
