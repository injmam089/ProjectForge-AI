package com.projectforge.dto;

import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.ProjectType;

import java.time.Instant;

/**
 * Lightweight project summary representation for dashboards and listings.
 */
public record ProjectSummaryResponse(
        Long id,
        String name,
        String description,
        ProjectType projectType,
        ProjectDifficulty difficulty,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectSummaryResponse fromEntity(Project project) {
        return new ProjectSummaryResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getProjectType(),
                project.getDifficulty(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    // Alias for description
    public String projectIdea() {
        return description;
    }
}
