package com.projectforge.dto;

import com.projectforge.entity.ProjectStatus;

import java.util.List;

/**
 * Result payload returned upon successful execution of document generation.
 */
public record DocumentGenerationResponse(
        Long projectId,
        ProjectStatus status,
        int documentsCount,
        List<DocumentSummaryResponse> documents,
        String message
) {
}
