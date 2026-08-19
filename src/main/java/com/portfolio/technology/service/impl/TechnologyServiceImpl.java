package com.portfolio.technology.service.impl;

import com.portfolio.common.exception.ValidationException;
import com.portfolio.technology.entity.Technology;
import com.portfolio.technology.repository.TechnologyRepository;
import com.portfolio.technology.service.TechnologyService;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TechnologyServiceImpl implements TechnologyService {

	private static final int MAX_NAME_LENGTH = 100;

	private final TechnologyRepository technologyRepository;

	public TechnologyServiceImpl(TechnologyRepository technologyRepository) {
		this.technologyRepository = technologyRepository;
	}

	@Override
	@Transactional
	public Set<Technology> resolveOrCreate(Collection<String> names) {
		if (names == null || names.isEmpty()) {
			return Set.of();
		}
		Set<Technology> resolved = new LinkedHashSet<>();
		for (String name : normalize(names)) {
			resolved.add(technologyRepository
					.findByNameIgnoreCase(name)
					.orElseGet(() -> technologyRepository.save(new Technology(name))));
		}
		return resolved;
	}

	/** Trim, collapse internal whitespace, drop blanks, and de-duplicate case-insensitively. */
	private List<String> normalize(Collection<String> names) {
		Set<String> seen = new LinkedHashSet<>();
		List<String> cleaned = names.stream()
				.filter(name -> name != null && !name.isBlank())
				.map(name -> name.trim().replaceAll("\\s+", " "))
				.peek(this::checkLength)
				.filter(name -> seen.add(name.toLowerCase()))
				.toList();
		return cleaned;
	}

	private void checkLength(String name) {
		if (name.length() > MAX_NAME_LENGTH) {
			throw new ValidationException(
					"Technology name must be at most %d characters".formatted(MAX_NAME_LENGTH));
		}
	}
}
