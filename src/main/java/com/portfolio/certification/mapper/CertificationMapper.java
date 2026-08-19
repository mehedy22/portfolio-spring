package com.portfolio.certification.mapper;

import com.portfolio.certification.dto.CertificationResponse;
import com.portfolio.certification.entity.Certification;
import com.portfolio.media.mapper.MediaMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = MediaMapper.class)
public interface CertificationMapper {

	CertificationResponse toResponse(Certification certification);
}
