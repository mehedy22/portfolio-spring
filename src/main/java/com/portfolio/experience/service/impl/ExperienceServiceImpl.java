package com.portfolio.experience.service.impl;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.common.exception.ResourceNotFoundException;
import com.portfolio.common.exception.ValidationException;
import com.portfolio.experience.dto.ExperienceCreateRequest;
import com.portfolio.experience.dto.ExperienceResponse;
import com.portfolio.experience.dto.ExperienceUpdateRequest;
import com.portfolio.experience.entity.EmploymentType;
import com.portfolio.experience.entity.Experience;
import com.portfolio.experience.mapper.ExperienceMapper;
import com.portfolio.experience.repository.ExperienceRepository;
import com.portfolio.experience.service.ExperienceService;
import com.portfolio.media.service.MediaReferenceResolver;
import com.portfolio.technology.service.TechnologyService;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExperienceServiceImpl implements ExperienceService {

	private static final Logger log = LoggerFactory.getLogger(ExperienceServiceImpl.class);

	private final ExperienceRepository experienceRepository;
	private final TechnologyService technologyService;
	private final MediaReferenceResolver mediaReferenceResolver;
	private final ExperienceMapper experienceMapper;

	public ExperienceServiceImpl(
			ExperienceRepository experienceRepository,
			TechnologyService technologyService,
			MediaReferenceResolver mediaReferenceResolver,
			ExperienceMapper experienceMapper) {
		this.experienceRepository = experienceRepository;
		this.technologyService = technologyService;
		this.mediaReferenceResolver = mediaReferenceResolver;
		this.experienceMapper = experienceMapper;
	}

	@Override
	@Transactional
	public ExperienceResponse create(ExperienceCreateRequest request) {
		Experience experience = new Experience();
		apply(experience, Fields.of(request));
		Experience saved = experienceRepository.save(experience);
		log.info("Experience created: id={} company={}", saved.getId(), saved.getCompany());
		return experienceMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public ExperienceResponse update(Long id, ExperienceUpdateRequest request) {
		Experience experience = require(id);
		apply(experience, Fields.of(request));
		return experienceMapper.toResponse(experienceRepository.save(experience));
	}

	@Override
	@Transactional
	public void delete(Long id) {
		experienceRepository.delete(require(id));
		log.info("Experience deleted (soft): id={}", id);
	}

	@Override
	@Transactional(readOnly = true)
	public ExperienceResponse getForAdmin(Long id) {
		return experienceMapper.toResponse(require(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ExperienceResponse> listForAdmin(ContentStatus status) {
		List<Experience> found = status == null
				? experienceRepository.findAllByOrderByDisplayOrderAscIdDesc()
				: experienceRepository.findByStatusOrderByDisplayOrderAscIdDesc(status);
		return found.stream().map(experienceMapper::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<ExperienceResponse> listPublished() {
		return experienceRepository.findByStatusOrderByDisplayOrderAscIdDesc(ContentStatus.PUBLISHED).stream()
				.map(experienceMapper::toResponse)
				.toList();
	}

	private void apply(Experience experience, Fields fields) {
		checkDates(fields);

		experience.setCompany(fields.company().trim());
		experience.setPosition(fields.position().trim());
		experience.setEmploymentType(fields.employmentType());
		experience.setDescription(fields.description());
		experience.setResponsibilities(fields.responsibilities());
		experience.setStartDate(fields.startDate());
		experience.setEndDate(fields.endDate());
		experience.setCurrentlyWorking(Boolean.TRUE.equals(fields.currentlyWorking()));
		experience.setCompanyLogo(
				mediaReferenceResolver.resolve(fields.companyLogoMediaId(), "companyLogoMediaId"));
		experience.setDisplayOrder(fields.displayOrder() == null ? 0 : fields.displayOrder());
		experience.setAiVisible(Boolean.TRUE.equals(fields.aiVisible()));
		if (fields.status() != null) {
			experience.setStatus(fields.status());
		}
		experience.replaceTechnologies(technologyService.resolveOrCreate(fields.technologies()));
	}

	/**
	 * Mirrors {@code ck_experience_dates} and {@code ck_experience_currently_working}. The CHECKs
	 * are the guarantee; these are the messages that make a rejection actionable instead of a 500.
	 */
	private void checkDates(Fields fields) {
		if (fields.endDate() != null && fields.endDate().isBefore(fields.startDate())) {
			throw new ValidationException("End date must not be before the start date");
		}
		if (Boolean.TRUE.equals(fields.currentlyWorking()) && fields.endDate() != null) {
			throw new ValidationException("A role marked as current cannot also have an end date");
		}
	}

	private Experience require(Long id) {
		return experienceRepository
				.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Experience " + id + " not found"));
	}

	/** Create and update carry identical fields; this keeps the write path written once. */
	private record Fields(
			String company,
			String position,
			EmploymentType employmentType,
			String description,
			String responsibilities,
			LocalDate startDate,
			LocalDate endDate,
			Boolean currentlyWorking,
			Long companyLogoMediaId,
			Integer displayOrder,
			ContentStatus status,
			Boolean aiVisible,
			List<String> technologies) {

		static Fields of(ExperienceCreateRequest r) {
			return new Fields(
					r.company(), r.position(), r.employmentType(), r.description(), r.responsibilities(),
					r.startDate(), r.endDate(), r.currentlyWorking(), r.companyLogoMediaId(),
					r.displayOrder(), r.status(), r.aiVisible(), r.technologies());
		}

		static Fields of(ExperienceUpdateRequest r) {
			return new Fields(
					r.company(), r.position(), r.employmentType(), r.description(), r.responsibilities(),
					r.startDate(), r.endDate(), r.currentlyWorking(), r.companyLogoMediaId(),
					r.displayOrder(), r.status(), r.aiVisible(), r.technologies());
		}
	}
}
