package com.portfolio.common.text;

import java.text.Normalizer;
import java.util.Locale;

/**
 * URL slug generation. Shared rather than project-local because Blog and Research (SHOULD tier)
 * slug the same way when they arrive.
 */
public final class Slugs {

	private static final int MAX_LENGTH = 220;

	private Slugs() {
	}

	/**
	 * A lowercase, hyphen-separated slug: accents are folded to ASCII ("Café" → "cafe") and every
	 * other non-alphanumeric run collapses to a single hyphen. Returns an empty string when the
	 * input has nothing sluggable in it (e.g. only punctuation) — callers decide what that means.
	 */
	public static String from(String text) {
		if (text == null || text.isBlank()) {
			return "";
		}
		String ascii = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
		String slug = ascii.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("(^-+)|(-+$)", "");
		return slug.length() > MAX_LENGTH ? trimTrailingHyphen(slug.substring(0, MAX_LENGTH)) : slug;
	}

	/** True when {@code slug} is already in the canonical form {@link #from(String)} produces. */
	public static boolean isValid(String slug) {
		return slug != null && !slug.isBlank() && slug.length() <= MAX_LENGTH && slug.equals(from(slug));
	}

	private static String trimTrailingHyphen(String slug) {
		return slug.replaceAll("-+$", "");
	}
}
