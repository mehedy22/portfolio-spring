package com.portfolio.media.repository;

import com.portfolio.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Soft-deleted rows are excluded automatically by the entity's {@code @SQLRestriction} — no
 * repository method needs to restate {@code deleted_at IS NULL}
 * (docs/11-technical-design/backend-design.md).
 */
public interface MediaRepository extends JpaRepository<Media, Long> {
}
