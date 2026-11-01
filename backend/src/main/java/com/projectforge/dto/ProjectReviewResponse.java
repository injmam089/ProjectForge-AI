package com.projectforge.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.ProjectStatus;

import java.util.List;

/**
 * Standardized response payload for human-in-the-loop review of project requirements.
 */
public record ProjectReviewResponse(
        Long projectId,
        ProjectStatus status,
        int qualityScore,
        QualityScoreDto qualityDetails,
        JsonNode specification,
        List<OpenQuestionDto> openQuestions,
        List<OpenQuestionDto> blockingQuestions,
        boolean approved,
        boolean canApprove,
        String message
) {}
