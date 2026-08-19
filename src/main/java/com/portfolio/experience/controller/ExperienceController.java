package com.portfolio.experience.controller;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.common.response.ApiResponse;
import com.portfolio.experience.dto.ExperienceCreateRequest;
import com.portfolio.experience.dto.ExperienceResponse;
import com.portfolio.experience.dto.ExperienceUpdateRequest;
import com.portfolio.experience.service.ExperienceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Experience endpoints per docs/07-api/endpoints.md. Public reads are unpaginated (NFR-03). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Experience", description = "Work experience (FR-04)")
public class ExperienceController {

	private final ExperienceService experienceService;

	public ExperienceController(ExperienceService experienceService) {
		this.experienceService = experienceService;
	}

	@GetMapping("/experience")
	@Operation(summary = "List published experience", description = "Ordered by display order.")
	public ApiResponse<List<ExperienceResponse>> listPublished() {
		return ApiResponse.of(experienceService.listPublished());
	}

	@GetMapping("/admin/experience")
	@Operation(summary = "List experience", description = "All statuses, optional ?status= filter.")
	public ApiResponse<List<ExperienceResponse>> list(@RequestParam(required = false) ContentStatus status) {
		return ApiResponse.of(experienceService.listForAdmin(status));
	}

	@PostMapping("/admin/experience")
	@Operation(summary = "Create experience", description = "Defaults to DRAFT unless a status is sent.")
	public ResponseEntity<ApiResponse<ExperienceResponse>> create(
			@Valid @RequestBody ExperienceCreateRequest request) {
		ExperienceResponse created = experienceService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created, "Experience created"));
	}

	@GetMapping("/admin/experience/{id}")
	@Operation(summary = "Get experience", description = "Any status.")
	public ApiResponse<ExperienceResponse> get(@PathVariable Long id) {
		return ApiResponse.of(experienceService.getForAdmin(id));
	}

	@PutMapping("/admin/experience/{id}")
	@Operation(summary = "Replace experience", description = "The technologies sent become the complete set.")
	public ApiResponse<ExperienceResponse> update(
			@PathVariable Long id, @Valid @RequestBody ExperienceUpdateRequest request) {
		return ApiResponse.of(experienceService.update(id, request), "Experience updated");
	}

	@DeleteMapping("/admin/experience/{id}")
	@Operation(summary = "Delete experience", description = "Soft delete.")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		experienceService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
