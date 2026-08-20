package com.portfolio.blog.dto;

/** A category or tag, for the admin's pickers and the public filter links. */
public record TaxonomyResponse(Long id, String name, String slug) {
}
