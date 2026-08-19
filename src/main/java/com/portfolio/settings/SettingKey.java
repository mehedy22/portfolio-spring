package com.portfolio.settings;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The catalogue of settings: which keys exist, what each holds, its default, and whether it may
 * be served publicly (D-024).
 *
 * <p>This lives in code rather than as columns on {@code site_setting} for two reasons. Public
 * exposure is a security boundary, and a boundary an admin can widen by typing a key name into a
 * form is one careless click from leaking something. And a genuinely new setting always needs
 * frontend code to consume it, so it was never a no-deploy change to begin with — making the
 * registry deployable costs nothing real.
 *
 * <p>A key absent from {@code site_setting} is not an error: it simply reads as its
 * {@link #defaultValue()}, so a fresh installation serves sensible values with no seed migration.
 */
public enum SettingKey {

	SITE_TITLE("site.title", Type.STRING, "My Portfolio", Access.PUBLIC, Group.GENERAL),
	SITE_TAGLINE("site.tagline", Type.STRING, "", Access.PUBLIC, Group.GENERAL),
	SITE_DESCRIPTION("site.description", Type.STRING, "", Access.PUBLIC, Group.GENERAL),
	SITE_FOOTER_TEXT("site.footer_text", Type.STRING, "", Access.PUBLIC, Group.GENERAL),
	SITE_COPYRIGHT("site.copyright", Type.STRING, "", Access.PUBLIC, Group.GENERAL),

	/**
	 * Where admin notifications would go (FR-15, not built yet) — deliberately private. Publishing
	 * a mailbox is what the contact form exists to avoid.
	 */
	CONTACT_NOTIFICATION_EMAIL("contact.notification_email", Type.STRING, "", Access.PRIVATE, Group.GENERAL),

	/** Nav toggles: the design hides Articles/Research until those modules ship. */
	NAV_SHOW_ARTICLES("nav.show_articles", Type.BOOLEAN, "false", Access.PUBLIC, Group.GENERAL),
	NAV_SHOW_RESEARCH("nav.show_research", Type.BOOLEAN, "false", Access.PUBLIC, Group.GENERAL),

	SEO_DEFAULT_TITLE("seo.default_title", Type.STRING, "", Access.PUBLIC, Group.SEO),
	SEO_DEFAULT_DESCRIPTION("seo.default_description", Type.STRING, "", Access.PUBLIC, Group.SEO),
	SEO_DEFAULT_OG_IMAGE_URL("seo.default_og_image_url", Type.STRING, "", Access.PUBLIC, Group.SEO);

	/** Which admin screen owns the key (docs/10-frontend/routes-and-layouts.md). */
	public enum Group {
		GENERAL,
		SEO
	}

	public enum Access {
		PUBLIC,
		PRIVATE
	}

	public enum Type {
		STRING,
		BOOLEAN
	}

	private final String key;
	private final Type type;
	private final String defaultValue;
	private final Access access;
	private final Group group;

	SettingKey(String key, Type type, String defaultValue, Access access, Group group) {
		this.key = key;
		this.type = type;
		this.defaultValue = defaultValue;
		this.access = access;
		this.group = group;
	}

	public String key() {
		return key;
	}

	public Type type() {
		return type;
	}

	public String defaultValue() {
		return defaultValue;
	}

	public Group group() {
		return group;
	}

	public boolean isPublic() {
		return access == Access.PUBLIC;
	}

	public static Optional<SettingKey> of(String key) {
		return Arrays.stream(values()).filter(candidate -> candidate.key.equals(key)).findFirst();
	}

	public static List<SettingKey> inGroup(Group group) {
		return Arrays.stream(values()).filter(candidate -> candidate.group == group).toList();
	}

	public static List<SettingKey> publicKeys() {
		return Arrays.stream(values()).filter(SettingKey::isPublic).toList();
	}

	/** Every known key name, for the "unknown setting" error message. */
	public static String knownKeys() {
		return String.join(", ", Arrays.stream(values()).map(SettingKey::key).sorted().toList());
	}
}
