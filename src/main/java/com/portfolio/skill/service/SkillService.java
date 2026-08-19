package com.portfolio.skill.service;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.skill.dto.SkillCategoryResponse;
import com.portfolio.skill.dto.SkillCreateRequest;
import com.portfolio.skill.dto.SkillGroupResponse;
import com.portfolio.skill.dto.SkillResponse;
import com.portfolio.skill.dto.SkillUpdateRequest;
import java.util.List;

public interface SkillService {

	SkillResponse create(SkillCreateRequest request);

	SkillResponse update(Long id, SkillUpdateRequest request);

	void delete(Long id);

	SkillResponse getForAdmin(Long id);

	List<SkillResponse> listForAdmin(ContentStatus status);

	/** Published skills only, grouped by category — the shape the public Skills page renders. */
	List<SkillGroupResponse> listPublishedGrouped();

	/** The category lookup, for the admin's chip/dropdown. */
	List<SkillCategoryResponse> listCategories();
}
