package com.projectforge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request payload containing answers to one or more open clarification questions.
 */
public record ClarificationRequest(
        @NotEmpty(message = "Answers list cannot be empty")
        @Valid
        List<ClarificationAnswerDto> answers
) {}
