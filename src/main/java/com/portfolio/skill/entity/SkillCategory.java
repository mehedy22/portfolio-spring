package com.portfolio.skill.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A skill grouping ("Backend", "DevOps"). A pure lookup like {@code technology}: two columns, no
 * audit fields, no soft delete. The FK from {@code skill} is RESTRICT, so a category still in use
 * cannot be deleted out from under its skills.
 */
@Entity
@Table(name = "skill_category")
public class SkillCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 100, unique = true)
	private String name;

	protected SkillCategory() {
		// JPA
	}

	public SkillCategory(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
