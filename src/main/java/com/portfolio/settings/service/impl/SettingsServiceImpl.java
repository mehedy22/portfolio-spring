package com.portfolio.settings.service.impl;

import com.portfolio.common.exception.ValidationException;
import com.portfolio.media.service.MediaReferenceResolver;
import com.portfolio.settings.SettingKey;
import com.portfolio.settings.dto.PublicSettingsResponse;
import com.portfolio.settings.dto.SettingsResponse;
import com.portfolio.settings.dto.SettingsUpdateRequest;
import com.portfolio.settings.dto.SiteProfileResponse;
import com.portfolio.settings.dto.SiteProfileUpdateRequest;
import com.portfolio.settings.dto.SocialLinkRequest;
import com.portfolio.settings.dto.SocialLinkResponse;
import com.portfolio.settings.dto.SocialLinksUpdateRequest;
import com.portfolio.settings.entity.SiteProfile;
import com.portfolio.settings.entity.SiteSetting;
import com.portfolio.settings.entity.SocialLink;
import com.portfolio.settings.mapper.SettingsMapper;
import com.portfolio.settings.repository.SiteProfileRepository;
import com.portfolio.settings.repository.SiteSettingRepository;
import com.portfolio.settings.repository.SocialLinkRepository;
import com.portfolio.settings.service.SettingsService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads always go through the registry rather than through whatever rows happen to exist: the
 * stored values are layered on top of {@link SettingKey#defaultValue()}, so a fresh installation
 * answers every key without a seed migration and a client never sees a key vanish.
 */
@Service
public class SettingsServiceImpl implements SettingsService {

	private static final Logger log = LoggerFactory.getLogger(SettingsServiceImpl.class);

	private final SiteSettingRepository settingRepository;
	private final SocialLinkRepository socialLinkRepository;
	private final SiteProfileRepository siteProfileRepository;
	private final MediaReferenceResolver mediaReferenceResolver;
	private final SettingsMapper settingsMapper;

	public SettingsServiceImpl(
			SiteSettingRepository settingRepository,
			SocialLinkRepository socialLinkRepository,
			SiteProfileRepository siteProfileRepository,
			MediaReferenceResolver mediaReferenceResolver,
			SettingsMapper settingsMapper) {
		this.settingRepository = settingRepository;
		this.socialLinkRepository = socialLinkRepository;
		this.siteProfileRepository = siteProfileRepository;
		this.mediaReferenceResolver = mediaReferenceResolver;
		this.settingsMapper = settingsMapper;
	}

	// --------------------------------------------------------------- public

	@Override
	@Transactional(readOnly = true)
	public PublicSettingsResponse publicSettings() {
		Map<String, String> stored = storedValues();
		Map<String, String> general = new LinkedHashMap<>();
		Map<String, String> seo = new LinkedHashMap<>();

		for (SettingKey key : SettingKey.publicKeys()) {
			String value = stored.getOrDefault(key.key(), key.defaultValue());
			(key.group() == SettingKey.Group.SEO ? seo : general).put(key.key(), value);
		}

		return new PublicSettingsResponse(
				general,
				seo,
				socialLinkRepository.findByVisibleTrueOrderByDisplayOrderAscIdAsc().stream()
						.map(settingsMapper::toResponse)
						.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public SiteProfileResponse profile() {
		return siteProfileRepository
				.findFirstByOrderByIdAsc()
				.map(settingsMapper::toResponse)
				// No row yet is a normal state, not a 404: the site simply has no photo or resume.
				.orElseGet(() -> new SiteProfileResponse(null, null));
	}

	// ---------------------------------------------------------------- admin

	@Override
	@Transactional(readOnly = true)
	public SettingsResponse settingsFor(SettingKey.Group group) {
		Map<String, String> stored = storedValues();
		Map<String, String> values = new LinkedHashMap<>();
		for (SettingKey key : SettingKey.inGroup(group)) {
			values.put(key.key(), stored.getOrDefault(key.key(), key.defaultValue()));
		}
		return new SettingsResponse(values);
	}

	@Override
	@Transactional
	public SettingsResponse update(SettingKey.Group group, SettingsUpdateRequest request) {
		request.settings().forEach((name, value) -> {
			SettingKey key = SettingKey.of(name)
					.orElseThrow(() -> new ValidationException(
							"Unknown setting '%s'. Known settings: %s".formatted(name, SettingKey.knownKeys())));
			if (key.group() != group) {
				throw new ValidationException(
						"Setting '%s' belongs to the %s group and cannot be changed here"
								.formatted(name, key.group()));
			}
			if (value == null) {
				// Reset: dropping the row makes the key read as its registry default again.
				settingRepository.deleteById(name);
				return;
			}
			checkType(key, value);
			settingRepository
					.findById(name)
					.ifPresentOrElse(
							existing -> existing.setValue(value),
							() -> settingRepository.save(new SiteSetting(name, value)));
		});
		log.info("Settings updated: group={} keys={}", group, request.settings().keySet());
		return settingsFor(group);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SocialLinkResponse> socialLinks() {
		return socialLinkRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
				.map(settingsMapper::toResponse)
				.toList();
	}

	@Override
	@Transactional
	public List<SocialLinkResponse> replaceSocialLinks(SocialLinksUpdateRequest request) {
		// Replaced wholesale rather than diffed: it is a short ordered list the admin edits as a
		// whole, and rewriting it keeps display_order consistent with the order actually sent.
		socialLinkRepository.deleteAllInBatch();
		List<SocialLink> links = new ArrayList<>(request.links().size());
		for (int order = 0; order < request.links().size(); order++) {
			SocialLinkRequest link = request.links().get(order);
			links.add(new SocialLink(
					link.platform().trim(), link.url().trim(), order, !Boolean.FALSE.equals(link.visible())));
		}
		return socialLinkRepository.saveAll(links).stream()
				.map(settingsMapper::toResponse)
				.toList();
	}

	@Override
	@Transactional
	public SiteProfileResponse updateProfile(SiteProfileUpdateRequest request) {
		SiteProfile profile = siteProfileRepository.findFirstByOrderByIdAsc().orElseGet(SiteProfile::new);
		profile.setProfileImage(mediaReferenceResolver.resolve(request.profileImageMediaId(), "profileImageMediaId"));
		profile.setResume(mediaReferenceResolver.resolve(request.resumeMediaId(), "resumeMediaId"));
		return settingsMapper.toResponse(siteProfileRepository.save(profile));
	}

	// -------------------------------------------------------------- helpers

	private Map<String, String> storedValues() {
		return settingRepository.findAll().stream()
				.filter(setting -> setting.getValue() != null)
				.collect(Collectors.toMap(SiteSetting::getKey, SiteSetting::getValue));
	}

	/**
	 * The EAV column is untyped text, so the registry's declared type is the only thing standing
	 * between a typo and a frontend that reads {@code "yes"} as a boolean and renders nothing.
	 */
	private void checkType(SettingKey key, String value) {
		if (key.type() == SettingKey.Type.BOOLEAN
				&& !"true".equalsIgnoreCase(value)
				&& !"false".equalsIgnoreCase(value)) {
			throw new ValidationException("Setting '%s' must be true or false".formatted(key.key()));
		}
	}
}
