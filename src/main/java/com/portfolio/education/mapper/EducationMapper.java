package com.portfolio.education.mapper;

import com.portfolio.education.dto.EducationResponse;
import com.portfolio.education.entity.Education;
import com.portfolio.media.mapper.MediaMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = MediaMapper.class)
public interface EducationMapper {

	EducationResponse toResponse(Education education);
}
