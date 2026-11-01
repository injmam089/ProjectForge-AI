package com.projectforge.dto;

import com.projectforge.entity.ProjectStatus;

import java.util.List;

/**
 * Workspace overview response returning project information and document manifest.
 */
public record WorkspaceResponse(
        Long projectId,
        String projectName,
        ProjectStatus status,
        List<DocumentSummaryResponse> documents
) {
}
