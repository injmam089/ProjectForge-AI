package com.projectforge.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating a project's structured specification.
 */
public record ProjectSpecificationUpdateRequest(
        @NotNull(message = "Specification payload cannot be null")
        JsonNode specification
) {
}
