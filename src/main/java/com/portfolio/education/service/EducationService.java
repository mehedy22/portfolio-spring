package com.portfolio.education.service;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.education.dto.EducationCreateRequest;
import com.portfolio.education.dto.EducationResponse;
import com.portfolio.education.dto.EducationUpdateRequest;
import java.util.List;

public interface EducationService {

	EducationResponse create(EducationCreateRequest request);

	EducationResponse update(Long id, EducationUpdateRequest request);

	void delete(Long id);

	EducationResponse getForAdmin(Long id);

	List<EducationResponse> listForAdmin(ContentStatus status);

	/** Published rows only — no status can be requested here. */
	List<EducationResponse> listPublished();
}
