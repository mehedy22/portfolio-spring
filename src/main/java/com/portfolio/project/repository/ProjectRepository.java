package com.portfolio.project.repository;

import com.portfolio.project.entity.Project;
import com.portfolio.common.content.ContentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Soft-deleted rows are excluded automatically by the entity's {@code @SQLRestriction}.
 *
 * <p>List queries fetch {@code technologies} eagerly: every list row renders its tech chips, so
 * without the graph a 20-row page costs 21 queries.
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

	@EntityGraph(attributePaths = "technologies")
	Optional<Project> findBySlugAndStatus(String slug, ContentStatus status);

	@EntityGraph(attributePaths = "technologies")
	List<Project> findByStatusOrderByDisplayOrderAscIdAsc(ContentStatus status);

	/**
	 * Public search (FR-18). Matches title, short description and technology name, case- and
	 * accent-insensitively enough for a portfolio: LOWER + LIKE is honest about what it is,
	 * whereas full-text search would be machinery this content volume cannot justify.
	 */
	@EntityGraph(attributePaths = "technologies")
	@Query("""
			select distinct p from Project p
			left join p.technologies t
			where p.status = :status
			  and (lower(p.title) like lower(concat('%', :term, '%'))
			       or lower(p.shortDescription) like lower(concat('%', :term, '%'))
			       or lower(t.name) like lower(concat('%', :term, '%')))
			order by p.displayOrder asc, p.id asc
			""")
	List<Project> searchPublished(@Param("status") ContentStatus status, @Param("term") String term);

	@EntityGraph(attributePaths = "technologies")
	Page<Project> findByStatus(ContentStatus status, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = "technologies")
	Page<Project> findAll(Pageable pageable);

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, Long id);
}
