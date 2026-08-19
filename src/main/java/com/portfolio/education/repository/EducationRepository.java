package com.portfolio.education.repository;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.education.entity.Education;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EducationRepository extends JpaRepository<Education, Long> {

	List<Education> findByStatusOrderByDisplayOrderAscIdDesc(ContentStatus status);

	List<Education> findAllByOrderByDisplayOrderAscIdDesc();
}
