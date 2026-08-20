package com.portfolio.project.service;

import com.portfolio.common.response.PageResponse;
import com.portfolio.project.dto.ProjectCreateRequest;
import com.portfolio.project.dto.ProjectResponse;
import com.portfolio.project.dto.ProjectSummaryResponse;
import com.portfolio.project.dto.ProjectUpdateRequest;
import com.portfolio.common.content.ContentStatus;
import java.util.List;

public interface ProjectService {

	/** Creates a DRAFT project with its challenges, gallery and technologies. */
	ProjectResponse create(ProjectCreateRequest request);

	/** Whole-aggregate replace. Status is untouched — only {@link #updateStatus} moves it. */
	ProjectResponse update(Long id, ProjectUpdateRequest request);

	ProjectResponse updateStatus(Long id, ContentStatus status);

	/** Soft delete. */
	void delete(Long id);

	ProjectResponse getForAdmin(Long id);

	PageResponse<ProjectSummaryResponse> listForAdmin(ContentStatus status, int page, int size, String sort);

	/**
	 * Published projects only, in display order — no status filter is accepted here by design.
	 *
	 * @param search optional free-text term matched against title, description and technologies
	 */
	List<ProjectSummaryResponse> listPublished(String search);

	/** 404 (not 403) when the slug is unknown <em>or</em> not published — drafts stay invisible. */
	ProjectResponse getPublishedBySlug(String slug);
}
