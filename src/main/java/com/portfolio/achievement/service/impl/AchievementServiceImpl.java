package com.portfolio.achievement.service.impl;

import com.portfolio.achievement.dto.AchievementCreateRequest;
import com.portfolio.achievement.dto.AchievementResponse;
import com.portfolio.achievement.dto.AchievementUpdateRequest;
import com.portfolio.achievement.entity.Achievement;
import com.portfolio.achievement.mapper.AchievementMapper;
import com.portfolio.achievement.repository.AchievementRepository;
import com.portfolio.achievement.service.AchievementService;
import com.portfolio.common.content.ContentStatus;
import com.portfolio.common.exception.ResourceNotFoundException;
import com.portfolio.media.service.MediaReferenceResolver;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AchievementServiceImpl implements AchievementService {

	private static final Logger log = LoggerFactory.getLogger(AchievementServiceImpl.class);

	private final AchievementRepository achievementRepository;
	private final MediaReferenceResolver mediaReferenceResolver;
	private final AchievementMapper achievementMapper;

	public AchievementServiceImpl(
			AchievementRepository achievementRepository,
			MediaReferenceResolver mediaReferenceResolver,
			AchievementMapper achievementMapper) {
		this.achievementRepository = achievementRepository;
		this.mediaReferenceResolver = mediaReferenceResolver;
		this.achievementMapper = achievementMapper;
	}

	@Override
	@Transactional
	public AchievementResponse create(AchievementCreateRequest request) {
		Achievement achievement = new Achievement();
		apply(achievement, Fields.of(request));
		Achievement saved = achievementRepository.save(achievement);
		log.info("Achievement created: id={} title={}", saved.getId(), saved.getTitle());
		return achievementMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public AchievementResponse update(Long id, AchievementUpdateRequest request) {
		Achievement achievement = require(id);
		apply(achievement, Fields.of(request));
		return achievementMapper.toResponse(achievementRepository.save(achievement));
	}

	@Override
	@Transactional
	public void delete(Long id) {
		achievementRepository.delete(require(id));
		log.info("Achievement deleted (soft): id={}", id);
	}

	@Override
	@Transactional(readOnly = true)
	public AchievementResponse getForAdmin(Long id) {
		return achievementMapper.toResponse(require(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<AchievementResponse> listForAdmin(ContentStatus status) {
		List<Achievement> found = status == null
				? achievementRepository.findAllByOrderByDisplayOrderAscIdDesc()
				: achievementRepository.findByStatusOrderByDisplayOrderAscIdDesc(status);
		return found.stream().map(achievementMapper::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<AchievementResponse> listPublished() {
		return achievementRepository.findByStatusOrderByDisplayOrderAscIdDesc(ContentStatus.PUBLISHED).stream()
				.map(achievementMapper::toResponse)
				.toList();
	}

	private void apply(Achievement achievement, Fields fields) {
		achievement.setTitle(fields.title().trim());
		achievement.setDescription(fields.description());
		achievement.setAchievedOn(fields.achievedOn());
		achievement.setImage(mediaReferenceResolver.resolve(fields.imageMediaId(), "imageMediaId"));
		achievement.setDisplayOrder(fields.displayOrder() == null ? 0 : fields.displayOrder());
		achievement.setAiVisible(Boolean.TRUE.equals(fields.aiVisible()));
		if (fields.status() != null) {
			achievement.setStatus(fields.status());
		}
	}

	private Achievement require(Long id) {
		return achievementRepository
				.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Achievement " + id + " not found"));
	}

	private record Fields(
			String title,
			String description,
			LocalDate achievedOn,
			Long imageMediaId,
			Integer displayOrder,
			ContentStatus status,
			Boolean aiVisible) {

		static Fields of(AchievementCreateRequest r) {
			return new Fields(r.title(), r.description(), r.achievedOn(), r.imageMediaId(),
					r.displayOrder(), r.status(), r.aiVisible());
		}

		static Fields of(AchievementUpdateRequest r) {
			return new Fields(r.title(), r.description(), r.achievedOn(), r.imageMediaId(),
					r.displayOrder(), r.status(), r.aiVisible());
		}
	}
}
