package com.portfolio.achievement.mapper;

import com.portfolio.achievement.dto.AchievementResponse;
import com.portfolio.achievement.entity.Achievement;
import com.portfolio.media.mapper.MediaMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = MediaMapper.class)
public interface AchievementMapper {

	AchievementResponse toResponse(Achievement achievement);
}
