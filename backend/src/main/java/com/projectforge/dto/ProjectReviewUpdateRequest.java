package com.projectforge.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating and reviewing a project specification during human review.
 */
public record ProjectReviewUpdateRequest(
        @NotNull(message = "Specification payload cannot be null")
        JsonNode specification
) {}
