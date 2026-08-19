package com.portfolio.skill.repository;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.skill.entity.Skill;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {

	List<Skill> findByStatusOrderByDisplayOrderAscIdDesc(ContentStatus status);

	List<Skill> findAllByOrderByDisplayOrderAscIdDesc();
}
