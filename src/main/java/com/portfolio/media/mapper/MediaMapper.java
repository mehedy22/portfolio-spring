package com.portfolio.media.mapper;

import com.portfolio.media.dto.MediaResponse;
import com.portfolio.media.entity.Media;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MediaMapper {

	@Mapping(target = "url", expression = "java(MediaResponse.urlFor(media.getId()))")
	MediaResponse toResponse(Media media);
}
