package com.portfolio.blog.repository;

import com.portfolio.blog.entity.Article;
import com.portfolio.blog.entity.ArticleStatus;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * "Published" on the public side means PUBLISHED <em>and</em> already due: a SCHEDULED article
 * with a future {@code published_at} must stay invisible until the moment arrives, without anyone
 * running a job to flip it (Phase 7).
 */
public interface ArticleRepository extends JpaRepository<Article, Long> {

	@EntityGraph(attributePaths = {"tags", "category"})
	// LEFT JOIN, not the implicit `a.category.slug` path: an implicit join is an INNER join, which
	// would silently drop every uncategorised article from the public list.
	@Query("""
			select a from Article a
			left join a.category c
			where a.status = :published and a.publishedAt <= :now
			  and (:categorySlug is null or c.slug = :categorySlug)
			  and (:tagSlug is null or exists (select t from a.tags t where t.slug = :tagSlug))
			  and (:term = ''
			       or lower(a.title) like lower(concat('%', :term, '%'))
			       or lower(a.excerpt) like lower(concat('%', :term, '%')))
			order by a.publishedAt desc
			""")
	Page<Article> findPublic(
			@Param("published") ArticleStatus published,
			@Param("now") Instant now,
			@Param("categorySlug") String categorySlug,
			@Param("tagSlug") String tagSlug,
			@Param("term") String term,
			Pageable pageable);

	@EntityGraph(attributePaths = {"tags", "category"})
	@Query("""
			select a from Article a
			where a.slug = :slug and a.status = :published and a.publishedAt <= :now
			""")
	Optional<Article> findPublicBySlug(
			@Param("slug") String slug,
			@Param("published") ArticleStatus published,
			@Param("now") Instant now);

	@EntityGraph(attributePaths = {"tags", "category"})
	Page<Article> findByStatus(ArticleStatus status, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = {"tags", "category"})
	Page<Article> findAll(Pageable pageable);

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, Long id);
}
