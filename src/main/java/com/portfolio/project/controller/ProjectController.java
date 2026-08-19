package com.portfolio.project.controller;

import com.portfolio.common.response.ApiResponse;
import com.portfolio.common.response.PageResponse;
import com.portfolio.project.dto.ProjectCreateRequest;
import com.portfolio.project.dto.ProjectResponse;
import com.portfolio.project.dto.ProjectStatusRequest;
import com.portfolio.project.dto.ProjectSummaryResponse;
import com.portfolio.project.dto.ProjectUpdateRequest;
import com.portfolio.common.content.ContentStatus;
import com.portfolio.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Project endpoints per docs/07-api/endpoints.md.
 *
 * <p>The public routes have no {@code status} parameter at all — they call service methods that
 * can only return PUBLISHED rows, so an unpublished project is unreachable rather than filtered.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Projects", description = "Portfolio projects (FR-02, FR-03)")
public class ProjectController {

	private static final int DEFAULT_PAGE_SIZE = 20;

	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	// --------------------------------------------------------------- public

	@GetMapping("/projects")
	@Operation(summary = "List published projects", description = "Ordered by display order. Not paginated.")
	public ApiResponse<List<ProjectSummaryResponse>> listPublished() {
		return ApiResponse.of(projectService.listPublished());
	}

	@GetMapping("/projects/{slug}")
	@Operation(summary = "Get a published project", description = "404 when unknown or not published.")
	public ApiResponse<ProjectResponse> getBySlug(@PathVariable String slug) {
		return ApiResponse.of(projectService.getPublishedBySlug(slug));
	}

	// ---------------------------------------------------------------- admin

	@GetMapping("/admin/projects")
	@Operation(summary = "List projects", description = "All statuses, paginated, optional ?status= filter.")
	public ApiResponse<PageResponse<ProjectSummaryResponse>> list(
			@RequestParam(required = false) ContentStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
			@RequestParam(required = false) String sort) {

		return ApiResponse.of(projectService.listForAdmin(status, page, size, sort));
	}

	@PostMapping("/admin/projects")
	@Operation(summary = "Create a project", description = "Always created as DRAFT.")
	public ResponseEntity<ApiResponse<ProjectResponse>> create(@Valid @RequestBody ProjectCreateRequest request) {
		ProjectResponse created = projectService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created, "Project created"));
	}

	@GetMapping("/admin/projects/{id}")
	@Operation(summary = "Get a project", description = "Any status.")
	public ApiResponse<ProjectResponse> get(@PathVariable Long id) {
		return ApiResponse.of(projectService.getForAdmin(id));
	}

	@PutMapping("/admin/projects/{id}")
	@Operation(
			summary = "Replace a project",
			description = "Whole-aggregate update: the challenges, gallery and technologies sent "
					+ "become the complete set. Status is not changed here.")
	public ApiResponse<ProjectResponse> update(
			@PathVariable Long id, @Valid @RequestBody ProjectUpdateRequest request) {
		return ApiResponse.of(projectService.update(id, request), "Project updated");
	}

	@PatchMapping("/admin/projects/{id}/status")
	@Operation(summary = "Publish / unpublish", description = "DRAFT | PUBLISHED | ARCHIVED.")
	public ApiResponse<ProjectResponse> updateStatus(
			@PathVariable Long id, @Valid @RequestBody ProjectStatusRequest request) {
		return ApiResponse.of(projectService.updateStatus(id, request.status()), "Status updated");
	}

	@DeleteMapping("/admin/projects/{id}")
	@Operation(summary = "Delete a project", description = "Soft delete.")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		projectService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
