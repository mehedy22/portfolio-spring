package com.portfolio.media;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.media.service.AllowedFileType;
import com.portfolio.media.service.FileSignatureDetector;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The allow-list is the whole of the upload type defence, so it is tested on its own rather than
 * only through the endpoint: these are the cases where "looks like an image" and "is an image"
 * disagree.
 */
class FileSignatureDetectorTest {

	private final FileSignatureDetector detector = new FileSignatureDetector();

	@Test
	@DisplayName("recognises each allowed format from its magic bytes")
	void detectsAllowedFormats() {
		assertThat(detector.detect(MediaTestFiles.png())).contains(AllowedFileType.PNG);
		assertThat(detector.detect(MediaTestFiles.jpeg())).contains(AllowedFileType.JPEG);
		assertThat(detector.detect(MediaTestFiles.gif())).contains(AllowedFileType.GIF);
		assertThat(detector.detect(MediaTestFiles.webp())).contains(AllowedFileType.WEBP);
		assertThat(detector.detect(MediaTestFiles.pdf())).contains(AllowedFileType.PDF);
	}

	@Test
	@DisplayName("rejects a script that merely claims to be an image")
	void rejectsDisguisedScript() {
		byte[] php = "<?php system($_GET['c']); ?>".getBytes(StandardCharsets.UTF_8);
		assertThat(detector.detect(php)).isEmpty();
	}

	@Test
	@DisplayName("rejects SVG — it is XML that can carry script, and is deliberately off the list")
	void rejectsSvg() {
		byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
				.getBytes(StandardCharsets.UTF_8);
		assertThat(detector.detect(svg)).isEmpty();
	}

	@Test
	@DisplayName("rejects a RIFF container that is not WebP (an AVI is also RIFF)")
	void rejectsNonWebpRiff() {
		byte[] avi = new byte[] {
			'R', 'I', 'F', 'F', 0x10, 0, 0, 0, 'A', 'V', 'I', ' '
		};
		assertThat(detector.detect(avi)).isEmpty();
	}

	@Test
	@DisplayName("rejects empty and truncated content instead of guessing")
	void rejectsEmptyAndTruncated() {
		assertThat(detector.detect(new byte[0])).isEmpty();
		assertThat(detector.detect(null)).isEmpty();
		// The first two bytes of a PNG signature, and nothing more.
		assertThat(detector.detect(new byte[] {(byte) 0x89, 'P'})).isEmpty();
	}
}
