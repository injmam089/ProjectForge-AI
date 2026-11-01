package com.projectforge.dto;

import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.ProjectType;

import java.time.Instant;

/**
 * Detailed project response payload.
 * Exposes sanitized project fields, owner metadata, and lifecycle status.
 */
public record ProjectResponse(
        Long id,
        String name,
        String description,
        String problemStatement,
        String targetUsers,
        ProjectType projectType,
        ProjectDifficulty difficulty,
        String preferredTechStack,
        String specialRequirements,
        ProjectStatus status,
        Long ownerId,
        String ownerName,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse fromEntity(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getProblemStatement(),
                project.getTargetUsers(),
                project.getProjectType(),
                project.getDifficulty(),
                project.getPreferredTechStack(),
                project.getSpecialRequirements(),
                project.getStatus(),
                project.getUser() != null ? project.getUser().getId() : null,
                project.getUser() != null ? project.getUser().getName() : null,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    // Alias for description
    public String projectIdea() {
        return description;
    }
}
