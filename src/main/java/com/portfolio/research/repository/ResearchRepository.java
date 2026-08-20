package com.portfolio.research.repository;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.research.entity.Research;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResearchRepository extends JpaRepository<Research, Long> {

	@EntityGraph(attributePaths = "tags")
	@Query("""
			select r from Research r
			where r.status = :status
			  and (:tagSlug is null or exists (select t from r.tags t where t.slug = :tagSlug))
			order by r.publicationDate desc nulls last, r.displayOrder asc, r.id desc
			""")
	List<Research> findPublic(@Param("status") ContentStatus status, @Param("tagSlug") String tagSlug);

	@EntityGraph(attributePaths = "tags")
	List<Research> findByStatusOrderByPublicationDateDescIdDesc(ContentStatus status);

	@EntityGraph(attributePaths = "tags")
	List<Research> findAllByOrderByPublicationDateDescIdDesc();

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, Long id);
}
