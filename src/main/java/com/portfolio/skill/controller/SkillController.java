package com.portfolio.skill.controller;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.common.response.ApiResponse;
import com.portfolio.skill.dto.SkillCategoryResponse;
import com.portfolio.skill.dto.SkillCreateRequest;
import com.portfolio.skill.dto.SkillGroupResponse;
import com.portfolio.skill.dto.SkillResponse;
import com.portfolio.skill.dto.SkillUpdateRequest;
import com.portfolio.skill.service.SkillService;
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

/** Skill endpoints per docs/07-api/endpoints.md. The public list is grouped by category. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Skills", description = "Skills, grouped by category (FR-05)")
public class SkillController {

	private final SkillService skillService;

	public SkillController(SkillService skillService) {
		this.skillService = skillService;
	}

	@GetMapping("/skills")
	@Operation(summary = "List published skills", description = "Grouped by category, each in display order.")
	public ApiResponse<List<SkillGroupResponse>> listPublished() {
		return ApiResponse.of(skillService.listPublishedGrouped());
	}

	@GetMapping("/admin/skills")
	@Operation(summary = "List skills", description = "Flat, all statuses, optional ?status= filter.")
	public ApiResponse<List<SkillResponse>> list(@RequestParam(required = false) ContentStatus status) {
		return ApiResponse.of(skillService.listForAdmin(status));
	}

	@PostMapping("/admin/skills")
	@Operation(summary = "Create a skill", description = "The category is created if it does not exist.")
	public ResponseEntity<ApiResponse<SkillResponse>> create(@Valid @RequestBody SkillCreateRequest request) {
		SkillResponse created = skillService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created, "Skill created"));
	}

	@GetMapping("/admin/skills/{id}")
	@Operation(summary = "Get a skill", description = "Any status.")
	public ApiResponse<SkillResponse> get(@PathVariable Long id) {
		return ApiResponse.of(skillService.getForAdmin(id));
	}

	@PutMapping("/admin/skills/{id}")
	@Operation(summary = "Replace a skill", description = "Whole-row update.")
	public ApiResponse<SkillResponse> update(
			@PathVariable Long id, @Valid @RequestBody SkillUpdateRequest request) {
		return ApiResponse.of(skillService.update(id, request), "Skill updated");
	}

	@DeleteMapping("/admin/skills/{id}")
	@Operation(summary = "Delete a skill", description = "Soft delete.")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		skillService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/admin/skill-categories")
	@Operation(summary = "List skill categories", description = "The lookup, alphabetical.")
	public ApiResponse<List<SkillCategoryResponse>> listCategories() {
		return ApiResponse.of(skillService.listCategories());
	}
}
