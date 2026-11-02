package com.projectforge.dto;

import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.ProjectType;

import java.time.Instant;

/**
 * Lightweight project summary representation for dashboards and listings.
 * Includes document counts and quality scores when available.
 */
public record ProjectSummaryResponse(
        Long id,
        String name,
        String description,
        ProjectType projectType,
        ProjectDifficulty difficulty,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt,
        int documentCount,
        Integer qualityScore
) {
    /**
     * Backward-compatible 8-argument constructor defaulting documentCount to 0 and qualityScore to null.
     */
    public ProjectSummaryResponse(
            Long id,
            String name,
            String description,
            ProjectType projectType,
            ProjectDifficulty difficulty,
            ProjectStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, name, description, projectType, difficulty, status, createdAt, updatedAt, 0, null);
    }

    public static ProjectSummaryResponse fromEntity(Project project) {
        return fromEntity(project, 0, null);
    }

    public static ProjectSummaryResponse fromEntity(Project project, int documentCount, Integer qualityScore) {
        return new ProjectSummaryResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getProjectType(),
                project.getDifficulty(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                documentCount,
                qualityScore
        );
    }

    // Alias for description
    public String projectIdea() {
        return description;
    }
}
