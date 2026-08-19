package com.portfolio.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Whole-list replace: what is sent becomes the complete set of links, in the order sent. */
@Schema(description = "The complete ordered list of social links")
public record SocialLinksUpdateRequest(@NotNull @Valid List<SocialLinkRequest> links) {
}
