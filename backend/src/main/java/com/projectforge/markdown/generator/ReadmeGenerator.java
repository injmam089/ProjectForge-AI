package com.projectforge.markdown.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.GeneratorUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generates README.md from the approved project specification.
 */
@Component
public class ReadmeGenerator implements DocumentContentGenerator {

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.README;
    }

    @Override
    public String generateContent(Project project, JsonNode spec) {
        String projectName = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
        String goal = GeneratorUtils.getText(spec.path("project"), "goal", project.getDescription());
        String problem = GeneratorUtils.getText(spec.path("project"), "problem", project.getProblemStatement());
        String projectType = GeneratorUtils.getText(spec.path("project"), "type", project.getProjectType().name());
        String difficulty = GeneratorUtils.getText(spec.path("project"), "difficulty", project.getDifficulty().name());

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(projectName).append("\n\n");

        sb.append("## Overview\n\n");
        sb.append(goal).append("\n\n");
        sb.append("- **Project Type**: `").append(projectType).append("`\n");
        sb.append("- **Architectural Complexity**: `").append(difficulty).append("`\n\n");

        if (problem != null && !problem.isBlank()) {
            sb.append("## Problem Statement\n\n");
            sb.append(problem).append("\n\n");
        }

        sb.append("## Goals\n\n");
        sb.append("- Deliver an enterprise-grade ").append(projectName).append(" adhering strictly to approved specifications.\n");
        sb.append("- Solve stated domain constraints with validated architectural patterns.\n\n");

        // Target Users
        sb.append("## Target Users\n\n");
        JsonNode usersNode = spec.path("users");
        if (usersNode.isArray() && usersNode.size() > 0) {
            for (JsonNode u : usersNode) {
                String role = GeneratorUtils.getText(u, "role", "User");
                String desc = GeneratorUtils.getText(u, "description", "");
                sb.append("- **").append(role).append("**: ").append(desc).append("\n");
            }
            sb.append("\n");
        } else if (project.getTargetUsers() != null && !project.getTargetUsers().isBlank()) {
            sb.append(project.getTargetUsers()).append("\n\n");
        } else {
            sb.append("End-users and domain operators as defined in requirements.\n\n");
        }

        // Features
        sb.append("## Key Features\n\n");
        JsonNode featuresNode = spec.path("features");
        if (featuresNode.isArray() && featuresNode.size() > 0) {
            sb.append("| Feature ID | Name | Category | Priority | Description |\n");
            sb.append("| :--- | :--- | :--- | :--- | :--- |\n");
            for (JsonNode f : featuresNode) {
                String id = GeneratorUtils.getText(f, "id", "FEAT");
                String name = GeneratorUtils.getText(f, "name", "Feature");
                String category = GeneratorUtils.getText(f, "category", "Core");
                String priority = GeneratorUtils.getText(f, "priority", "MEDIUM");
                String description = GeneratorUtils.getText(f, "description", "").replace("\n", " ");
                sb.append("| `").append(id).append("` | ").append(name).append(" | ")
                        .append(category).append(" | `").append(priority).append("` | ")
                        .append(description).append(" |\n");
            }
            sb.append("\n");
        } else {
            sb.append("Features detailed in [FEATURES.md](./FEATURES.md).\n\n");
        }

        // Technology Stack
        sb.append("## Technology Stack\n\n");
        JsonNode techNode = spec.path("technology");
        List<String> frontend = GeneratorUtils.getStringList(techNode, "frontend");
        List<String> backend = GeneratorUtils.getStringList(techNode, "backend");
        List<String> database = GeneratorUtils.getStringList(techNode, "database");
        List<String> deployment = GeneratorUtils.getStringList(techNode, "deployment");

        if (!frontend.isEmpty()) sb.append("- **Frontend**: ").append(String.join(", ", frontend)).append("\n");
        if (!backend.isEmpty()) sb.append("- **Backend**: ").append(String.join(", ", backend)).append("\n");
        if (!database.isEmpty()) sb.append("- **Database**: ").append(String.join(", ", database)).append("\n");
        if (!deployment.isEmpty()) sb.append("- **Deployment**: ").append(String.join(", ", deployment)).append("\n");
        sb.append("\nFor complete stack details and rationale, refer to [TECH_STACK.md](./TECH_STACK.md).\n\n");

        // Architecture
        sb.append("## Architecture\n\n");
        String archStyle = GeneratorUtils.getText(spec.path("architecture"), "style", "Modular Architecture");
        sb.append("- **Style**: ").append(archStyle).append("\n");
        List<String> components = GeneratorUtils.getStringList(spec.path("architecture"), "components");
        if (!components.isEmpty()) {
            sb.append("- **Core Components**: ").append(String.join(", ", components)).append("\n");
        }
        sb.append("\nDetailed component topologies and data flows are documented in [ARCHITECTURE.md](./ARCHITECTURE.md).\n\n");

        // Security
        sb.append("## Security\n\n");
        String auth = GeneratorUtils.getText(spec.path("security"), "authentication", "Token-based authentication");
        String authz = GeneratorUtils.getText(spec.path("security"), "authorization", "Role-Based Access Control");
        sb.append("- **Authentication**: ").append(auth).append("\n");
        sb.append("- **Authorization**: ").append(authz).append("\n");
        sb.append("\nSee [SECURITY.md](./SECURITY.md) for complete security controls and audit requirements.\n\n");

        // Testing
        sb.append("## Testing\n\n");
        List<String> unitTests = GeneratorUtils.getStringList(spec.path("testing"), "unitTesting");
        List<String> intTests = GeneratorUtils.getStringList(spec.path("testing"), "integrationTesting");
        if (!unitTests.isEmpty()) sb.append("- **Unit Testing**: ").append(String.join(", ", unitTests)).append("\n");
        if (!intTests.isEmpty()) sb.append("- **Integration Testing**: ").append(String.join(", ", intTests)).append("\n");
        sb.append("\nSee [TESTING.md](./TESTING.md) for test execution workflows.\n\n");

        // Deployment
        sb.append("## Deployment\n\n");
        String env = GeneratorUtils.getText(spec.path("deployment"), "environment", "Containerized environment");
        sb.append("- **Target Environment**: ").append(env).append("\n");
        sb.append("\nRefer to [DEPLOYMENT.md](./DEPLOYMENT.md) for infrastructure and environment setup.\n\n");

        // Development Plan
        sb.append("## Development Plan\n\n");
        sb.append("Implementation follows structured phases detailed in [DEVELOPMENT_PLAN.md](./DEVELOPMENT_PLAN.md).\n\n");

        // AI Guidance
        sb.append("## AI Build Instructions\n\n");
        sb.append("Engineers and AI coding assistants must consult [AI_BUILD_INSTRUCTIONS.md](./AI_BUILD_INSTRUCTIONS.md) prior to writing code.\n");

        return sb.toString();
    }
}
