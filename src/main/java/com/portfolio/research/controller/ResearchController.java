package com.portfolio.research.controller;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.common.response.ApiResponse;
import com.portfolio.research.dto.ResearchCreateRequest;
import com.portfolio.research.dto.ResearchResponse;
import com.portfolio.research.dto.ResearchUpdateRequest;
import com.portfolio.research.service.ResearchService;
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

/**
 * Research endpoints per docs/07-api/endpoints.md (D-014). There is no public detail route: an
 * entry links out to the paper, so the list item is the whole thing.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Research", description = "Papers and technical write-ups (D-014)")
public class ResearchController {

	private final ResearchService researchService;

	public ResearchController(ResearchService researchService) {
		this.researchService = researchService;
	}

	@GetMapping("/research")
	@Operation(summary = "List published research", description = "Newest publication first.")
	public ApiResponse<List<ResearchResponse>> listPublished(
			@RequestParam(required = false) String tag) {
		return ApiResponse.of(researchService.listPublished(tag));
	}

	@GetMapping("/admin/research")
	@Operation(summary = "List research", description = "All statuses, optional ?status= filter.")
	public ApiResponse<List<ResearchResponse>> list(@RequestParam(required = false) ContentStatus status) {
		return ApiResponse.of(researchService.listForAdmin(status));
	}

	@PostMapping("/admin/research")
	@Operation(summary = "Create a research entry", description = "Requires an external URL or a PDF.")
	public ResponseEntity<ApiResponse<ResearchResponse>> create(
			@Valid @RequestBody ResearchCreateRequest request) {
		ResearchResponse created = researchService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created, "Research created"));
	}

	@GetMapping("/admin/research/{id}")
	@Operation(summary = "Get a research entry", description = "Any status.")
	public ApiResponse<ResearchResponse> get(@PathVariable Long id) {
		return ApiResponse.of(researchService.getForAdmin(id));
	}

	@PutMapping("/admin/research/{id}")
	@Operation(summary = "Replace a research entry", description = "The tags sent become the complete set.")
	public ApiResponse<ResearchResponse> update(
			@PathVariable Long id, @Valid @RequestBody ResearchUpdateRequest request) {
		return ApiResponse.of(researchService.update(id, request), "Research updated");
	}

	@DeleteMapping("/admin/research/{id}")
	@Operation(summary = "Delete a research entry", description = "Soft delete.")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		researchService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
