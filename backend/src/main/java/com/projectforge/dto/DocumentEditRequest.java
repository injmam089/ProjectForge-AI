package com.projectforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for manually saving updates to a Markdown document.
 */
public record DocumentEditRequest(
        @NotBlank(message = "Document content must not be blank")
        @Size(max = 500000, message = "Document content cannot exceed 500000 characters")
        String content
) {
}
