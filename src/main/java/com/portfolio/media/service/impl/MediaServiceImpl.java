package com.portfolio.media.service.impl;

import com.portfolio.common.exception.ResourceNotFoundException;
import com.portfolio.common.exception.ValidationException;
import com.portfolio.common.response.PageResponse;
import com.portfolio.media.MediaProperties;
import com.portfolio.media.dto.MediaResponse;
import com.portfolio.media.entity.Media;
import com.portfolio.media.mapper.MediaMapper;
import com.portfolio.media.repository.MediaRepository;
import com.portfolio.media.service.AllowedFileType;
import com.portfolio.media.service.FileSignatureDetector;
import com.portfolio.media.service.MediaService;
import com.portfolio.media.service.MediaStorageService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload pipeline, in order: read → sniff the real type → enforce the per-type size limit →
 * generate a storage name → write through the storage abstraction → record the row.
 *
 * <p>The client-supplied filename and {@code Content-Type} never influence any of it; they are
 * recorded for display only (docs/08-security/application-security.md, "File upload security").
 */
@Service
public class MediaServiceImpl implements MediaService {

	private static final Logger log = LoggerFactory.getLogger(MediaServiceImpl.class);

	private static final int MAX_PAGE_SIZE = 100;
	private static final int MAX_ALT_TEXT_LENGTH = 300;
	private static final int MAX_ORIGINAL_FILE_NAME_LENGTH = 255;

	private final MediaRepository mediaRepository;
	private final MediaStorageService storageService;
	private final FileSignatureDetector signatureDetector;
	private final MediaMapper mediaMapper;
	private final MediaProperties properties;

	public MediaServiceImpl(
			MediaRepository mediaRepository,
			MediaStorageService storageService,
			FileSignatureDetector signatureDetector,
			MediaMapper mediaMapper,
			MediaProperties properties) {
		this.mediaRepository = mediaRepository;
		this.storageService = storageService;
		this.signatureDetector = signatureDetector;
		this.mediaMapper = mediaMapper;
		this.properties = properties;
	}

	@Override
	@Transactional
	public MediaResponse upload(MultipartFile file, String altText, Long adminId) {
		byte[] content = read(file);
		AllowedFileType type = detectType(content);
		enforceSizeLimit(type, content.length);

		String normalizedAltText = normalizeAltText(altText);
		String storageName = UUID.randomUUID() + "." + type.extension();
		String locator = storageService.store(storageName, content);

		Dimensions dimensions = type.isImage() ? readDimensions(content) : Dimensions.UNKNOWN;

		Media media = new Media(
				storageName,
				originalFileName(file),
				type.mimeType(),
				content.length,
				storageService.backend(),
				locator,
				dimensions.width(),
				dimensions.height(),
				normalizedAltText,
				adminId);

		Media saved = mediaRepository.save(media);
		log.info("Media uploaded: id={} type={} bytes={}", saved.getId(), type.mimeType(), content.length);
		return mediaMapper.toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<MediaResponse> list(int page, int size) {
		PageRequest pageRequest = PageRequest.of(
				Math.max(page, 0),
				Math.clamp(size, 1, MAX_PAGE_SIZE),
				Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
		Page<MediaResponse> found = mediaRepository.findAll(pageRequest).map(mediaMapper::toResponse);
		return PageResponse.from(found);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		Media media = mediaRepository.findById(id).orElseThrow(() -> notFound(id));
		// Soft delete (@SQLDelete) — the row survives for audit/restore, but the bytes do not:
		// keeping the file of a "deleted" image would leave it publicly fetchable by anyone who
		// had already learned its URL.
		mediaRepository.delete(media);
		storageService.delete(media.getStoragePathOrUrl());
		log.info("Media deleted: id={}", id);
	}

	@Override
	@Transactional(readOnly = true)
	public MediaContent loadContent(Long id) {
		Media media = mediaRepository.findById(id).orElseThrow(() -> notFound(id));
		Resource resource = storageService
				.load(media.getStoragePathOrUrl())
				.orElseThrow(() -> {
					// Row without bytes: a storage-layer inconsistency worth an operator's attention,
					// but the caller still just gets a 404.
					log.error("Media row {} has no readable file at its recorded location", id);
					return notFound(id);
				});
		return new MediaContent(resource, media.getMimeType(), media.getSizeBytes(), media.getOriginalFileName());
	}

	private byte[] read(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ValidationException("A file is required");
		}
		try {
			return file.getBytes();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not read the uploaded file", ex);
		}
	}

	private AllowedFileType detectType(byte[] content) {
		return signatureDetector.detect(content).orElseThrow(() -> new ValidationException(
				"Unsupported file type. Allowed: JPEG, PNG, GIF, WebP images and PDF documents"));
	}

	private void enforceSizeLimit(AllowedFileType type, int actualBytes) {
		long limit = (type.isImage() ? properties.maxImageSize() : properties.maxDocumentSize()).toBytes();
		if (actualBytes > limit) {
			throw new ValidationException("File exceeds the %d MB limit for %s uploads"
					.formatted(limit / (1024 * 1024), type.isImage() ? "image" : "document"));
		}
	}

	private String normalizeAltText(String altText) {
		if (altText == null || altText.isBlank()) {
			return null;
		}
		String trimmed = altText.trim();
		if (trimmed.length() > MAX_ALT_TEXT_LENGTH) {
			throw new ValidationException("Alt text must be at most %d characters".formatted(MAX_ALT_TEXT_LENGTH));
		}
		return trimmed;
	}

	/**
	 * Kept for display only. Any directory component is stripped rather than trusted: browsers are
	 * supposed to send a bare name, but the part header is client-controlled, and this value must
	 * never be able to look like a path.
	 */
	private String originalFileName(MultipartFile file) {
		String submitted = file.getOriginalFilename();
		if (submitted == null || submitted.isBlank()) {
			return "unnamed";
		}
		String bare = submitted.replace('\\', '/');
		bare = bare.substring(bare.lastIndexOf('/') + 1).trim();
		if (bare.isBlank()) {
			return "unnamed";
		}
		return bare.length() > MAX_ORIGINAL_FILE_NAME_LENGTH
				? bare.substring(0, MAX_ORIGINAL_FILE_NAME_LENGTH)
				: bare;
	}

	/** Empty dimensions when no installed ImageIO reader understands the format (e.g. WebP). */
	private Dimensions readDimensions(byte[] content) {
		try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
			if (stream == null) {
				return Dimensions.UNKNOWN;
			}
			Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
			if (!readers.hasNext()) {
				return Dimensions.UNKNOWN;
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(stream);
				return new Dimensions(reader.getWidth(0), reader.getHeight(0));
			}
			finally {
				reader.dispose();
			}
		}
		catch (IOException | RuntimeException ex) {
			log.warn("Could not read image dimensions; storing them as unknown", ex);
			return Dimensions.UNKNOWN;
		}
	}

	private ResourceNotFoundException notFound(Long id) {
		return new ResourceNotFoundException("Media " + id + " not found");
	}

	private record Dimensions(Integer width, Integer height) {

		static final Dimensions UNKNOWN = new Dimensions(null, null);
	}
}
