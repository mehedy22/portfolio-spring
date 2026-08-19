package com.portfolio.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One "challenge → solution" block, the pattern the reference site uses (three per project,
 * docs/01-requirements/reference-analysis.md). Normalized instead of two flat text columns on
 * {@code project}, and CASCADE-deleted: a challenge has no life outside its project.
 */
@Entity
@Table(name = "project_challenge")
public class ProjectChallenge {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "challenge", nullable = false, columnDefinition = "text")
	private String challenge;

	@Column(name = "solution", nullable = false, columnDefinition = "text")
	private String solution;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	protected ProjectChallenge() {
		// JPA
	}

	public ProjectChallenge(Project project, String title, String challenge, String solution, int displayOrder) {
		this.project = project;
		this.title = title;
		this.challenge = challenge;
		this.solution = solution;
		this.displayOrder = displayOrder;
	}

	public Long getId() {
		return id;
	}

	public Project getProject() {
		return project;
	}

	public String getTitle() {
		return title;
	}

	public String getChallenge() {
		return challenge;
	}

	public String getSolution() {
		return solution;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}
}
