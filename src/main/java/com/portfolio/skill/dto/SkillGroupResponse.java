package com.portfolio.skill.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The public Skills page renders one block per category, so the grouping is done server-side
 * rather than leaving every client to re-derive it (docs/07-api/endpoints.md: "grouped by
 * category in the response").
 */
@Schema(description = "Published skills of one category, in display order")
public record SkillGroupResponse(String category, List<SkillResponse> skills) {
}
