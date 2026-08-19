package com.portfolio.skill.mapper;

import com.portfolio.skill.dto.SkillCategoryResponse;
import com.portfolio.skill.dto.SkillResponse;
import com.portfolio.skill.entity.Skill;
import com.portfolio.skill.entity.SkillCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SkillMapper {

	@Mapping(target = "category", source = "category.name")
	SkillResponse toResponse(Skill skill);

	SkillCategoryResponse toCategoryResponse(SkillCategory category);
}
