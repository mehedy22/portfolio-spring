package com.portfolio.blog.service;

import com.portfolio.blog.dto.ArticleCreateRequest;
import com.portfolio.blog.dto.ArticleResponse;
import com.portfolio.blog.dto.ArticleSummaryResponse;
import com.portfolio.blog.dto.ArticleUpdateRequest;
import com.portfolio.blog.dto.TaxonomyResponse;
import com.portfolio.blog.entity.ArticleStatus;
import com.portfolio.common.response.PageResponse;
import java.util.List;

public interface BlogService {

	ArticleResponse create(ArticleCreateRequest request, Long authorAdminId);

	ArticleResponse update(Long id, ArticleUpdateRequest request);

	void delete(Long id);

	ArticleResponse getForAdmin(Long id);

	PageResponse<ArticleSummaryResponse> listForAdmin(ArticleStatus status, int page, int size);

	/** Published and already due only — a scheduled article stays invisible until its time. */
	PageResponse<ArticleSummaryResponse> listPublic(
			String categorySlug, String tagSlug, String search, int page, int size);

	ArticleResponse getPublicBySlug(String slug);

	List<TaxonomyResponse> categories();

	List<TaxonomyResponse> tags();
}
