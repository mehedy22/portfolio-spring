package com.portfolio.media.service;

import java.util.Arrays;
import java.util.Optional;

/**
 * The upload allow-list (NFR-06, D-018). A file is accepted only if its <em>content</em> matches
 * one of these signatures — the client-supplied {@code Content-Type} and the filename extension
 * are both trivially spoofable and are never trusted (docs/08-security/application-security.md).
 *
 * <p>SVG is deliberately absent: it is XML that can carry {@code <script>}, so serving one inline
 * from the API's own origin would be a stored-XSS vector. Adding it later would require sanitizing
 * the document, not just widening this list.
 */
public enum AllowedFileType {

	JPEG("image/jpeg", "jpg", true, sig(0xFF, 0xD8, 0xFF)),
	PNG("image/png", "png", true, sig(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)),
	GIF("image/gif", "gif", true, sig(0x47, 0x49, 0x46, 0x38)),
	PDF("application/pdf", "pdf", false, sig(0x25, 0x50, 0x44, 0x46, 0x2D)),
	/** RIFF container: bytes 0-3 are "RIFF", 8-11 are "WEBP" — handled by {@link #WEBP_MARKER}. */
	WEBP("image/webp", "webp", true, sig(0x52, 0x49, 0x46, 0x46));

	private static final byte[] WEBP_MARKER = sig(0x57, 0x45, 0x42, 0x50);
	private static final int WEBP_MARKER_OFFSET = 8;

	/** Longest prefix any detector needs to look at. */
	static final int SIGNATURE_WINDOW = WEBP_MARKER_OFFSET + 4;

	private final String mimeType;
	private final String extension;
	private final boolean image;
	private final byte[] signature;

	AllowedFileType(String mimeType, String extension, boolean image, byte[] signature) {
		this.mimeType = mimeType;
		this.extension = extension;
		this.image = image;
		this.signature = signature;
	}

	public String mimeType() {
		return mimeType;
	}

	public String extension() {
		return extension;
	}

	/** Images get their pixel dimensions recorded; documents do not. */
	public boolean isImage() {
		return image;
	}

	static Optional<AllowedFileType> matching(byte[] head) {
		return Arrays.stream(values()).filter(type -> type.matches(head)).findFirst();
	}

	private boolean matches(byte[] head) {
		if (!startsWith(head, signature, 0)) {
			return false;
		}
		// RIFF alone is also AVI/WAV — only the WEBP marker makes it an image we accept.
		return this != WEBP || startsWith(head, WEBP_MARKER, WEBP_MARKER_OFFSET);
	}

	private static boolean startsWith(byte[] head, byte[] expected, int offset) {
		if (head.length < offset + expected.length) {
			return false;
		}
		for (int i = 0; i < expected.length; i++) {
			if (head[offset + i] != expected[i]) {
				return false;
			}
		}
		return true;
	}

	private static byte[] sig(int... bytes) {
		byte[] signature = new byte[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			signature[i] = (byte) bytes[i];
		}
		return signature;
	}
}
