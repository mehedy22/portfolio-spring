package com.portfolio.media;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Upload limits and storage location (NFR-06, D-018). Externalized rather than hardcoded, per
 * Portfolio.md's "nothing that could change without a deploy is hardcoded" principle — raising
 * the image limit should be an env-var change, not a release.
 *
 * @param maxImageSize per-file ceiling for image uploads
 * @param maxDocumentSize per-file ceiling for document (PDF) uploads
 * @param storageRoot local-filesystem root for the LOCAL storage backend
 */
@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(DataSize maxImageSize, DataSize maxDocumentSize, Path storageRoot) {
}
