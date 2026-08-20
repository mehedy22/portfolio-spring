package com.portfolio.blog.service.impl;

import com.portfolio.blog.dto.ArticleCreateRequest;
import com.portfolio.blog.dto.ArticleResponse;
import com.portfolio.blog.dto.ArticleSummaryResponse;
import com.portfolio.blog.dto.ArticleUpdateRequest;
import com.portfolio.blog.dto.TaxonomyResponse;
import com.portfolio.blog.entity.Article;
import com.portfolio.blog.entity.ArticleStatus;
import com.portfolio.blog.entity.Category;
import com.portfolio.blog.entity.Tag;
import com.portfolio.blog.mapper.ArticleMapper;
import com.portfolio.blog.repository.ArticleRepository;
import com.portfolio.blog.repository.CategoryRepository;
import com.portfolio.blog.repository.TagRepository;
import com.portfolio.blog.service.BlogService;
import com.portfolio.blog.service.TagResolver;
import com.portfolio.common.exception.ConflictException;
import com.portfolio.common.exception.ResourceNotFoundException;
import com.portfolio.common.exception.ValidationException;
import com.portfolio.common.html.HtmlSanitizer;
import com.portfolio.common.response.PageResponse;
import com.portfolio.common.text.Slugs;
import com.portfolio.media.service.MediaReferenceResolver;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlogServiceImpl implements BlogService {

	private static final Logger log = LoggerFactory.getLogger(BlogServiceImpl.class);

	private static final int MAX_PAGE_SIZE = 100;
	/** Roughly the average adult reading pace; only ever used as an estimate. */
	private static final int WORDS_PER_MINUTE = 200;

	private final ArticleRepository articleRepository;
	private final CategoryRepository categoryRepository;
	private final TagRepository tagRepository;
	private final TagResolver tagResolver;
	private final MediaReferenceResolver mediaReferenceResolver;
	private final HtmlSanitizer htmlSanitizer;
	private final ArticleMapper articleMapper;

	public BlogServiceImpl(
			ArticleRepository articleRepository,
			CategoryRepository categoryRepository,
			TagRepository tagRepository,
			TagResolver tagResolver,
			MediaReferenceResolver mediaReferenceResolver,
			HtmlSanitizer htmlSanitizer,
			ArticleMapper articleMapper) {
		this.articleRepository = articleRepository;
		this.categoryRepository = categoryRepository;
		this.tagRepository = tagRepository;
		this.tagResolver = tagResolver;
		this.mediaReferenceResolver = mediaReferenceResolver;
		this.htmlSanitizer = htmlSanitizer;
		this.articleMapper = articleMapper;
	}

	@Override
	@Transactional
	public ArticleResponse create(ArticleCreateRequest request, Long authorAdminId) {
		Article article = new Article();
		article.setAuthorAdminId(authorAdminId);
		apply(article, Fields.of(request));
		Article saved = save(article);
		log.info("Article created: id={} slug={} status={}", saved.getId(), saved.getSlug(), saved.getStatus());
		return articleMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public ArticleResponse update(Long id, ArticleUpdateRequest request) {
		Article article = require(id);
		apply(article, Fields.of(request));
		return articleMapper.toResponse(save(article));
	}

	@Override
	@Transactional
	public void delete(Long id) {
		articleRepository.delete(require(id));
		log.info("Article deleted (soft): id={}", id);
	}

	@Override
	@Transactional(readOnly = true)
	public ArticleResponse getForAdmin(Long id) {
		return articleMapper.toResponse(require(id));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ArticleSummaryResponse> listForAdmin(ArticleStatus status, int page, int size) {
		Pageable pageable = PageRequest.of(
				Math.max(page, 0),
				Math.clamp(size, 1, MAX_PAGE_SIZE),
				Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id")));
		Page<Article> found = status == null
				? articleRepository.findAll(pageable)
				: articleRepository.findByStatus(status, pageable);
		return PageResponse.from(found.map(articleMapper::toSummary));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ArticleSummaryResponse> listPublic(
			String categorySlug, String tagSlug, String search, int page, int size) {

		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
		Page<Article> found = articleRepository.findPublic(
				ArticleStatus.PUBLISHED,
				Instant.now(),
				blankToNull(categorySlug),
				blankToNull(tagSlug),
				// Empty string, not null: see the repository note on parameter typing.
				search == null ? "" : search.trim(),
				pageable);
		return PageResponse.from(found.map(articleMapper::toSummary));
	}

	@Override
	@Transactional(readOnly = true)
	public ArticleResponse getPublicBySlug(String slug) {
		return articleRepository
				.findPublicBySlug(slug, ArticleStatus.PUBLISHED, Instant.now())
				// A draft, a scheduled article that is not due, and a nonexistent slug are all the
				// same 404 — the public API never confirms that something merely isn't ready yet.
				.map(articleMapper::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Article '" + slug + "' not found"));
	}

	@Override
	@Transactional(readOnly = true)
	public List<TaxonomyResponse> categories() {
		return categoryRepository.findAllByOrderByNameAsc().stream()
				.map(articleMapper::toTaxonomy)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<TaxonomyResponse> tags() {
		return tagRepository.findAllByOrderByNameAsc().stream()
				.map(articleMapper::toTaxonomy)
				.toList();
	}

	// -------------------------------------------------------------- helpers

	private void apply(Article article, Fields fields) {
		ArticleStatus status = fields.status() == null ? ArticleStatus.DRAFT : fields.status();
		Instant publishedAt = fields.publishedAt();

		if (status == ArticleStatus.PUBLISHED || status == ArticleStatus.SCHEDULED) {
			// Mirrors ck_article_published_at; publishing "now" is the obvious default rather than
			// an error the admin has to decode.
			publishedAt = publishedAt != null ? publishedAt : Instant.now();
		}
		if (status == ArticleStatus.SCHEDULED && !publishedAt.isAfter(Instant.now())) {
			throw new ValidationException("A scheduled article needs a publication time in the future");
		}

		String sanitized = htmlSanitizer.sanitize(fields.content());
		if (sanitized == null || sanitized.isBlank()) {
			throw new ValidationException("Content is empty once unsafe markup is removed");
		}

		article.setTitle(fields.title().trim());
		article.setSlug(resolveSlug(fields.slug(), fields.title(), article.getId()));
		article.setExcerpt(blankToNull(fields.excerpt()));
		article.setContent(sanitized);
		article.setThumbnail(mediaReferenceResolver.resolve(fields.thumbnailMediaId(), "thumbnailMediaId"));
		article.setOgImage(mediaReferenceResolver.resolve(fields.ogImageMediaId(), "ogImageMediaId"));
		article.setCategory(resolveCategory(fields.category()));
		article.setStatus(status);
		article.setPublishedAt(publishedAt);
		article.setReadingTimeMinutes(readingTime(sanitized));
		article.setSeoTitle(blankToNull(fields.seoTitle()));
		article.setSeoDescription(blankToNull(fields.seoDescription()));
		article.setAiVisible(Boolean.TRUE.equals(fields.aiVisible()));
		article.replaceTags(tagResolver.resolve(fields.tags()));
	}

	/** Estimated from the sanitized text, so markup is not counted as words. */
	private int readingTime(String html) {
		String text = html.replaceAll("<[^>]+>", " ");
		long words = text.split("\\s+").length;
		return Math.max(1, (int) Math.ceil(words / (double) WORDS_PER_MINUTE));
	}

	private String resolveSlug(String requestedSlug, String title, Long articleId) {
		if (requestedSlug != null && !requestedSlug.isBlank()) {
			String slug = requestedSlug.trim();
			if (!Slugs.isValid(slug)) {
				throw new ValidationException(
						"Slug must be lowercase alphanumeric words separated by single hyphens");
			}
			if (isTaken(slug, articleId)) {
				throw new ConflictException("An article with slug '" + slug + "' already exists");
			}
			return slug;
		}
		String base = Slugs.from(title);
		if (base.isEmpty()) {
			throw new ValidationException("Could not derive a slug from the title; supply one explicitly");
		}
		String candidate = base;
		int suffix = 2;
		while (isTaken(candidate, articleId)) {
			candidate = base + "-" + suffix++;
		}
		return candidate;
	}

	private boolean isTaken(String slug, Long articleId) {
		return articleId == null
				? articleRepository.existsBySlug(slug)
				: articleRepository.existsBySlugAndIdNot(slug, articleId);
	}

	/** Resolve-or-create by name, as for technologies and skill categories (D-020, D-022). */
	private Category resolveCategory(String name) {
		if (name == null || name.isBlank()) {
			return null;
		}
		String normalized = name.trim().replaceAll("\\s+", " ");
		return categoryRepository
				.findByNameIgnoreCase(normalized)
				.orElseGet(() -> categoryRepository.save(new Category(normalized, uniqueTaxonomySlug(normalized))));
	}


	/** Taxonomy slugs are unique across their own table; suffix rather than fail on a clash. */
	private String uniqueTaxonomySlug(String name) {
		String base = Slugs.from(name);
		return base.isEmpty() ? "item-" + System.nanoTime() : base;
	}

	private Article save(Article article) {
		try {
			return articleRepository.saveAndFlush(article);
		}
		catch (DataIntegrityViolationException ex) {
			log.warn("Article write violated a constraint: {}", ex.getMostSpecificCause().getMessage());
			throw new ConflictException("An article with slug '" + article.getSlug() + "' already exists");
		}
	}

	private Article require(Long id) {
		return articleRepository
				.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Article " + id + " not found"));
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	/** Create and update carry identical fields; the write path exists once. */
	private record Fields(
			String title,
			String slug,
			String excerpt,
			String content,
			Long thumbnailMediaId,
			Long ogImageMediaId,
			String category,
			List<String> tags,
			ArticleStatus status,
			Instant publishedAt,
			String seoTitle,
			String seoDescription,
			Boolean aiVisible) {

		static Fields of(ArticleCreateRequest r) {
			return new Fields(r.title(), r.slug(), r.excerpt(), r.content(), r.thumbnailMediaId(),
					r.ogImageMediaId(), r.category(), r.tags(), r.status(), r.publishedAt(),
					r.seoTitle(), r.seoDescription(), r.aiVisible());
		}

		static Fields of(ArticleUpdateRequest r) {
			return new Fields(r.title(), r.slug(), r.excerpt(), r.content(), r.thumbnailMediaId(),
					r.ogImageMediaId(), r.category(), r.tags(), r.status(), r.publishedAt(),
					r.seoTitle(), r.seoDescription(), r.aiVisible());
		}
	}
}
