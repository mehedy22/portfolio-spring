package com.portfolio.common.response;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Pagination envelope for admin list endpoints, per docs/07-api/api-conventions.md:
 * {content, page, size, totalElements, totalPages}.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}
}
