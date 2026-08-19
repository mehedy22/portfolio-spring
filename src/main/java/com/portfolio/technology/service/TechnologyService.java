package com.portfolio.technology.service;

import com.portfolio.technology.entity.Technology;
import java.util.Collection;
import java.util.Set;

public interface TechnologyService {

	/**
	 * Resolves each name to its existing row, creating one where it doesn't exist yet (D-020).
	 * Names are trimmed and whitespace-collapsed; matching is case-insensitive, so a project
	 * tagged "spring boot" joins the same row as one tagged "Spring Boot".
	 */
	Set<Technology> resolveOrCreate(Collection<String> names);
}
