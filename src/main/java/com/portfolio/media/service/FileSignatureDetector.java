package com.portfolio.media.service;

import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Content-sniffing type detection: reads the leading magic bytes and returns the matching
 * {@link AllowedFileType}, or empty when the content is anything else.
 *
 * <p>This is an allow-list, not a classifier — an unrecognised file is rejected rather than
 * guessed at, which is what makes a renamed {@code .php} or a zip-bomb-with-an-image-extension
 * fail closed (docs/08-security/threat-model.md, "Malicious file upload").
 */
@Component
public class FileSignatureDetector {

	/** The detected type of {@code content}, or empty if it is not on the allow-list. */
	public Optional<AllowedFileType> detect(byte[] content) {
		if (content == null || content.length == 0) {
			return Optional.empty();
		}
		int window = Math.min(content.length, AllowedFileType.SIGNATURE_WINDOW);
		byte[] head = new byte[window];
		System.arraycopy(content, 0, head, 0, window);
		return AllowedFileType.matching(head);
	}
}
