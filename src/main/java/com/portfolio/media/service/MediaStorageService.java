package com.portfolio.media.service;

import com.portfolio.media.entity.StorageBackend;
import java.util.Optional;
import org.springframework.core.io.Resource;

/**
 * Storage-agnostic file access (docs/05-architecture/system-architecture.md §4): local filesystem
 * in development, an S3-compatible object store in production. Swapping the backend must require
 * no change in any caller — which is why callers only ever see the opaque
 * {@code storagePathOrUrl} this returns, never a filesystem path they could manipulate.
 */
public interface MediaStorageService {

	/** Which backend this implementation persists to — recorded on every {@code media} row. */
	StorageBackend backend();

	/**
	 * Persists {@code content} under a server-generated name.
	 *
	 * @param fileName generated, collision-resistant name (never user-supplied)
	 * @return the backend-specific locator to store in {@code media.storage_path_or_url}
	 */
	String store(String fileName, byte[] content);

	/** The stored bytes, or empty when the locator resolves to nothing readable. */
	Optional<Resource> load(String storagePathOrUrl);

	/** Removes the stored file. Absent files are not an error — deletion is idempotent. */
	void delete(String storagePathOrUrl);
}
