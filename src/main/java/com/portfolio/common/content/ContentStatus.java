package com.portfolio.common.content;

/**
 * The publication state shared by every content entity (project, experience, skill, education,
 * certification, and later article/research). One enum rather than one per module: the values,
 * the CHECK constraints and the "published only" public rule are identical everywhere, so a
 * per-module copy would be five chances to drift apart.
 *
 * <p>Public endpoints only ever return {@link #PUBLISHED} rows.
 */
public enum ContentStatus {

	/** Not yet visible to anyone but the admin. */
	DRAFT,

	/** Live on the public site. */
	PUBLISHED,

	/** Deliberately retired, kept for the record — invisible publicly, same as a draft. */
	ARCHIVED
}
