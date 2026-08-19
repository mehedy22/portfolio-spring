package com.portfolio.certification.service;

import com.portfolio.certification.dto.CertificationCreateRequest;
import com.portfolio.certification.dto.CertificationResponse;
import com.portfolio.certification.dto.CertificationUpdateRequest;
import com.portfolio.common.content.ContentStatus;
import java.util.List;

public interface CertificationService {

	CertificationResponse create(CertificationCreateRequest request);

	CertificationResponse update(Long id, CertificationUpdateRequest request);

	void delete(Long id);

	CertificationResponse getForAdmin(Long id);

	List<CertificationResponse> listForAdmin(ContentStatus status);

	/** Published rows only — no status can be requested here. */
	List<CertificationResponse> listPublished();
}
