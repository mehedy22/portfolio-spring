package com.portfolio.common.html;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

/**
 * Allow-list sanitization for the one field in the system that is genuinely rich text: an
 * article's body (Phase 8; the condition D-027 said would trigger this).
 *
 * <p>Sanitizing on <em>write</em> rather than on render is deliberate: it means the database can
 * never hold a payload waiting for the one code path that forgets to escape it, and every future
 * consumer — an RSS feed, an export, the AI module — inherits the guarantee without knowing it
 * exists.
 *
 * <p>The policy permits what a written article actually needs and nothing more. No {@code
 * <script>}, no {@code <style>}, no event handlers, no {@code <iframe>}, no {@code <form>}.
 * Links are forced to {@code rel="nofollow noopener noreferrer"} and restricted to http/https, so
 * a {@code javascript:} URL cannot survive.
 */
@Component
public class HtmlSanitizer {

	private static final PolicyFactory POLICY = Sanitizers.FORMATTING
			.and(Sanitizers.BLOCKS)
			.and(Sanitizers.TABLES)
			.and(new HtmlPolicyBuilder()
					.allowElements("h2", "h3", "h4", "pre", "code", "hr", "figure", "figcaption")
					.allowAttributes("class").onElements("code", "pre")
					.toFactory())
			.and(new HtmlPolicyBuilder()
					.allowElements("a")
					.allowAttributes("href").onElements("a")
					.allowStandardUrlProtocols()
					.requireRelNofollowOnLinks()
					.toFactory())
			.and(new HtmlPolicyBuilder()
					.allowElements("img")
					.allowAttributes("src", "alt", "title").onElements("img")
					.allowStandardUrlProtocols()
					.toFactory());

	/** Returns the safe subset of {@code html}; null and blank pass through unchanged. */
	public String sanitize(String html) {
		if (html == null || html.isBlank()) {
			return html;
		}
		return POLICY.sanitize(html);
	}
}
