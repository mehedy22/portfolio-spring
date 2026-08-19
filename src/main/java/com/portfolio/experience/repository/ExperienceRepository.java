package com.portfolio.experience.repository;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.experience.entity.Experience;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceRepository extends JpaRepository<Experience, Long> {

	@EntityGraph(attributePaths = "technologies")
	List<Experience> findByStatusOrderByDisplayOrderAscIdDesc(ContentStatus status);

	@EntityGraph(attributePaths = "technologies")
	List<Experience> findAllByOrderByDisplayOrderAscIdDesc();
}
