package com.portfolio.experience.mapper;

import com.portfolio.experience.dto.ExperienceResponse;
import com.portfolio.experience.entity.Experience;
import com.portfolio.media.mapper.MediaMapper;
import com.portfolio.technology.entity.Technology;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = MediaMapper.class)
public interface ExperienceMapper {

	ExperienceResponse toResponse(Experience experience);

	/** Alphabetical, so chip order is stable across requests rather than insertion-dependent. */
	default List<String> toTechnologyNames(Set<Technology> technologies) {
		if (technologies == null) {
			return List.of();
		}
		return technologies.stream()
				.map(Technology::getName)
				.sorted(Comparator.comparing(name -> name.toLowerCase()))
				.toList();
	}
}
