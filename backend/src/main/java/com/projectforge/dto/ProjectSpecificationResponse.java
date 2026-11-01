package com.projectforge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectStatus;

import java.time.Instant;

/**
 * Structured project specification response payload.
 * Provides the JSON specification representing the single source of truth for the project.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectSpecificationResponse(
        Long projectId,
        String projectName,
        ProjectStatus status,
        JsonNode specification,
        Instant updatedAt
) {
    public ProjectSpecificationResponse(Long projectId, String projectName, ProjectStatus status, JsonNode specification) {
        this(projectId, projectName, status, specification, Instant.now());
    }

    public static ProjectSpecificationResponse of(Project project, JsonNode specification) {
        return new ProjectSpecificationResponse(
                project.getId(),
                project.getName(),
                project.getStatus(),
                specification,
                project.getUpdatedAt()
        );
    }
}
