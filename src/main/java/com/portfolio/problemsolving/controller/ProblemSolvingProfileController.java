package com.portfolio.problemsolving.controller;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.common.response.ApiResponse;
import com.portfolio.problemsolving.dto.ProblemSolvingProfileCreateRequest;
import com.portfolio.problemsolving.dto.ProblemSolvingProfileResponse;
import com.portfolio.problemsolving.dto.ProblemSolvingProfileUpdateRequest;
import com.portfolio.problemsolving.service.ProblemSolvingProfileService;
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

/** Judge / competitive-programming profiles, shown with Skills and Certifications. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Problem solving", description = "Judge profiles (LeetCode, Codeforces, …)")
public class ProblemSolvingProfileController {

	private final ProblemSolvingProfileService profileService;

	public ProblemSolvingProfileController(ProblemSolvingProfileService profileService) {
		this.profileService = profileService;
	}

	@GetMapping("/problem-solving")
	@Operation(summary = "List published profiles", description = "Ordered by display order.")
	public ApiResponse<List<ProblemSolvingProfileResponse>> listPublished() {
		return ApiResponse.of(profileService.listPublished());
	}

	@GetMapping("/admin/problem-solving")
	@Operation(summary = "List profiles", description = "All statuses, optional ?status= filter.")
	public ApiResponse<List<ProblemSolvingProfileResponse>> list(
			@RequestParam(required = false) ContentStatus status) {
		return ApiResponse.of(profileService.listForAdmin(status));
	}

	@PostMapping("/admin/problem-solving")
	@Operation(summary = "Add a profile", description = "One handle per platform.")
	public ResponseEntity<ApiResponse<ProblemSolvingProfileResponse>> create(
			@Valid @RequestBody ProblemSolvingProfileCreateRequest request) {
		ProblemSolvingProfileResponse created = profileService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created, "Profile added"));
	}

	@GetMapping("/admin/problem-solving/{id}")
	@Operation(summary = "Get a profile", description = "Any status.")
	public ApiResponse<ProblemSolvingProfileResponse> get(@PathVariable Long id) {
		return ApiResponse.of(profileService.getForAdmin(id));
	}

	@PutMapping("/admin/problem-solving/{id}")
	@Operation(summary = "Replace a profile", description = "Whole-row update.")
	public ApiResponse<ProblemSolvingProfileResponse> update(
			@PathVariable Long id, @Valid @RequestBody ProblemSolvingProfileUpdateRequest request) {
		return ApiResponse.of(profileService.update(id, request), "Profile updated");
	}

	@DeleteMapping("/admin/problem-solving/{id}")
	@Operation(summary = "Delete a profile", description = "Soft delete.")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		profileService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
