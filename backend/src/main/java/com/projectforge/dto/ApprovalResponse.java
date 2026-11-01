package com.projectforge.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.ProjectStatus;

/**
 * Response payload returned after successful human-in-the-loop requirement approval.
 */
public record ApprovalResponse(
        Long projectId,
        ProjectStatus status,
        boolean approved,
        int qualityScore,
        String message,
        JsonNode specification
) {}
