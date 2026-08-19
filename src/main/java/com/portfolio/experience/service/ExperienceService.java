package com.portfolio.experience.service;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.experience.dto.ExperienceCreateRequest;
import com.portfolio.experience.dto.ExperienceResponse;
import com.portfolio.experience.dto.ExperienceUpdateRequest;
import java.util.List;

public interface ExperienceService {

	ExperienceResponse create(ExperienceCreateRequest request);

	ExperienceResponse update(Long id, ExperienceUpdateRequest request);

	void delete(Long id);

	ExperienceResponse getForAdmin(Long id);

	/** All statuses, in display order; optionally filtered. */
	List<ExperienceResponse> listForAdmin(ContentStatus status);

	/** Published rows only — no status can be requested here. */
	List<ExperienceResponse> listPublished();
}
