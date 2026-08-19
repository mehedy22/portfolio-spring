package com.portfolio.project.mapper;

import com.portfolio.media.dto.MediaResponse;
import com.portfolio.media.mapper.MediaMapper;
import com.portfolio.project.dto.ProjectChallengeResponse;
import com.portfolio.project.dto.ProjectResponse;
import com.portfolio.project.dto.ProjectSummaryResponse;
import com.portfolio.project.entity.Project;
import com.portfolio.project.entity.ProjectChallenge;
import com.portfolio.technology.entity.Technology;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * The gallery is passed in rather than mapped from the entity: gallery slots hold media ids, and
 * resolving them (dropping any whose media has since been deleted — D-019) is the service's job.
 */
@Mapper(componentModel = "spring", uses = MediaMapper.class)
public interface ProjectMapper {

	@Mapping(target = "gallery", source = "galleryMedia")
	@Mapping(target = "technologies", source = "project.technologies")
	@Mapping(target = "challenges", source = "project.challenges")
	ProjectResponse toResponse(Project project, List<MediaResponse> galleryMedia);

	ProjectSummaryResponse toSummary(Project project);

	ProjectChallengeResponse toChallengeResponse(ProjectChallenge challenge);

	/** Alphabetical so chip order is stable across requests rather than following insertion order. */
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
