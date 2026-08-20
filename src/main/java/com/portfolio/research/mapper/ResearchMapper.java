package com.portfolio.research.mapper;

import com.portfolio.blog.entity.Tag;
import com.portfolio.media.mapper.MediaMapper;
import com.portfolio.research.dto.ResearchResponse;
import com.portfolio.research.entity.Research;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = MediaMapper.class)
public interface ResearchMapper {

	ResearchResponse toResponse(Research research);

	/** Alphabetical, so tag order is stable rather than insertion-dependent. */
	default List<String> toTagNames(Set<Tag> tags) {
		if (tags == null) {
			return List.of();
		}
		return tags.stream()
				.map(Tag::getName)
				.sorted(Comparator.comparing(name -> name.toLowerCase()))
				.toList();
	}
}
