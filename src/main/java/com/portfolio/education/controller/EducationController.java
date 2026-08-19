package com.portfolio.education.controller;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.common.response.ApiResponse;
import com.portfolio.education.dto.EducationCreateRequest;
import com.portfolio.education.dto.EducationResponse;
import com.portfolio.education.dto.EducationUpdateRequest;
import com.portfolio.education.service.EducationService;
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

/** Education endpoints per docs/07-api/endpoints.md. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Education", description = "Education history (FR-06)")
public class EducationController {

	private final EducationService educationService;

	public EducationController(EducationService educationService) {
		this.educationService = educationService;
	}

	@GetMapping("/education")
	@Operation(summary = "List published education", description = "Ordered by display order.")
	public ApiResponse<List<EducationResponse>> listPublished() {
		return ApiResponse.of(educationService.listPublished());
	}

	@GetMapping("/admin/education")
	@Operation(summary = "List education", description = "All statuses, optional ?status= filter.")
	public ApiResponse<List<EducationResponse>> list(@RequestParam(required = false) ContentStatus status) {
		return ApiResponse.of(educationService.listForAdmin(status));
	}

	@PostMapping("/admin/education")
	@Operation(summary = "Create education", description = "Defaults to PUBLISHED unless a status is sent.")
	public ResponseEntity<ApiResponse<EducationResponse>> create(
			@Valid @RequestBody EducationCreateRequest request) {
		EducationResponse created = educationService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created, "Education created"));
	}

	@GetMapping("/admin/education/{id}")
	@Operation(summary = "Get education", description = "Any status.")
	public ApiResponse<EducationResponse> get(@PathVariable Long id) {
		return ApiResponse.of(educationService.getForAdmin(id));
	}

	@PutMapping("/admin/education/{id}")
	@Operation(summary = "Replace education", description = "Whole-row update.")
	public ApiResponse<EducationResponse> update(
			@PathVariable Long id, @Valid @RequestBody EducationUpdateRequest request) {
		return ApiResponse.of(educationService.update(id, request), "Education updated");
	}

	@DeleteMapping("/admin/education/{id}")
	@Operation(summary = "Delete education", description = "Soft delete.")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		educationService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
