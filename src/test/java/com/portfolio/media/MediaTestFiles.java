package com.portfolio.media;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;

/** Byte fixtures for the upload tests — real encoded images where the test needs real dimensions. */
final class MediaTestFiles {

	static final int PNG_WIDTH = 24;
	static final int PNG_HEIGHT = 12;

	private MediaTestFiles() {
	}

	/** A genuinely encoded PNG, so ImageIO can read its dimensions back. */
	static byte[] png() {
		return encode("png", PNG_WIDTH, PNG_HEIGHT);
	}

	static byte[] jpeg() {
		return encode("jpg", 8, 8);
	}

	/** Minimal GIF87a header — enough for the allow-list, which only inspects the signature. */
	static byte[] gif() {
		return new byte[] {'G', 'I', 'F', '8', '7', 'a', 1, 0, 1, 0};
	}

	/** RIFF container carrying the WEBP marker at offset 8. */
	static byte[] webp() {
		return new byte[] {'R', 'I', 'F', 'F', 0x1A, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};
	}

	static byte[] pdf() {
		return "%PDF-1.7\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF\n".getBytes(StandardCharsets.UTF_8);
	}

	/** A PNG header followed by filler, used to exceed a size limit without encoding a huge image. */
	static byte[] oversizedPng(int totalBytes) {
		byte[] header = png();
		byte[] padded = new byte[totalBytes];
		System.arraycopy(header, 0, padded, 0, header.length);
		return padded;
	}

	private static byte[] encode(String format, int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, format, out);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return out.toByteArray();
	}
}
