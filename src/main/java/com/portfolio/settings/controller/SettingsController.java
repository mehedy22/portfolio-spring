package com.portfolio.settings.controller;

import com.portfolio.common.response.ApiResponse;
import com.portfolio.settings.SettingKey;
import com.portfolio.settings.dto.PublicSettingsResponse;
import com.portfolio.settings.dto.SettingsResponse;
import com.portfolio.settings.dto.SettingsUpdateRequest;
import com.portfolio.settings.dto.SiteProfileResponse;
import com.portfolio.settings.dto.SiteProfileUpdateRequest;
import com.portfolio.settings.dto.SocialLinkResponse;
import com.portfolio.settings.dto.SocialLinksUpdateRequest;
import com.portfolio.settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Settings endpoints per docs/07-api/endpoints.md.
 *
 * <p>The public route serves only keys the registry marks public (D-024), so the boundary is a
 * property of the key rather than of the caller remembering to filter.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Settings", description = "Site configuration (FR-11, FR-14, D-015)")
public class SettingsController {

	private final SettingsService settingsService;

	public SettingsController(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	// --------------------------------------------------------------- public

	@GetMapping("/settings")
	@Operation(
			summary = "Public site configuration",
			description = "Title, tagline, footer, nav toggles, SEO defaults and visible social "
					+ "links — everything both frontends need to render their shell, in one call.")
	public ApiResponse<PublicSettingsResponse> publicSettings() {
		return ApiResponse.of(settingsService.publicSettings());
	}

	@GetMapping("/settings/profile")
	@Operation(summary = "Profile photo and resume", description = "Either may be null (D-015).")
	public ApiResponse<SiteProfileResponse> publicProfile() {
		return ApiResponse.of(settingsService.profile());
	}

	// ---------------------------------------------------------------- admin

	@GetMapping("/admin/settings")
	@Operation(summary = "General settings", description = "Every general key, defaults filled in.")
	public ApiResponse<SettingsResponse> generalSettings() {
		return ApiResponse.of(settingsService.settingsFor(SettingKey.Group.GENERAL));
	}

	@PutMapping("/admin/settings")
	@Operation(
			summary = "Change general settings",
			description = "Upserts the keys sent and leaves the rest alone. A null value resets a "
					+ "key to its default. Unknown keys are rejected.")
	public ApiResponse<SettingsResponse> updateGeneralSettings(@Valid @RequestBody SettingsUpdateRequest request) {
		return ApiResponse.of(settingsService.update(SettingKey.Group.GENERAL, request), "Settings updated");
	}

	@GetMapping("/admin/settings/seo")
	@Operation(summary = "SEO defaults", description = "Default meta/OG values (SHOULD, FR-14).")
	public ApiResponse<SettingsResponse> seoSettings() {
		return ApiResponse.of(settingsService.settingsFor(SettingKey.Group.SEO));
	}

	@PutMapping("/admin/settings/seo")
	@Operation(summary = "Change SEO defaults", description = "Same upsert semantics as general settings.")
	public ApiResponse<SettingsResponse> updateSeoSettings(@Valid @RequestBody SettingsUpdateRequest request) {
		return ApiResponse.of(settingsService.update(SettingKey.Group.SEO, request), "SEO defaults updated");
	}

	@GetMapping("/admin/settings/social-links")
	@Operation(summary = "Social links", description = "All links, including hidden ones, in order.")
	public ApiResponse<List<SocialLinkResponse>> socialLinks() {
		return ApiResponse.of(settingsService.socialLinks());
	}

	@PutMapping("/admin/settings/social-links")
	@Operation(summary = "Replace social links", description = "Whole-list replace; order comes from position.")
	public ApiResponse<List<SocialLinkResponse>> replaceSocialLinks(
			@Valid @RequestBody SocialLinksUpdateRequest request) {
		return ApiResponse.of(settingsService.replaceSocialLinks(request), "Social links updated");
	}

	@GetMapping("/admin/settings/profile")
	@Operation(summary = "Profile photo and resume", description = "Admin view of the same singleton row.")
	public ApiResponse<SiteProfileResponse> adminProfile() {
		return ApiResponse.of(settingsService.profile());
	}

	@PutMapping("/admin/settings/profile")
	@Operation(
			summary = "Set profile photo and resume",
			description = "References media uploaded earlier; null clears without deleting the file.")
	public ApiResponse<SiteProfileResponse> updateProfile(@Valid @RequestBody SiteProfileUpdateRequest request) {
		return ApiResponse.of(settingsService.updateProfile(request), "Profile updated");
	}
}
