package com.projectforge.dto;

import com.projectforge.entity.ProjectHistory;
import com.projectforge.entity.ProjectHistoryEventType;

import java.time.Instant;

/**
 * DTO for project history and audit events.
 */
public record ProjectHistoryResponse(
        Long id,
        Long projectId,
        ProjectHistoryEventType eventType,
        String description,
        String metadata,
        String createdBy,
        Instant createdAt
) {
    public static ProjectHistoryResponse fromEntity(ProjectHistory history) {
        return new ProjectHistoryResponse(
                history.getId(),
                history.getProject() != null ? history.getProject().getId() : null,
                history.getEventType(),
                history.getDescription(),
                history.getMetadata(),
                history.getCreatedBy(),
                history.getCreatedAt()
        );
    }
}
