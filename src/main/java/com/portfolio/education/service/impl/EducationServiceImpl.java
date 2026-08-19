package com.portfolio.education.service.impl;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.common.exception.ResourceNotFoundException;
import com.portfolio.common.exception.ValidationException;
import com.portfolio.education.dto.EducationCreateRequest;
import com.portfolio.education.dto.EducationResponse;
import com.portfolio.education.dto.EducationUpdateRequest;
import com.portfolio.education.entity.Education;
import com.portfolio.education.mapper.EducationMapper;
import com.portfolio.education.repository.EducationRepository;
import com.portfolio.education.service.EducationService;
import com.portfolio.media.service.MediaReferenceResolver;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EducationServiceImpl implements EducationService {

	private static final Logger log = LoggerFactory.getLogger(EducationServiceImpl.class);

	private final EducationRepository educationRepository;
	private final MediaReferenceResolver mediaReferenceResolver;
	private final EducationMapper educationMapper;

	public EducationServiceImpl(
			EducationRepository educationRepository,
			MediaReferenceResolver mediaReferenceResolver,
			EducationMapper educationMapper) {
		this.educationRepository = educationRepository;
		this.mediaReferenceResolver = mediaReferenceResolver;
		this.educationMapper = educationMapper;
	}

	@Override
	@Transactional
	public EducationResponse create(EducationCreateRequest request) {
		Education education = new Education();
		apply(education, Fields.of(request));
		Education saved = educationRepository.save(education);
		log.info("Education created: id={} institution={}", saved.getId(), saved.getInstitution());
		return educationMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public EducationResponse update(Long id, EducationUpdateRequest request) {
		Education education = require(id);
		apply(education, Fields.of(request));
		return educationMapper.toResponse(educationRepository.save(education));
	}

	@Override
	@Transactional
	public void delete(Long id) {
		educationRepository.delete(require(id));
		log.info("Education deleted (soft): id={}", id);
	}

	@Override
	@Transactional(readOnly = true)
	public EducationResponse getForAdmin(Long id) {
		return educationMapper.toResponse(require(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<EducationResponse> listForAdmin(ContentStatus status) {
		List<Education> found = status == null
				? educationRepository.findAllByOrderByDisplayOrderAscIdDesc()
				: educationRepository.findByStatusOrderByDisplayOrderAscIdDesc(status);
		return found.stream().map(educationMapper::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<EducationResponse> listPublished() {
		return educationRepository.findByStatusOrderByDisplayOrderAscIdDesc(ContentStatus.PUBLISHED).stream()
				.map(educationMapper::toResponse)
				.toList();
	}

	private void apply(Education education, Fields fields) {
		if (fields.startDate() != null && fields.endDate() != null && fields.endDate().isBefore(fields.startDate())) {
			throw new ValidationException("End date must not be before the start date");
		}

		education.setInstitution(fields.institution().trim());
		education.setDegree(fields.degree());
		education.setField(fields.field());
		education.setDescription(fields.description());
		education.setStartDate(fields.startDate());
		education.setEndDate(fields.endDate());
		education.setCurrentlyStudying(Boolean.TRUE.equals(fields.currentlyStudying()));
		education.setLogo(mediaReferenceResolver.resolve(fields.logoMediaId(), "logoMediaId"));
		education.setDisplayOrder(fields.displayOrder() == null ? 0 : fields.displayOrder());
		education.setAiVisible(Boolean.TRUE.equals(fields.aiVisible()));
		if (fields.status() != null) {
			education.setStatus(fields.status());
		}
	}

	private Education require(Long id) {
		return educationRepository
				.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Education " + id + " not found"));
	}

	private record Fields(
			String institution,
			String degree,
			String field,
			String description,
			LocalDate startDate,
			LocalDate endDate,
			Boolean currentlyStudying,
			Long logoMediaId,
			Integer displayOrder,
			ContentStatus status,
			Boolean aiVisible) {

		static Fields of(EducationCreateRequest r) {
			return new Fields(
					r.institution(), r.degree(), r.field(), r.description(), r.startDate(), r.endDate(),
					r.currentlyStudying(), r.logoMediaId(), r.displayOrder(), r.status(), r.aiVisible());
		}

		static Fields of(EducationUpdateRequest r) {
			return new Fields(
					r.institution(), r.degree(), r.field(), r.description(), r.startDate(), r.endDate(),
					r.currentlyStudying(), r.logoMediaId(), r.displayOrder(), r.status(), r.aiVisible());
		}
	}
}
