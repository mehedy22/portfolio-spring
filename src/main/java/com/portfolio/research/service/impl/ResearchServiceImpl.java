package com.portfolio.research.service.impl;

import com.portfolio.blog.service.TagResolver;
import com.portfolio.common.content.ContentStatus;
import com.portfolio.common.exception.ConflictException;
import com.portfolio.common.exception.ResourceNotFoundException;
import com.portfolio.common.exception.ValidationException;
import com.portfolio.common.text.Slugs;
import com.portfolio.media.service.MediaReferenceResolver;
import com.portfolio.research.dto.ResearchCreateRequest;
import com.portfolio.research.dto.ResearchResponse;
import com.portfolio.research.dto.ResearchUpdateRequest;
import com.portfolio.research.entity.Research;
import com.portfolio.research.mapper.ResearchMapper;
import com.portfolio.research.repository.ResearchRepository;
import com.portfolio.research.service.ResearchService;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResearchServiceImpl implements ResearchService {

	private static final Logger log = LoggerFactory.getLogger(ResearchServiceImpl.class);

	private final ResearchRepository researchRepository;
	private final MediaReferenceResolver mediaReferenceResolver;
	private final TagResolver tagResolver;
	private final ResearchMapper researchMapper;

	public ResearchServiceImpl(
			ResearchRepository researchRepository,
			MediaReferenceResolver mediaReferenceResolver,
			TagResolver tagResolver,
			ResearchMapper researchMapper) {
		this.researchRepository = researchRepository;
		this.mediaReferenceResolver = mediaReferenceResolver;
		this.tagResolver = tagResolver;
		this.researchMapper = researchMapper;
	}

	@Override
	@Transactional
	public ResearchResponse create(ResearchCreateRequest request) {
		Research research = new Research();
		apply(research, Fields.of(request));
		Research saved = save(research);
		log.info("Research created: id={} slug={}", saved.getId(), saved.getSlug());
		return researchMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public ResearchResponse update(Long id, ResearchUpdateRequest request) {
		Research research = require(id);
		apply(research, Fields.of(request));
		return researchMapper.toResponse(save(research));
	}

	@Override
	@Transactional
	public void delete(Long id) {
		researchRepository.delete(require(id));
		log.info("Research deleted (soft): id={}", id);
	}

	@Override
	@Transactional(readOnly = true)
	public ResearchResponse getForAdmin(Long id) {
		return researchMapper.toResponse(require(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ResearchResponse> listForAdmin(ContentStatus status) {
		List<Research> found = status == null
				? researchRepository.findAllByOrderByPublicationDateDescIdDesc()
				: researchRepository.findByStatusOrderByPublicationDateDescIdDesc(status);
		return found.stream().map(researchMapper::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<ResearchResponse> listPublished(String tagSlug) {
		String tag = tagSlug == null || tagSlug.isBlank() ? null : tagSlug.trim();
		return researchRepository.findPublic(ContentStatus.PUBLISHED, tag).stream()
				.map(researchMapper::toResponse)
				.toList();
	}

	private void apply(Research research, Fields fields) {
		// An entry that links nowhere is not a research entry; one of the two routes out is required.
		boolean hasLink = fields.externalUrl() != null && !fields.externalUrl().isBlank();
		if (!hasLink && fields.pdfMediaId() == null) {
			throw new ValidationException("Provide an external URL, an uploaded PDF, or both");
		}

		research.setTitle(fields.title().trim());
		research.setSlug(resolveSlug(fields.slug(), fields.title(), research.getId()));
		research.setAbstractText(fields.abstractText().trim());
		research.setPublicationVenue(blankToNull(fields.publicationVenue()));
		research.setPublicationDate(fields.publicationDate());
		research.setExternalUrl(blankToNull(fields.externalUrl()));
		research.setPdf(mediaReferenceResolver.resolve(fields.pdfMediaId(), "pdfMediaId"));
		research.setDisplayOrder(fields.displayOrder() == null ? 0 : fields.displayOrder());
		research.setAiVisible(Boolean.TRUE.equals(fields.aiVisible()));
		if (fields.status() != null) {
			research.setStatus(fields.status());
		}
		research.replaceTags(tagResolver.resolve(fields.tags()));
	}

	private String resolveSlug(String requestedSlug, String title, Long researchId) {
		if (requestedSlug != null && !requestedSlug.isBlank()) {
			String slug = requestedSlug.trim();
			if (!Slugs.isValid(slug)) {
				throw new ValidationException(
						"Slug must be lowercase alphanumeric words separated by single hyphens");
			}
			if (isTaken(slug, researchId)) {
				throw new ConflictException("A research entry with slug '" + slug + "' already exists");
			}
			return slug;
		}
		String base = Slugs.from(title);
		if (base.isEmpty()) {
			throw new ValidationException("Could not derive a slug from the title; supply one explicitly");
		}
		String candidate = base;
		int suffix = 2;
		while (isTaken(candidate, researchId)) {
			candidate = base + "-" + suffix++;
		}
		return candidate;
	}

	private boolean isTaken(String slug, Long researchId) {
		return researchId == null
				? researchRepository.existsBySlug(slug)
				: researchRepository.existsBySlugAndIdNot(slug, researchId);
	}

	private Research save(Research research) {
		try {
			return researchRepository.saveAndFlush(research);
		}
		catch (DataIntegrityViolationException ex) {
			log.warn("Research write violated a constraint: {}", ex.getMostSpecificCause().getMessage());
			throw new ConflictException("A research entry with slug '" + research.getSlug() + "' already exists");
		}
	}

	private Research require(Long id) {
		return researchRepository
				.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Research " + id + " not found"));
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	/** Create and update carry identical fields; the write path exists once. */
	private record Fields(
			String title,
			String slug,
			String abstractText,
			String publicationVenue,
			LocalDate publicationDate,
			String externalUrl,
			Long pdfMediaId,
			List<String> tags,
			ContentStatus status,
			Integer displayOrder,
			Boolean aiVisible) {

		static Fields of(ResearchCreateRequest r) {
			return new Fields(r.title(), r.slug(), r.abstractText(), r.publicationVenue(),
					r.publicationDate(), r.externalUrl(), r.pdfMediaId(), r.tags(), r.status(),
					r.displayOrder(), r.aiVisible());
		}

		static Fields of(ResearchUpdateRequest r) {
			return new Fields(r.title(), r.slug(), r.abstractText(), r.publicationVenue(),
					r.publicationDate(), r.externalUrl(), r.pdfMediaId(), r.tags(), r.status(),
					r.displayOrder(), r.aiVisible());
		}
	}
}
