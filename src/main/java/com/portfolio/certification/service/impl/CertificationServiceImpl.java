package com.portfolio.certification.service.impl;

import com.portfolio.certification.dto.CertificationCreateRequest;
import com.portfolio.certification.dto.CertificationResponse;
import com.portfolio.certification.dto.CertificationUpdateRequest;
import com.portfolio.certification.entity.Certification;
import com.portfolio.certification.mapper.CertificationMapper;
import com.portfolio.certification.repository.CertificationRepository;
import com.portfolio.certification.service.CertificationService;
import com.portfolio.common.content.ContentStatus;
import com.portfolio.common.exception.ResourceNotFoundException;
import com.portfolio.common.exception.ValidationException;
import com.portfolio.media.service.MediaReferenceResolver;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CertificationServiceImpl implements CertificationService {

	private static final Logger log = LoggerFactory.getLogger(CertificationServiceImpl.class);

	private final CertificationRepository certificationRepository;
	private final MediaReferenceResolver mediaReferenceResolver;
	private final CertificationMapper certificationMapper;

	public CertificationServiceImpl(
			CertificationRepository certificationRepository,
			MediaReferenceResolver mediaReferenceResolver,
			CertificationMapper certificationMapper) {
		this.certificationRepository = certificationRepository;
		this.mediaReferenceResolver = mediaReferenceResolver;
		this.certificationMapper = certificationMapper;
	}

	@Override
	@Transactional
	public CertificationResponse create(CertificationCreateRequest request) {
		Certification certification = new Certification();
		apply(certification, Fields.of(request));
		Certification saved = certificationRepository.save(certification);
		log.info("Certification created: id={} name={}", saved.getId(), saved.getName());
		return certificationMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public CertificationResponse update(Long id, CertificationUpdateRequest request) {
		Certification certification = require(id);
		apply(certification, Fields.of(request));
		return certificationMapper.toResponse(certificationRepository.save(certification));
	}

	@Override
	@Transactional
	public void delete(Long id) {
		certificationRepository.delete(require(id));
		log.info("Certification deleted (soft): id={}", id);
	}

	@Override
	@Transactional(readOnly = true)
	public CertificationResponse getForAdmin(Long id) {
		return certificationMapper.toResponse(require(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<CertificationResponse> listForAdmin(ContentStatus status) {
		List<Certification> found = status == null
				? certificationRepository.findAllByOrderByDisplayOrderAscIdDesc()
				: certificationRepository.findByStatusOrderByDisplayOrderAscIdDesc(status);
		return found.stream().map(certificationMapper::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<CertificationResponse> listPublished() {
		return certificationRepository.findByStatusOrderByDisplayOrderAscIdDesc(ContentStatus.PUBLISHED).stream()
				.map(certificationMapper::toResponse)
				.toList();
	}

	private void apply(Certification certification, Fields fields) {
		// Mirrors ck_certification_dates: a certificate cannot expire before it was issued.
		if (fields.issueDate() != null
				&& fields.expiryDate() != null
				&& fields.expiryDate().isBefore(fields.issueDate())) {
			throw new ValidationException("Expiry date must not be before the issue date");
		}

		certification.setName(fields.name().trim());
		certification.setIssuer(fields.issuer().trim());
		certification.setCredentialId(fields.credentialId());
		certification.setCredentialUrl(fields.credentialUrl());
		certification.setIssueDate(fields.issueDate());
		certification.setExpiryDate(fields.expiryDate());
		certification.setDescription(fields.description());
		certification.setCertificateImage(
				mediaReferenceResolver.resolve(fields.certificateImageMediaId(), "certificateImageMediaId"));
		certification.setDisplayOrder(fields.displayOrder() == null ? 0 : fields.displayOrder());
		certification.setAiVisible(Boolean.TRUE.equals(fields.aiVisible()));
		if (fields.status() != null) {
			certification.setStatus(fields.status());
		}
	}

	private Certification require(Long id) {
		return certificationRepository
				.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Certification " + id + " not found"));
	}

	private record Fields(
			String name,
			String issuer,
			String credentialId,
			String credentialUrl,
			LocalDate issueDate,
			LocalDate expiryDate,
			String description,
			Long certificateImageMediaId,
			Integer displayOrder,
			ContentStatus status,
			Boolean aiVisible) {

		static Fields of(CertificationCreateRequest r) {
			return new Fields(
					r.name(), r.issuer(), r.credentialId(), r.credentialUrl(), r.issueDate(), r.expiryDate(),
					r.description(), r.certificateImageMediaId(), r.displayOrder(), r.status(), r.aiVisible());
		}

		static Fields of(CertificationUpdateRequest r) {
			return new Fields(
					r.name(), r.issuer(), r.credentialId(), r.credentialUrl(), r.issueDate(), r.expiryDate(),
					r.description(), r.certificateImageMediaId(), r.displayOrder(), r.status(), r.aiVisible());
		}
	}
}
