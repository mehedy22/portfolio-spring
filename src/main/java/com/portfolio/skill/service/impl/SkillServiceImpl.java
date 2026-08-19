package com.portfolio.skill.service.impl;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.common.exception.ResourceNotFoundException;
import com.portfolio.skill.dto.SkillCategoryResponse;
import com.portfolio.skill.dto.SkillCreateRequest;
import com.portfolio.skill.dto.SkillGroupResponse;
import com.portfolio.skill.dto.SkillResponse;
import com.portfolio.skill.dto.SkillUpdateRequest;
import com.portfolio.skill.entity.Proficiency;
import com.portfolio.skill.entity.Skill;
import com.portfolio.skill.entity.SkillCategory;
import com.portfolio.skill.mapper.SkillMapper;
import com.portfolio.skill.repository.SkillCategoryRepository;
import com.portfolio.skill.repository.SkillRepository;
import com.portfolio.skill.service.SkillService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillServiceImpl implements SkillService {

	private static final Logger log = LoggerFactory.getLogger(SkillServiceImpl.class);

	private final SkillRepository skillRepository;
	private final SkillCategoryRepository categoryRepository;
	private final SkillMapper skillMapper;

	public SkillServiceImpl(
			SkillRepository skillRepository,
			SkillCategoryRepository categoryRepository,
			SkillMapper skillMapper) {
		this.skillRepository = skillRepository;
		this.categoryRepository = categoryRepository;
		this.skillMapper = skillMapper;
	}

	@Override
	@Transactional
	public SkillResponse create(SkillCreateRequest request) {
		Skill skill = new Skill();
		apply(skill, Fields.of(request));
		Skill saved = skillRepository.save(skill);
		log.info("Skill created: id={} name={}", saved.getId(), saved.getName());
		return skillMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public SkillResponse update(Long id, SkillUpdateRequest request) {
		Skill skill = require(id);
		apply(skill, Fields.of(request));
		return skillMapper.toResponse(skillRepository.save(skill));
	}

	@Override
	@Transactional
	public void delete(Long id) {
		skillRepository.delete(require(id));
		log.info("Skill deleted (soft): id={}", id);
	}

	@Override
	@Transactional(readOnly = true)
	public SkillResponse getForAdmin(Long id) {
		return skillMapper.toResponse(require(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<SkillResponse> listForAdmin(ContentStatus status) {
		List<Skill> found = status == null
				? skillRepository.findAllByOrderByDisplayOrderAscIdDesc()
				: skillRepository.findByStatusOrderByDisplayOrderAscIdDesc(status);
		return found.stream().map(skillMapper::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<SkillGroupResponse> listPublishedGrouped() {
		// One query, grouped in memory: at portfolio scale this is a few dozen rows, and a
		// per-category query would be N+1 for no benefit.
		Map<String, List<SkillResponse>> byCategory = new LinkedHashMap<>();
		skillRepository.findByStatusOrderByDisplayOrderAscIdDesc(ContentStatus.PUBLISHED).stream()
				.map(skillMapper::toResponse)
				.forEach(skill -> byCategory
						.computeIfAbsent(skill.category(), key -> new ArrayList<>())
						.add(skill));

		return byCategory.entrySet().stream()
				.sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
				.map(entry -> new SkillGroupResponse(entry.getKey(), List.copyOf(entry.getValue())))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<SkillCategoryResponse> listCategories() {
		return categoryRepository.findAllByOrderByNameAsc().stream()
				.map(skillMapper::toCategoryResponse)
				.toList();
	}

	private void apply(Skill skill, Fields fields) {
		skill.setName(fields.name().trim());
		skill.setCategory(resolveCategory(fields.category()));
		skill.setProficiency(fields.proficiency());
		skill.setIcon(fields.icon());
		skill.setDisplayOrder(fields.displayOrder() == null ? 0 : fields.displayOrder());
		skill.setFeatured(Boolean.TRUE.equals(fields.featured()));
		skill.setAiVisible(Boolean.TRUE.equals(fields.aiVisible()));
		if (fields.status() != null) {
			skill.setStatus(fields.status());
		}
	}

	/** Resolve-or-create by name, matched case-insensitively (D-022, mirroring D-020). */
	private SkillCategory resolveCategory(String name) {
		String normalized = name.trim().replaceAll("\\s+", " ");
		return categoryRepository
				.findByNameIgnoreCase(normalized)
				.orElseGet(() -> categoryRepository.save(new SkillCategory(normalized)));
	}

	private Skill require(Long id) {
		return skillRepository
				.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Skill " + id + " not found"));
	}

	private record Fields(
			String name,
			String category,
			Proficiency proficiency,
			String icon,
			Integer displayOrder,
			Boolean featured,
			ContentStatus status,
			Boolean aiVisible) {

		static Fields of(SkillCreateRequest r) {
			return new Fields(
					r.name(), r.category(), r.proficiency(), r.icon(), r.displayOrder(), r.featured(),
					r.status(), r.aiVisible());
		}

		static Fields of(SkillUpdateRequest r) {
			return new Fields(
					r.name(), r.category(), r.proficiency(), r.icon(), r.displayOrder(), r.featured(),
					r.status(), r.aiVisible());
		}
	}
}
