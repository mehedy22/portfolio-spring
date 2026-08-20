package com.portfolio.problemsolving.mapper;

import com.portfolio.problemsolving.dto.ProblemSolvingProfileResponse;
import com.portfolio.problemsolving.entity.ProblemSolvingProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProblemSolvingProfileMapper {

	ProblemSolvingProfileResponse toResponse(ProblemSolvingProfile profile);
}
