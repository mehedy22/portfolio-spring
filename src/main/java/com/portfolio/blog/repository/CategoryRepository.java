package com.portfolio.blog.repository;

import com.portfolio.blog.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	Optional<Category> findByNameIgnoreCase(String name);

	Optional<Category> findBySlug(String slug);

	List<Category> findAllByOrderByNameAsc();
}
