package com.portfolio.skill.repository;

import com.portfolio.skill.entity.SkillCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillCategoryRepository extends JpaRepository<SkillCategory, Long> {

	/** Matches {@code uq_skill_category_name_lower}, so "Backend" and "backend" are one group. */
	Optional<SkillCategory> findByNameIgnoreCase(String name);

	List<SkillCategory> findAllByOrderByNameAsc();
}
