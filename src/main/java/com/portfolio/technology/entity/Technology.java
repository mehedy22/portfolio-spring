package com.portfolio.technology.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A shared technology tag ("Spring Boot", "Redis"). Lives in its own package rather than under
 * {@code project} because Experience references the same rows from Sprint 4
 * (docs/06-database/table-definitions.md — {@code experience_technology}).
 *
 * <p>A pure lookup: no audit columns and no soft delete, matching the two-column shape Phase 6
 * specifies for {@code technology} and {@code skill_category}.
 */
@Entity
@Table(name = "technology")
public class Technology {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 100, unique = true)
	private String name;

	protected Technology() {
		// JPA
	}

	public Technology(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
