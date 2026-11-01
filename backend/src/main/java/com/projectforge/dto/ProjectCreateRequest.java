package com.projectforge.dto;

import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Validated request payload for creating a new Project.
 */
public record ProjectCreateRequest(
        @NotBlank(message = "Project name is required")
        @Size(min = 2, max = 150, message = "Project name must be between 2 and 150 characters")
        String name,

        @NotBlank(message = "Project description is required")
        @Size(max = 5000, message = "Project description cannot exceed 5000 characters")
        String description,

        @Size(max = 5000, message = "Problem statement cannot exceed 5000 characters")
        String problemStatement,

        @Size(max = 2000, message = "Target users cannot exceed 2000 characters")
        String targetUsers,

        @NotNull(message = "Project type is required")
        ProjectType projectType,

        @NotNull(message = "Project difficulty is required")
        ProjectDifficulty difficulty,

        @Size(max = 2000, message = "Preferred tech stack cannot exceed 2000 characters")
        String preferredTechStack,

        @Size(max = 5000, message = "Special requirements cannot exceed 5000 characters")
        String specialRequirements
) {}
