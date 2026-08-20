package com.portfolio.analytics.service;

import com.portfolio.analytics.entity.DeviceType;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Reduces a User-Agent header to two coarse buckets and throws the rest away.
 *
 * <p>This is intentionally crude. A full UA-parsing library would give finer-grained detail —
 * exact versions, engines, OS builds — which is precisely the detail that makes a User-Agent a
 * fingerprint. Storing "MOBILE" and "Safari" tells the admin what they actually want to know
 * (does the site need to work well on phones?) while retaining nothing that could re-identify a
 * visitor, which is what FR-17's "no PII" requirement is protecting.
 */
@Component
public class UserAgentParser {

	public DeviceType deviceType(String userAgent) {
		if (userAgent == null || userAgent.isBlank()) {
			return DeviceType.UNKNOWN;
		}
		String value = userAgent.toLowerCase(Locale.ROOT);
		if (value.contains("ipad") || (value.contains("android") && !value.contains("mobile"))
				|| value.contains("tablet")) {
			return DeviceType.TABLET;
		}
		if (value.contains("mobi") || value.contains("iphone") || value.contains("android")) {
			return DeviceType.MOBILE;
		}
		return DeviceType.DESKTOP;
	}

	/** Family only — never a version, which narrows a visitor far more than the family does. */
	public String browser(String userAgent) {
		if (userAgent == null || userAgent.isBlank()) {
			return null;
		}
		String value = userAgent.toLowerCase(Locale.ROOT);
		// Order matters: Edge and Chrome both claim "chrome"/"safari" in their UA strings.
		if (value.contains("edg/")) {
			return "Edge";
		}
		if (value.contains("opr/") || value.contains("opera")) {
			return "Opera";
		}
		if (value.contains("firefox")) {
			return "Firefox";
		}
		if (value.contains("chrome") || value.contains("crios")) {
			return "Chrome";
		}
		if (value.contains("safari")) {
			return "Safari";
		}
		return "Other";
	}
}
