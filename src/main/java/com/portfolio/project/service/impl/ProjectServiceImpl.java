package com.portfolio.project.service.impl;

import com.portfolio.common.exception.ConflictException;
import com.portfolio.common.exception.ResourceNotFoundException;
import com.portfolio.common.exception.ValidationException;
import com.portfolio.common.response.PageResponse;
import com.portfolio.common.text.Slugs;
import com.portfolio.media.dto.MediaResponse;
import com.portfolio.media.entity.Media;
import com.portfolio.media.mapper.MediaMapper;
import com.portfolio.media.repository.MediaRepository;
import com.portfolio.media.service.MediaReferenceResolver;
import com.portfolio.project.dto.ProjectChallengeRequest;
import com.portfolio.project.dto.ProjectCreateRequest;
import com.portfolio.project.dto.ProjectResponse;
import com.portfolio.project.dto.ProjectSummaryResponse;
import com.portfolio.project.dto.ProjectUpdateRequest;
import com.portfolio.project.entity.Project;
import com.portfolio.project.entity.ProjectChallenge;
import com.portfolio.project.entity.ProjectGalleryItem;
import com.portfolio.common.content.ContentStatus;
import com.portfolio.project.entity.ProjectType;
import com.portfolio.project.mapper.ProjectMapper;
import com.portfolio.project.repository.ProjectRepository;
import com.portfolio.project.service.ProjectService;
import com.portfolio.technology.service.TechnologyService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A project is treated as one aggregate: challenges, gallery and technologies are replaced
 * wholesale on every write, because they are part of the project rather than resources with their
 * own lifecycle (docs/07-api/endpoints.md).
 *
 * <p>Public reads go through {@link #listPublished()} / {@link #getPublishedBySlug(String)}, which
 * cannot express any other status — a draft is invisible by construction, not by a filter someone
 * has to remember to apply.
 */
@Service
public class ProjectServiceImpl implements ProjectService {

	private static final Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);

	private static final int MAX_PAGE_SIZE = 100;

	/** Sorting is whitelisted: a client-supplied property would otherwise reach the query planner. */
	private static final Map<String, String> SORTABLE = Map.of(
			"displayOrder", "displayOrder",
			"createdAt", "createdAt",
			"updatedAt", "updatedAt",
			"title", "title");

	private final ProjectRepository projectRepository;
	private final MediaRepository mediaRepository;
	private final MediaReferenceResolver mediaReferenceResolver;
	private final TechnologyService technologyService;
	private final ProjectMapper projectMapper;
	private final MediaMapper mediaMapper;

	public ProjectServiceImpl(
			ProjectRepository projectRepository,
			MediaRepository mediaRepository,
			MediaReferenceResolver mediaReferenceResolver,
			TechnologyService technologyService,
			ProjectMapper projectMapper,
			MediaMapper mediaMapper) {
		this.projectRepository = projectRepository;
		this.mediaRepository = mediaRepository;
		this.mediaReferenceResolver = mediaReferenceResolver;
		this.technologyService = technologyService;
		this.projectMapper = projectMapper;
		this.mediaMapper = mediaMapper;
	}

	// ---------------------------------------------------------------- write

	@Override
	@Transactional
	public ProjectResponse create(ProjectCreateRequest request) {
		Project project = new Project();
		apply(project, Fields.of(request));
		Project saved = save(project);
		log.info("Project created: id={} slug={} status=DRAFT", saved.getId(), saved.getSlug());
		return toResponse(saved);
	}

	@Override
	@Transactional
	public ProjectResponse update(Long id, ProjectUpdateRequest request) {
		Project project = require(id);
		apply(project, Fields.of(request));
		Project saved = save(project);
		log.info("Project updated: id={} slug={}", saved.getId(), saved.getSlug());
		return toResponse(saved);
	}

	@Override
	@Transactional
	public ProjectResponse updateStatus(Long id, ContentStatus status) {
		Project project = require(id);
		project.setStatus(status);
		Project saved = projectRepository.save(project);
		log.info("Project {} status changed to {}", id, status);
		return toResponse(saved);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		Project project = require(id);
		projectRepository.delete(project);
		log.info("Project deleted (soft): id={}", id);
	}

	// ----------------------------------------------------------------- read

	@Override
	@Transactional(readOnly = true)
	public ProjectResponse getForAdmin(Long id) {
		return toResponse(require(id));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ProjectSummaryResponse> listForAdmin(
			ContentStatus status, int page, int size, String sort) {

		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), sortOf(sort));
		Page<Project> found = status == null
				? projectRepository.findAll(pageable)
				: projectRepository.findByStatus(status, pageable);
		return PageResponse.from(found.map(projectMapper::toSummary));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProjectSummaryResponse> listPublished(String search) {
		String term = search == null ? null : search.trim();
		List<Project> found = term == null || term.isEmpty()
				? projectRepository.findByStatusOrderByDisplayOrderAscIdAsc(ContentStatus.PUBLISHED)
				: projectRepository.searchPublished(ContentStatus.PUBLISHED, term);
		return found.stream().map(projectMapper::toSummary).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ProjectResponse getPublishedBySlug(String slug) {
		Project project = projectRepository
				.findBySlugAndStatus(slug, ContentStatus.PUBLISHED)
				// Deliberately the same 404 a nonexistent slug gets: a draft's existence must not be
				// confirmable from the public API (docs/08-security/threat-model.md).
				.orElseThrow(() -> new ResourceNotFoundException("Project '" + slug + "' not found"));
		return toResponse(project);
	}

	// -------------------------------------------------------------- helpers

	private void apply(Project project, Fields fields) {
		checkDates(fields.startDate(), fields.endDate());

		project.setTitle(fields.title().trim());
		project.setSlug(resolveSlug(fields.slug(), fields.title(), project.getId()));
		project.setShortDescription(fields.shortDescription().trim());
		project.setDetailedDescription(fields.detailedDescription());
		project.setThumbnail(mediaReferenceResolver.resolve(fields.thumbnailMediaId(), "thumbnailMediaId"));
		project.setGithubUrl(fields.githubUrl());
		project.setLiveUrl(fields.liveUrl());
		project.setProjectType(fields.projectType());
		project.setStartDate(fields.startDate());
		project.setEndDate(fields.endDate());
		project.setFeatured(Boolean.TRUE.equals(fields.featured()));
		project.setDisplayOrder(fields.displayOrder() == null ? 0 : fields.displayOrder());
		project.setFeatures(fields.features());
		project.setAiVisible(Boolean.TRUE.equals(fields.aiVisible()));

		project.replaceTechnologies(technologyService.resolveOrCreate(fields.technologies()));
		project.replaceChallenges(buildChallenges(project, fields.challenges()));
		project.replaceGallery(buildGallery(project, fields.galleryMediaIds()));
	}

	/**
	 * Enforced here as well as by {@code ck_project_dates}: the CHECK is the guarantee, this is the
	 * message. Without it the violation would surface as a 500 instead of a 400.
	 */
	private void checkDates(LocalDate startDate, LocalDate endDate) {
		if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
			throw new ValidationException("End date must not be before the start date");
		}
	}

	/**
	 * An explicitly supplied slug must be canonical and free — colliding with another project is a
	 * 409, because the admin chose that exact value. A derived slug instead gets a numeric suffix:
	 * two projects can legitimately share a title, and failing the save would be unhelpful.
	 */
	private String resolveSlug(String requestedSlug, String title, Long projectId) {
		if (requestedSlug != null && !requestedSlug.isBlank()) {
			String slug = requestedSlug.trim();
			if (!Slugs.isValid(slug)) {
				throw new ValidationException(
						"Slug must be lowercase alphanumeric words separated by single hyphens");
			}
			if (isTaken(slug, projectId)) {
				throw new ConflictException("A project with slug '" + slug + "' already exists");
			}
			return slug;
		}

		String base = Slugs.from(title);
		if (base.isEmpty()) {
			throw new ValidationException("Could not derive a slug from the title; supply one explicitly");
		}
		String candidate = base;
		int suffix = 2;
		while (isTaken(candidate, projectId)) {
			candidate = base + "-" + suffix++;
		}
		return candidate;
	}

	private boolean isTaken(String slug, Long projectId) {
		return projectId == null
				? projectRepository.existsBySlug(slug)
				: projectRepository.existsBySlugAndIdNot(slug, projectId);
	}

	private List<ProjectChallenge> buildChallenges(Project project, List<ProjectChallengeRequest> requests) {
		if (requests == null || requests.isEmpty()) {
			return List.of();
		}
		List<ProjectChallenge> built = new ArrayList<>(requests.size());
		for (int order = 0; order < requests.size(); order++) {
			ProjectChallengeRequest request = requests.get(order);
			built.add(new ProjectChallenge(
					project, request.title().trim(), request.challenge(), request.solution(), order));
		}
		return built;
	}

	/**
	 * Ids are validated up front so a typo fails the whole write rather than silently dropping one
	 * image. Duplicates are collapsed — the join table's composite PK would reject them anyway.
	 */
	private List<ProjectGalleryItem> buildGallery(Project project, List<Long> mediaIds) {
		if (mediaIds == null || mediaIds.isEmpty()) {
			return List.of();
		}
		Set<Long> distinct = new LinkedHashSet<>(mediaIds.stream().filter(Objects::nonNull).toList());
		Set<Long> existing = mediaRepository.findAllById(distinct).stream()
				.map(Media::getId)
				.collect(Collectors.toSet());
		List<Long> missing = distinct.stream().filter(id -> !existing.contains(id)).toList();
		if (!missing.isEmpty()) {
			throw new ValidationException("Gallery media do not exist: " + missing);
		}

		List<ProjectGalleryItem> built = new ArrayList<>(distinct.size());
		int order = 0;
		for (Long mediaId : distinct) {
			built.add(new ProjectGalleryItem(project, mediaId, order++));
		}
		return built;
	}

	/** Persists, turning a slug race into the documented 409 rather than a 500. */
	private Project save(Project project) {
		try {
			return projectRepository.saveAndFlush(project);
		}
		catch (DataIntegrityViolationException ex) {
			log.warn("Project write violated a constraint: {}", ex.getMostSpecificCause().getMessage());
			throw new ConflictException("A project with slug '" + project.getSlug() + "' already exists");
		}
	}

	private Project require(Long id) {
		return projectRepository
				.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Project " + id + " not found"));
	}

	private ProjectResponse toResponse(Project project) {
		return projectMapper.toResponse(project, galleryOf(project));
	}

	/** Resolves gallery ids in one query; media deleted since the save simply drops out (D-019). */
	private List<MediaResponse> galleryOf(Project project) {
		List<Long> ids = project.getGallery().stream().map(ProjectGalleryItem::getMediaId).toList();
		if (ids.isEmpty()) {
			return List.of();
		}
		Map<Long, Media> byId = mediaRepository.findAllById(ids).stream()
				.collect(Collectors.toMap(Media::getId, Function.identity()));
		return project.getGallery().stream()
				.map(item -> byId.get(item.getMediaId()))
				.filter(Objects::nonNull)
				.map(mediaMapper::toResponse)
				.toList();
	}

	/** {@code ?sort=field,dir}; anything outside the whitelist is a 400, not a silent default. */
	private Sort sortOf(String sort) {
		Sort fallback = Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.desc("id"));
		if (sort == null || sort.isBlank()) {
			return fallback;
		}
		String[] parts = sort.split(",");
		String property = SORTABLE.get(parts[0].trim());
		if (property == null) {
			throw new ValidationException("Cannot sort by '" + parts[0].trim() + "'. Allowed: " + sortableNames());
		}
		boolean descending = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim());
		return Sort.by(descending ? Sort.Order.desc(property) : Sort.Order.asc(property), Sort.Order.desc("id"));
	}

	private String sortableNames() {
		return SORTABLE.keySet().stream().sorted().collect(Collectors.joining(", "));
	}

	/**
	 * Create and update carry identical fields but are separate DTOs by convention
	 * (docs/11-technical-design/backend-design.md). This collapses both into one shape so the write
	 * path exists once rather than twice.
	 */
	private record Fields(
			String title,
			String slug,
			String shortDescription,
			String detailedDescription,
			Long thumbnailMediaId,
			String githubUrl,
			String liveUrl,
			ProjectType projectType,
			LocalDate startDate,
			LocalDate endDate,
			Boolean featured,
			Integer displayOrder,
			String features,
			Boolean aiVisible,
			List<String> technologies,
			List<ProjectChallengeRequest> challenges,
			List<Long> galleryMediaIds) {

		static Fields of(ProjectCreateRequest r) {
			return new Fields(
					r.title(), r.slug(), r.shortDescription(), r.detailedDescription(), r.thumbnailMediaId(),
					r.githubUrl(), r.liveUrl(), r.projectType(), r.startDate(), r.endDate(), r.featured(),
					r.displayOrder(), r.features(), r.aiVisible(), r.technologies(), r.challenges(),
					r.galleryMediaIds());
		}

		static Fields of(ProjectUpdateRequest r) {
			return new Fields(
					r.title(), r.slug(), r.shortDescription(), r.detailedDescription(), r.thumbnailMediaId(),
					r.githubUrl(), r.liveUrl(), r.projectType(), r.startDate(), r.endDate(), r.featured(),
					r.displayOrder(), r.features(), r.aiVisible(), r.technologies(), r.challenges(),
					r.galleryMediaIds());
		}
	}
}
