package com.projectforge.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for AI-assisted editing of a Markdown document.
 */
public record DocumentAiEditRequest(
        @NotNull(message = "AI action is required")
        DocumentAiAction action,

        @Size(max = 2000, message = "Instruction cannot exceed 2000 characters")
        String instruction
) {
}
