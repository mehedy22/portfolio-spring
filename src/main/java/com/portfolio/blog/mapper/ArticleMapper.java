package com.portfolio.blog.mapper;

import com.portfolio.blog.dto.ArticleResponse;
import com.portfolio.blog.dto.ArticleSummaryResponse;
import com.portfolio.blog.dto.TaxonomyResponse;
import com.portfolio.blog.entity.Article;
import com.portfolio.blog.entity.Category;
import com.portfolio.blog.entity.Tag;
import com.portfolio.media.mapper.MediaMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = MediaMapper.class)
public interface ArticleMapper {

	@Mapping(target = "category", source = "category.name")
	ArticleResponse toResponse(Article article);

	@Mapping(target = "category", source = "category.name")
	ArticleSummaryResponse toSummary(Article article);

	TaxonomyResponse toTaxonomy(Category category);

	TaxonomyResponse toTaxonomy(Tag tag);

	/** Alphabetical, so tag order is stable rather than insertion-dependent. */
	default List<String> toTagNames(Set<Tag> tags) {
		if (tags == null) {
			return List.of();
		}
		return tags.stream()
				.map(Tag::getName)
				.sorted(Comparator.comparing(name -> name.toLowerCase()))
				.toList();
	}
}
