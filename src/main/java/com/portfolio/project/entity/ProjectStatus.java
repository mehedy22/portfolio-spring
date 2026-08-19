package com.portfolio.project.entity;

/** Mirrors {@code ck_project_status}. Public endpoints only ever return {@link #PUBLISHED} rows. */
public enum ProjectStatus {
	DRAFT,
	PUBLISHED,
	ARCHIVED
}
