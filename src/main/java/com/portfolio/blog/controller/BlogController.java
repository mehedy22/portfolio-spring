package com.portfolio.blog.controller;

import com.portfolio.blog.dto.ArticleCreateRequest;
import com.portfolio.blog.dto.ArticleResponse;
import com.portfolio.blog.dto.ArticleSummaryResponse;
import com.portfolio.blog.dto.ArticleUpdateRequest;
import com.portfolio.blog.dto.TaxonomyResponse;
import com.portfolio.blog.entity.ArticleStatus;
import com.portfolio.blog.service.BlogService;
import com.portfolio.common.response.ApiResponse;
import com.portfolio.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
 * Blog endpoints per docs/07-api/endpoints.md. Articles paginate on both surfaces — the one list
 * in this system that can grow without bound (NFR-03).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Blog", description = "Articles, categories and tags (FR-13)")
public class BlogController {

	private static final int DEFAULT_PAGE_SIZE = 10;

	private final BlogService blogService;

	public BlogController(BlogService blogService) {
		this.blogService = blogService;
	}

	// --------------------------------------------------------------- public

	@GetMapping("/articles")
	@Operation(
			summary = "List published articles",
			description = "Newest first. A scheduled article appears on its own when its time passes.")
	public ApiResponse<PageResponse<ArticleSummaryResponse>> listPublic(
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String tag,
			@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {

		return ApiResponse.of(blogService.listPublic(category, tag, search, page, size));
	}

	@GetMapping("/articles/{slug}")
	@Operation(summary = "Get a published article", description = "404 when unknown, draft, or not yet due.")
	public ApiResponse<ArticleResponse> getPublic(@PathVariable String slug) {
		return ApiResponse.of(blogService.getPublicBySlug(slug));
	}

	@GetMapping("/articles/categories")
	@Operation(summary = "Categories", description = "For the public filter links.")
	public ApiResponse<List<TaxonomyResponse>> publicCategories() {
		return ApiResponse.of(blogService.categories());
	}

	@GetMapping("/articles/tags")
	@Operation(summary = "Tags", description = "For the public filter links.")
	public ApiResponse<List<TaxonomyResponse>> publicTags() {
		return ApiResponse.of(blogService.tags());
	}

	// ---------------------------------------------------------------- admin

	@GetMapping("/admin/articles")
	@Operation(summary = "List articles", description = "All statuses, paginated, optional ?status= filter.")
	public ApiResponse<PageResponse<ArticleSummaryResponse>> list(
			@RequestParam(required = false) ArticleStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		return ApiResponse.of(blogService.listForAdmin(status, page, size));
	}

	@PostMapping("/admin/articles")
	@Operation(summary = "Create an article", description = "Content is sanitized on write.")
	public ResponseEntity<ApiResponse<ArticleResponse>> create(
			@Valid @RequestBody ArticleCreateRequest request, Authentication authentication) {

		ArticleResponse created = blogService.create(request, (Long) authentication.getPrincipal());
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created, "Article created"));
	}

	@GetMapping("/admin/articles/{id}")
	@Operation(summary = "Get an article", description = "Any status.")
	public ApiResponse<ArticleResponse> get(@PathVariable Long id) {
		return ApiResponse.of(blogService.getForAdmin(id));
	}

	@PutMapping("/admin/articles/{id}")
	@Operation(summary = "Replace an article", description = "The tags sent become the complete set.")
	public ApiResponse<ArticleResponse> update(
			@PathVariable Long id, @Valid @RequestBody ArticleUpdateRequest request) {
		return ApiResponse.of(blogService.update(id, request), "Article updated");
	}

	@DeleteMapping("/admin/articles/{id}")
	@Operation(summary = "Delete an article", description = "Soft delete.")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		blogService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/admin/categories")
	@Operation(summary = "Categories", description = "Created implicitly by naming one on an article.")
	public ApiResponse<List<TaxonomyResponse>> categories() {
		return ApiResponse.of(blogService.categories());
	}

	@GetMapping("/admin/tags")
	@Operation(summary = "Tags", description = "Created implicitly by naming one on an article.")
	public ApiResponse<List<TaxonomyResponse>> tags() {
		return ApiResponse.of(blogService.tags());
	}
}
