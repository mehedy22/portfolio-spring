package com.portfolio.research.service;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.research.dto.ResearchCreateRequest;
import com.portfolio.research.dto.ResearchResponse;
import com.portfolio.research.dto.ResearchUpdateRequest;
import java.util.List;

public interface ResearchService {

	ResearchResponse create(ResearchCreateRequest request);

	ResearchResponse update(Long id, ResearchUpdateRequest request);

	void delete(Long id);

	ResearchResponse getForAdmin(Long id);

	List<ResearchResponse> listForAdmin(ContentStatus status);

	/** Published entries only, newest publication first. */
	List<ResearchResponse> listPublished(String tagSlug);
}
