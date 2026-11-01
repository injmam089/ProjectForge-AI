package com.projectforge.dto;

/**
 * Data transfer object representing an open clarification question identified by the AI.
 */
public record OpenQuestionDto(
        String id,
        String question,
        String importance,
        String reason
) {}
