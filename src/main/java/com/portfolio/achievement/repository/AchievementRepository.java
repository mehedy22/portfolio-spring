package com.portfolio.achievement.repository;

import com.portfolio.achievement.entity.Achievement;
import com.portfolio.common.content.ContentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {

	List<Achievement> findByStatusOrderByDisplayOrderAscIdDesc(ContentStatus status);

	List<Achievement> findAllByOrderByDisplayOrderAscIdDesc();
}
