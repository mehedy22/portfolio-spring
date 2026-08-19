package com.portfolio.certification.controller;

import com.portfolio.certification.dto.CertificationCreateRequest;
import com.portfolio.certification.dto.CertificationResponse;
import com.portfolio.certification.dto.CertificationUpdateRequest;
import com.portfolio.certification.service.CertificationService;
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

/**
 * Certification endpoints. The public path is {@code /api/v1/certifications} (plural), resolving
 * endpoints.md's {@code {resource}} placeholder against routes-and-layouts.md, which names the
 * plural explicitly — see D-022.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Certifications", description = "Certifications (FR-07)")
public class CertificationController {

	private final CertificationService certificationService;

	public CertificationController(CertificationService certificationService) {
		this.certificationService = certificationService;
	}

	@GetMapping("/certifications")
	@Operation(summary = "List published certifications", description = "Ordered by display order.")
	public ApiResponse<List<CertificationResponse>> listPublished() {
		return ApiResponse.of(certificationService.listPublished());
	}

	@GetMapping("/admin/certifications")
	@Operation(summary = "List certifications", description = "All statuses, optional ?status= filter.")
	public ApiResponse<List<CertificationResponse>> list(@RequestParam(required = false) ContentStatus status) {
		return ApiResponse.of(certificationService.listForAdmin(status));
	}

	@PostMapping("/admin/certifications")
	@Operation(summary = "Create a certification", description = "Defaults to PUBLISHED unless a status is sent.")
	public ResponseEntity<ApiResponse<CertificationResponse>> create(
			@Valid @RequestBody CertificationCreateRequest request) {
		CertificationResponse created = certificationService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created, "Certification created"));
	}

	@GetMapping("/admin/certifications/{id}")
	@Operation(summary = "Get a certification", description = "Any status.")
	public ApiResponse<CertificationResponse> get(@PathVariable Long id) {
		return ApiResponse.of(certificationService.getForAdmin(id));
	}

	@PutMapping("/admin/certifications/{id}")
	@Operation(summary = "Replace a certification", description = "Whole-row update.")
	public ApiResponse<CertificationResponse> update(
			@PathVariable Long id, @Valid @RequestBody CertificationUpdateRequest request) {
		return ApiResponse.of(certificationService.update(id, request), "Certification updated");
	}

	@DeleteMapping("/admin/certifications/{id}")
	@Operation(summary = "Delete a certification", description = "Soft delete.")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		certificationService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
