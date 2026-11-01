package com.projectforge.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.ProjectStatus;

import java.util.List;

/**
 * Standardized response payload for AI requirements analysis and clarification endpoints.
 */
public record RequirementsAnalysisResponse(
        Long projectId,
        ProjectStatus status,
        int qualityScore,
        JsonNode specification,
        List<OpenQuestionDto> openQuestions,
        String message
) {}
