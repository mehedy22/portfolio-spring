package com.portfolio.project.repository;

import com.portfolio.project.entity.Project;
import com.portfolio.common.content.ContentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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

	@EntityGraph(attributePaths = "technologies")
	Page<Project> findByStatus(ContentStatus status, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = "technologies")
	Page<Project> findAll(Pageable pageable);

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, Long id);
}
