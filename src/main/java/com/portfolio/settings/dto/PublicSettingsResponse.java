package com.portfolio.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * Everything both frontends need to render their shell — site title, tagline, footer, nav
 * toggles, SEO defaults and the visible social links — in one request, so a page load does not
 * fan out to fetch its own chrome.
 *
 * <p>Only keys marked public in the registry appear here (D-024).
 */
@Schema(description = "Public-safe site configuration")
public record PublicSettingsResponse(
		Map<String, String> settings, Map<String, String> seo, List<SocialLinkResponse> socialLinks) {
}
