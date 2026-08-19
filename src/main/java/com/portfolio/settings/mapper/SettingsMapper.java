package com.portfolio.settings.mapper;

import com.portfolio.media.mapper.MediaMapper;
import com.portfolio.settings.dto.SiteProfileResponse;
import com.portfolio.settings.dto.SocialLinkResponse;
import com.portfolio.settings.entity.SiteProfile;
import com.portfolio.settings.entity.SocialLink;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = MediaMapper.class)
public interface SettingsMapper {

	SocialLinkResponse toResponse(SocialLink link);

	SiteProfileResponse toResponse(SiteProfile profile);
}
