package com.portfolio.achievement.service;

import com.portfolio.achievement.dto.AchievementCreateRequest;
import com.portfolio.achievement.dto.AchievementResponse;
import com.portfolio.achievement.dto.AchievementUpdateRequest;
import com.portfolio.common.content.ContentStatus;
import java.util.List;

public interface AchievementService {

	AchievementResponse create(AchievementCreateRequest request);

	AchievementResponse update(Long id, AchievementUpdateRequest request);

	void delete(Long id);

	AchievementResponse getForAdmin(Long id);

	List<AchievementResponse> listForAdmin(ContentStatus status);

	/** Published rows only. */
	List<AchievementResponse> listPublished();
}
