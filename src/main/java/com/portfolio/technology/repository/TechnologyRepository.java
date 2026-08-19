package com.portfolio.technology.repository;

import com.portfolio.technology.entity.Technology;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnologyRepository extends JpaRepository<Technology, Long> {

	/** Matches the {@code uq_technology_name_lower} index, so "Redis" and "redis" are one row. */
	Optional<Technology> findByNameIgnoreCase(String name);
}
