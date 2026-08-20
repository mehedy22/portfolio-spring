package com.portfolio.problemsolving.service.impl;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.common.exception.ConflictException;
import com.portfolio.common.exception.ResourceNotFoundException;
import com.portfolio.problemsolving.dto.ProblemSolvingProfileCreateRequest;
import com.portfolio.problemsolving.dto.ProblemSolvingProfileResponse;
import com.portfolio.problemsolving.dto.ProblemSolvingProfileUpdateRequest;
import com.portfolio.problemsolving.entity.ProblemSolvingProfile;
import com.portfolio.problemsolving.mapper.ProblemSolvingProfileMapper;
import com.portfolio.problemsolving.repository.ProblemSolvingProfileRepository;
import com.portfolio.problemsolving.service.ProblemSolvingProfileService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProblemSolvingProfileServiceImpl implements ProblemSolvingProfileService {

	private static final Logger log = LoggerFactory.getLogger(ProblemSolvingProfileServiceImpl.class);

	private final ProblemSolvingProfileRepository profileRepository;
	private final ProblemSolvingProfileMapper profileMapper;

	public ProblemSolvingProfileServiceImpl(
			ProblemSolvingProfileRepository profileRepository,
			ProblemSolvingProfileMapper profileMapper) {
		this.profileRepository = profileRepository;
		this.profileMapper = profileMapper;
	}

	@Override
	@Transactional
	public ProblemSolvingProfileResponse create(ProblemSolvingProfileCreateRequest request) {
		ProblemSolvingProfile profile = new ProblemSolvingProfile();
		apply(profile, Fields.of(request), null);
		ProblemSolvingProfile saved = profileRepository.save(profile);
		log.info("Problem-solving profile created: id={} platform={}", saved.getId(), saved.getPlatform());
		return profileMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public ProblemSolvingProfileResponse update(Long id, ProblemSolvingProfileUpdateRequest request) {
		ProblemSolvingProfile profile = require(id);
		apply(profile, Fields.of(request), id);
		return profileMapper.toResponse(profileRepository.save(profile));
	}

	@Override
	@Transactional
	public void delete(Long id) {
		profileRepository.delete(require(id));
		log.info("Problem-solving profile deleted (soft): id={}", id);
	}

	@Override
	@Transactional(readOnly = true)
	public ProblemSolvingProfileResponse getForAdmin(Long id) {
		return profileMapper.toResponse(require(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProblemSolvingProfileResponse> listForAdmin(ContentStatus status) {
		List<ProblemSolvingProfile> found = status == null
				? profileRepository.findAllByOrderByDisplayOrderAscIdAsc()
				: profileRepository.findByStatusOrderByDisplayOrderAscIdAsc(status);
		return found.stream().map(profileMapper::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProblemSolvingProfileResponse> listPublished() {
		return profileRepository.findByStatusOrderByDisplayOrderAscIdAsc(ContentStatus.PUBLISHED).stream()
				.map(profileMapper::toResponse)
				.toList();
	}

	private void apply(ProblemSolvingProfile profile, Fields fields, Long id) {
		String platform = fields.platform().trim();
		String handle = fields.handle().trim();

		// The unique index is the guarantee; this is the message that makes the rejection
		// actionable rather than a 500 from a constraint the admin cannot see.
		boolean duplicate = id == null
				? profileRepository.existsByPlatformIgnoreCaseAndHandleIgnoreCase(platform, handle)
				: profileRepository.existsByPlatformIgnoreCaseAndHandleIgnoreCaseAndIdNot(platform, handle, id);
		if (duplicate) {
			throw new ConflictException("A %s profile for '%s' already exists".formatted(platform, handle));
		}

		profile.setPlatform(platform);
		profile.setHandle(handle);
		profile.setProfileUrl(blankToNull(fields.profileUrl()));
		profile.setProblemsSolved(fields.problemsSolved());
		profile.setRating(fields.rating());
		profile.setRankTitle(blankToNull(fields.rankTitle()));
		profile.setDisplayOrder(fields.displayOrder() == null ? 0 : fields.displayOrder());
		profile.setAiVisible(Boolean.TRUE.equals(fields.aiVisible()));
		if (fields.status() != null) {
			profile.setStatus(fields.status());
		}
	}

	private ProblemSolvingProfile require(Long id) {
		return profileRepository
				.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Problem-solving profile " + id + " not found"));
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private record Fields(
			String platform,
			String handle,
			String profileUrl,
			Integer problemsSolved,
			Integer rating,
			String rankTitle,
			Integer displayOrder,
			ContentStatus status,
			Boolean aiVisible) {

		static Fields of(ProblemSolvingProfileCreateRequest r) {
			return new Fields(r.platform(), r.handle(), r.profileUrl(), r.problemsSolved(), r.rating(),
					r.rankTitle(), r.displayOrder(), r.status(), r.aiVisible());
		}

		static Fields of(ProblemSolvingProfileUpdateRequest r) {
			return new Fields(r.platform(), r.handle(), r.profileUrl(), r.problemsSolved(), r.rating(),
					r.rankTitle(), r.displayOrder(), r.status(), r.aiVisible());
		}
	}
}
