package com.portfolio.blog.service;

import com.portfolio.blog.entity.Tag;
import com.portfolio.blog.repository.TagRepository;
import com.portfolio.common.text.Slugs;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolve-or-create for tags, shared by Blog and Research.
 *
 * <p>The two modules deliberately use one {@code tag} table (Phase 6), so a tag applied to an
 * article and to a paper is one row — which is the whole reason the table is not nested under
 * either module. Extracted here so there is one implementation of "resolve these names", rather
 * than a second copy that could drift on normalization or slugging.
 */
@Component
public class TagResolver {

	private final TagRepository tagRepository;

	public TagResolver(TagRepository tagRepository) {
		this.tagRepository = tagRepository;
	}

	@Transactional
	public Set<Tag> resolve(List<String> names) {
		if (names == null || names.isEmpty()) {
			return Set.of();
		}
		Set<String> seen = new LinkedHashSet<>();
		Set<Tag> resolved = new LinkedHashSet<>();

		for (String raw : names) {
			if (raw == null || raw.isBlank()) {
				continue;
			}
			String name = raw.trim().replaceAll("\\s+", " ");
			if (!seen.add(name.toLowerCase())) {
				continue;
			}
			resolved.add(tagRepository
					.findByNameIgnoreCase(name)
					.orElseGet(() -> tagRepository.save(new Tag(name, uniqueSlug(name)))));
		}
		return resolved;
	}

	/** Tag slugs are unique across the table; suffix rather than fail on a clash. */
	private String uniqueSlug(String name) {
		String base = Slugs.from(name);
		return base.isEmpty() ? "tag-" + System.nanoTime() : base;
	}
}
