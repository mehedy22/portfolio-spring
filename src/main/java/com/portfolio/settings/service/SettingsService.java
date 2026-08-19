package com.portfolio.settings.service;

import com.portfolio.settings.SettingKey;
import com.portfolio.settings.dto.PublicSettingsResponse;
import com.portfolio.settings.dto.SettingsResponse;
import com.portfolio.settings.dto.SettingsUpdateRequest;
import com.portfolio.settings.dto.SiteProfileResponse;
import com.portfolio.settings.dto.SiteProfileUpdateRequest;
import com.portfolio.settings.dto.SocialLinkResponse;
import com.portfolio.settings.dto.SocialLinksUpdateRequest;
import java.util.List;

public interface SettingsService {

	/** Public-safe configuration for both frontends, in one call. */
	PublicSettingsResponse publicSettings();

	SiteProfileResponse profile();

	/** Every key in the group, defaults filled in. */
	SettingsResponse settingsFor(SettingKey.Group group);

	/** Upserts the keys sent; a null value resets that key to its registry default. */
	SettingsResponse update(SettingKey.Group group, SettingsUpdateRequest request);

	List<SocialLinkResponse> socialLinks();

	/** Whole-list replace, in the order sent. */
	List<SocialLinkResponse> replaceSocialLinks(SocialLinksUpdateRequest request);

	SiteProfileResponse updateProfile(SiteProfileUpdateRequest request);
}
