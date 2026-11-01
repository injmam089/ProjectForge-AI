package com.projectforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object representing a user's answer to a specific open clarification question.
 */
public record ClarificationAnswerDto(
        @NotBlank(message = "Question ID is required")
        @Size(max = 50, message = "Question ID cannot exceed 50 characters")
        String questionId,

        @NotBlank(message = "Answer cannot be blank")
        @Size(max = 3000, message = "Answer cannot exceed 3000 characters")
        String answer
) {}
