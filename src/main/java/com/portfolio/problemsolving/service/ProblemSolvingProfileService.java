package com.portfolio.problemsolving.service;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.problemsolving.dto.ProblemSolvingProfileCreateRequest;
import com.portfolio.problemsolving.dto.ProblemSolvingProfileResponse;
import com.portfolio.problemsolving.dto.ProblemSolvingProfileUpdateRequest;
import java.util.List;

public interface ProblemSolvingProfileService {

	ProblemSolvingProfileResponse create(ProblemSolvingProfileCreateRequest request);

	ProblemSolvingProfileResponse update(Long id, ProblemSolvingProfileUpdateRequest request);

	void delete(Long id);

	ProblemSolvingProfileResponse getForAdmin(Long id);

	List<ProblemSolvingProfileResponse> listForAdmin(ContentStatus status);

	/** Published profiles only. */
	List<ProblemSolvingProfileResponse> listPublished();
}
