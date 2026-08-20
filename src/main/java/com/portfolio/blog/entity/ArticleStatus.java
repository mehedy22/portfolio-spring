package com.portfolio.blog.entity;

/**
 * Editorial state, mirroring {@code ck_article_status}. Distinct from
 * {@code common.content.ContentStatus} because Blog adds {@link #SCHEDULED}: an article whose
 * {@code published_at} is in the future and which becomes public on its own, with no second
 * admin action.
 */
public enum ArticleStatus {
	DRAFT,
	SCHEDULED,
	PUBLISHED,
	ARCHIVED
}
