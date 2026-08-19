package com.portfolio.media.entity;

/** Where a media file physically lives — mirrors the {@code ck_media_storage_backend} CHECK. */
public enum StorageBackend {

	/** Local filesystem, used in development (docs/05-architecture/system-architecture.md §4). */
	LOCAL,

	/** S3-compatible object store, used in production. Provider still TBD (OPEN_QUESTIONS.md #11). */
	OBJECT_STORAGE
}
