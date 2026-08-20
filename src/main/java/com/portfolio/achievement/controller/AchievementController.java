package com.portfolio.achievement.controller;

import com.portfolio.achievement.dto.AchievementCreateRequest;
import com.portfolio.achievement.dto.AchievementResponse;
import com.portfolio.achievement.dto.AchievementUpdateRequest;
import com.portfolio.achievement.service.AchievementService;
import com.portfolio.common.content.ContentStatus;
import com.portfolio.common.response.ApiResponse;
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

/** Achievements per docs/07-api/endpoints.md — the same shape as the other content modules. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Achievements", description = "Awards and recognition (FR-12)")
public class AchievementController {

	private final AchievementService achievementService;

	public AchievementController(AchievementService achievementService) {
		this.achievementService = achievementService;
	}

	@GetMapping("/achievements")
	@Operation(summary = "List published achievements", description = "Ordered by display order.")
	public ApiResponse<List<AchievementResponse>> listPublished() {
		return ApiResponse.of(achievementService.listPublished());
	}

	@GetMapping("/admin/achievements")
	@Operation(summary = "List achievements", description = "All statuses, optional ?status= filter.")
	public ApiResponse<List<AchievementResponse>> list(@RequestParam(required = false) ContentStatus status) {
		return ApiResponse.of(achievementService.listForAdmin(status));
	}

	@PostMapping("/admin/achievements")
	@Operation(summary = "Create an achievement", description = "Defaults to PUBLISHED.")
	public ResponseEntity<ApiResponse<AchievementResponse>> create(
			@Valid @RequestBody AchievementCreateRequest request) {
		AchievementResponse created = achievementService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created, "Achievement created"));
	}

	@GetMapping("/admin/achievements/{id}")
	@Operation(summary = "Get an achievement", description = "Any status.")
	public ApiResponse<AchievementResponse> get(@PathVariable Long id) {
		return ApiResponse.of(achievementService.getForAdmin(id));
	}

	@PutMapping("/admin/achievements/{id}")
	@Operation(summary = "Replace an achievement", description = "Whole-row update.")
	public ApiResponse<AchievementResponse> update(
			@PathVariable Long id, @Valid @RequestBody AchievementUpdateRequest request) {
		return ApiResponse.of(achievementService.update(id, request), "Achievement updated");
	}

	@DeleteMapping("/admin/achievements/{id}")
	@Operation(summary = "Delete an achievement", description = "Soft delete.")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		achievementService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
