package com.projectforge.dto;

import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new project.
 * Supports backward compatibility with Phase 1/2 callers.
 */
public record CreateProjectRequest(
        @NotBlank(message = "Project name is required")
        @Size(min = 2, max = 150, message = "Project name must be between 2 and 150 characters")
        String name,

        @NotBlank(message = "Project description is required")
        @Size(max = 5000, message = "Project description cannot exceed 5000 characters")
        String projectIdea,

        @Size(max = 5000, message = "Problem statement cannot exceed 5000 characters")
        String problemStatement,

        @Size(max = 2000, message = "Target users cannot exceed 2000 characters")
        String targetUsers,

        ProjectType projectType,

        ProjectDifficulty difficulty,

        @Size(max = 2000, message = "Preferred tech stack cannot exceed 2000 characters")
        String preferredTechStack,

        @Size(max = 5000, message = "Special requirements cannot exceed 5000 characters")
        String specialRequirements
) {
    public ProjectCreateRequest toProjectCreateRequest() {
        return new ProjectCreateRequest(
                name,
                projectIdea,
                problemStatement,
                targetUsers,
                projectType != null ? projectType : ProjectType.WEB_APPLICATION,
                difficulty != null ? difficulty : ProjectDifficulty.INTERMEDIATE,
                preferredTechStack,
                specialRequirements
        );
    }
}
