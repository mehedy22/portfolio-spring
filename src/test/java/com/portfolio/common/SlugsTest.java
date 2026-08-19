package com.portfolio.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.common.text.Slugs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SlugsTest {

	@Test
	@DisplayName("lowercases, folds accents, and collapses punctuation runs into single hyphens")
	void slugifies() {
		assertThat(Slugs.from("A Café — Real-Time Sync!")).isEqualTo("a-cafe-real-time-sync");
		assertThat(Slugs.from("  Spring   Boot  ")).isEqualTo("spring-boot");
		assertThat(Slugs.from("C++ / Rust")).isEqualTo("c-rust");
	}

	@Test
	@DisplayName("returns empty when there is nothing sluggable, rather than a lone hyphen")
	void handlesUnsluggableInput() {
		assertThat(Slugs.from("!!! ???")).isEmpty();
		assertThat(Slugs.from("")).isEmpty();
		assertThat(Slugs.from(null)).isEmpty();
	}

	@Test
	@DisplayName("truncation never leaves a trailing hyphen")
	void truncatesCleanly() {
		String slug = Slugs.from("word ".repeat(80));
		assertThat(slug).hasSizeLessThanOrEqualTo(220).doesNotEndWith("-");
	}

	@Test
	@DisplayName("only canonical slugs validate")
	void validates() {
		assertThat(Slugs.isValid("real-time-sync")).isTrue();
		assertThat(Slugs.isValid("Real-Time")).isFalse();
		assertThat(Slugs.isValid("double--hyphen")).isFalse();
		assertThat(Slugs.isValid("-leading")).isFalse();
		assertThat(Slugs.isValid("")).isFalse();
	}
}
