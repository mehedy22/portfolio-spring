package com.portfolio.media.service.impl;

import com.portfolio.media.MediaProperties;
import com.portfolio.media.entity.StorageBackend;
import com.portfolio.media.service.MediaStorageService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Local-filesystem backend, used for development (docs/05-architecture/deployment-view.md).
 *
 * <p>Files are sharded into {@code yyyy/MM/} directories so the upload root does not degrade into
 * one flat directory with thousands of entries.
 *
 * <p>Every locator is re-resolved against the configured root and checked to still be inside it
 * before any read or delete. The names this class generates can't escape, but that check is what
 * keeps a future corrupted or hand-edited {@code storage_path_or_url} (e.g. {@code ../../etc/passwd})
 * from turning into an arbitrary-file read.
 */
@Service
public class LocalMediaStorageService implements MediaStorageService {

	private static final Logger log = LoggerFactory.getLogger(LocalMediaStorageService.class);

	private final Path root;

	public LocalMediaStorageService(MediaProperties properties) {
		this.root = properties.storageRoot().toAbsolutePath().normalize();
	}

	@Override
	public StorageBackend backend() {
		return StorageBackend.LOCAL;
	}

	@Override
	public String store(String fileName, byte[] content) {
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		String relativePath = "%04d/%02d/%s".formatted(today.getYear(), today.getMonthValue(), fileName);
		Path target = resolve(relativePath)
				.orElseThrow(() -> new IllegalArgumentException("Generated storage path escapes the media root"));
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, content);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not store media file " + fileName, ex);
		}
		return relativePath;
	}

	@Override
	public Optional<Resource> load(String storagePathOrUrl) {
		return resolve(storagePathOrUrl)
				.filter(Files::isRegularFile)
				.map(PathResource::new);
	}

	@Override
	public void delete(String storagePathOrUrl) {
		Optional<Path> path = resolve(storagePathOrUrl);
		if (path.isEmpty()) {
			log.warn("Refusing to delete media locator outside the storage root");
			return;
		}
		try {
			Files.deleteIfExists(path.get());
		}
		catch (IOException ex) {
			// The row is already soft-deleted; an orphaned file is untidy, not incorrect.
			log.warn("Could not delete stored media file {}", storagePathOrUrl, ex);
		}
	}

	/** Empty when the locator resolves outside {@link #root}. */
	private Optional<Path> resolve(String storagePathOrUrl) {
		if (storagePathOrUrl == null || storagePathOrUrl.isBlank()) {
			return Optional.empty();
		}
		Path candidate = root.resolve(storagePathOrUrl).normalize();
		return candidate.startsWith(root) ? Optional.of(candidate) : Optional.empty();
	}
}
