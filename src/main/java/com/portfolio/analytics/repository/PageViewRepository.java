package com.portfolio.analytics.repository;

import com.portfolio.analytics.entity.PageView;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Aggregates are computed on read for v1 — the raw log is small enough, and a rollup table is a
 * follow-up for when real traffic volume is known (docs/06-database/table-definitions.md).
 *
 * <p>All queries are JPQL with bind parameters; nothing is string-built (Phase 8).
 */
public interface PageViewRepository extends JpaRepository<PageView, Long> {

	long countByViewedAtAfter(Instant since);

	Page<PageView> findByEntityType(String entityType, Pageable pageable);

	Page<PageView> findByViewedAtBetween(Instant from, Instant to, Pageable pageable);

	Page<PageView> findByEntityTypeAndViewedAtBetween(
			String entityType, Instant from, Instant to, Pageable pageable);

	@Query("""
			select v.path as label, count(v) as total
			from PageView v
			where v.viewedAt >= :since
			group by v.path
			order by count(v) desc
			""")
	List<CountByLabel> topPaths(@Param("since") Instant since, Pageable pageable);

	@Query("""
			select v.referrer as label, count(v) as total
			from PageView v
			where v.viewedAt >= :since and v.referrer is not null and v.referrer <> ''
			group by v.referrer
			order by count(v) desc
			""")
	List<CountByLabel> topReferrers(@Param("since") Instant since, Pageable pageable);

	@Query("""
			select cast(v.deviceType as string) as label, count(v) as total
			from PageView v
			where v.viewedAt >= :since
			group by v.deviceType
			order by count(v) desc
			""")
	List<CountByLabel> byDevice(@Param("since") Instant since);

	@Query("""
			select v.browser as label, count(v) as total
			from PageView v
			where v.viewedAt >= :since and v.browser is not null
			group by v.browser
			order by count(v) desc
			""")
	List<CountByLabel> byBrowser(@Param("since") Instant since);

	/** Views per entity, for "which project is actually being read". */
	@Query("""
			select v.entityType as entityType, v.entityId as entityId, count(v) as total
			from PageView v
			where v.viewedAt >= :since and v.entityType is not null and v.entityId is not null
			group by v.entityType, v.entityId
			order by count(v) desc
			""")
	List<CountByEntity> topEntities(@Param("since") Instant since, Pageable pageable);

	/** One row per calendar day (UTC), for the trend chart. */
	@Query(
			value = """
					select date_trunc('day', viewed_at) as day, count(*) as total
					from page_view
					where viewed_at >= :since
					group by 1
					order by 1
					""",
			nativeQuery = true)
	List<DailyCount> dailyCounts(@Param("since") Instant since);

	interface CountByLabel {
		String getLabel();

		long getTotal();
	}

	interface CountByEntity {
		String getEntityType();

		Long getEntityId();

		long getTotal();
	}

	interface DailyCount {
		Instant getDay();

		long getTotal();
	}
}
