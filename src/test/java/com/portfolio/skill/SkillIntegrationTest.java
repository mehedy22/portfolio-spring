package com.portfolio.skill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.ContentModuleTestBase;
import com.portfolio.skill.repository.SkillCategoryRepository;
import com.portfolio.skill.repository.SkillRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

class SkillIntegrationTest extends ContentModuleTestBase {

	@Autowired
	private SkillRepository skillRepository;

	@Autowired
	private SkillCategoryRepository categoryRepository;

	@BeforeEach
	void clear() {
		truncate("skill", "skill_category");
	}

	@Test
	@DisplayName("a skill defaults to PUBLISHED and creates its category on the way through")
	void createDefaultsToPublished() throws Exception {
		mockMvc.perform(adminPost("/api/v1/admin/skills", skill("Spring Boot", "Backend")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("PUBLISHED"))
				.andExpect(jsonPath("$.data.category").value("Backend"));

		assertThat(categoryRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("categories are matched case-insensitively rather than duplicated")
	void categoriesAreDeduplicated() throws Exception {
		mockMvc.perform(adminPost("/api/v1/admin/skills", skill("Spring Boot", "Backend")))
				.andExpect(status().isCreated());
		mockMvc.perform(adminPost("/api/v1/admin/skills", skill("Postgres", "  backend  ")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.category").value("Backend"));

		assertThat(categoryRepository.count()).as("one Backend group").isEqualTo(1);
	}

	@Test
	@DisplayName("the public list is grouped by category, alphabetically, each in display order")
	void publicListIsGrouped() throws Exception {
		mockMvc.perform(adminPost("/api/v1/admin/skills", orderedSkill("Terraform", "DevOps", 1)));
		mockMvc.perform(adminPost("/api/v1/admin/skills", orderedSkill("Postgres", "Backend", 2)));
		mockMvc.perform(adminPost("/api/v1/admin/skills", orderedSkill("Spring Boot", "Backend", 1)));

		mockMvc.perform(get("/api/v1/skills"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].category").value("Backend"))
				.andExpect(jsonPath("$.data[0].skills[0].name").value("Spring Boot"))
				.andExpect(jsonPath("$.data[0].skills[1].name").value("Postgres"))
				.andExpect(jsonPath("$.data[1].category").value("DevOps"))
				.andExpect(jsonPath("$.data[1].skills.length()").value(1));
	}

	@Test
	@DisplayName("a draft skill is absent from the public grouping and takes its empty group with it")
	void draftSkillsAreInvisible() throws Exception {
		Map<String, Object> draft = skill("Secret Tool", "Internal");
		draft.put("status", "DRAFT");
		mockMvc.perform(adminPost("/api/v1/admin/skills", draft)).andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/skills")).andExpect(jsonPath("$.data.length()").value(0));
		mockMvc.perform(get("/api/v1/admin/skills").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(jsonPath("$.data.length()").value(1));
	}

	@Test
	@DisplayName("a blank category fails validation")
	void validation() throws Exception {
		Map<String, Object> blankCategory = skill("Rust", " ");
		mockMvc.perform(adminPost("/api/v1/admin/skills", blankCategory))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("category"));
	}

	@Test
	@DisplayName("the admin category lookup lists what has been created, alphabetically")
	void categoryLookup() throws Exception {
		mockMvc.perform(adminPost("/api/v1/admin/skills", skill("Terraform", "DevOps")));
		mockMvc.perform(adminPost("/api/v1/admin/skills", skill("Spring Boot", "Backend")));

		mockMvc.perform(get("/api/v1/admin/skill-categories").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].name").value("Backend"))
				.andExpect(jsonPath("$.data[1].name").value("DevOps"));

		mockMvc.perform(get("/api/v1/admin/skill-categories")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("update can move a skill to another category; delete hides it")
	void updateAndDelete() throws Exception {
		Long id = idOf(mockMvc.perform(adminPost("/api/v1/admin/skills", skill("Docker", "Backend")))
				.andReturn()
				.getResponse()
				.getContentAsString());

		mockMvc.perform(adminPut("/api/v1/admin/skills/" + id, skill("Docker", "DevOps")))
				.andExpect(jsonPath("$.data.category").value("DevOps"));

		mockMvc.perform(delete("/api/v1/admin/skills/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/skills")).andExpect(jsonPath("$.data.length()").value(0));
	}

	private Map<String, Object> skill(String name, String category) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("name", name);
		body.put("category", category);
		return body;
	}

	private Map<String, Object> orderedSkill(String name, String category, int displayOrder) {
		Map<String, Object> body = skill(name, category);
		body.put("displayOrder", displayOrder);
		return body;
	}
}
