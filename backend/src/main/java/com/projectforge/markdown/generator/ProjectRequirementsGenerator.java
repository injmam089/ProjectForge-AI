package com.projectforge.markdown.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.GeneratorUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generates PROJECT_REQUIREMENTS.md from the approved project specification.
 */
@Component
public class ProjectRequirementsGenerator implements DocumentContentGenerator {

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.PROJECT_REQUIREMENTS;
    }

    @Override
    public String generateContent(Project project, JsonNode spec) {
        String projectName = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
        String goal = GeneratorUtils.getText(spec.path("project"), "goal", project.getDescription());
        String problem = GeneratorUtils.getText(spec.path("project"), "problem", project.getProblemStatement());

        StringBuilder sb = new StringBuilder();
        sb.append("# Project Requirements: ").append(projectName).append("\n\n");

        sb.append("## 1. Project Overview\n\n");
        sb.append(goal).append("\n\n");

        if (problem != null && !problem.isBlank()) {
            sb.append("## 2. Problem Statement\n\n");
            sb.append(problem).append("\n\n");
        }

        sb.append("## 3. Goals & Objectives\n\n");
        sb.append("- Establish a production-grade software solution addressing specified requirements.\n");
        sb.append("- Adhere strictly to the approved architectural blueprint and data model.\n");
        sb.append("- Maintain clear traceability between approved specifications and implementation.\n\n");

        sb.append("## 4. Target Users & User Roles\n\n");
        JsonNode usersNode = spec.path("users");
        if (usersNode.isArray() && usersNode.size() > 0) {
            for (JsonNode u : usersNode) {
                String role = GeneratorUtils.getText(u, "role", "User Role");
                String desc = GeneratorUtils.getText(u, "description", "");
                sb.append("### ").append(role).append("\n\n");
                sb.append(desc).append("\n\n");

                List<String> resp = GeneratorUtils.getStringList(u, "responsibilities");
                if (!resp.isEmpty()) {
                    sb.append("**Responsibilities**:\n");
                    for (String r : resp) {
                        sb.append("- ").append(r).append("\n");
                    }
                    sb.append("\n");
                }

                List<String> perms = GeneratorUtils.getStringList(u, "permissions");
                if (!perms.isEmpty()) {
                    sb.append("**Permissions**: `").append(String.join("`, `", perms)).append("`\n\n");
                }
            }
        }

        sb.append("## 5. Scope & Functional Requirements\n\n");
        JsonNode featuresNode = spec.path("features");
        if (featuresNode.isArray() && featuresNode.size() > 0) {
            sb.append("The project delivers the following functional capabilities:\n\n");
            for (JsonNode f : featuresNode) {
                String id = GeneratorUtils.getText(f, "id", "FEAT");
                String name = GeneratorUtils.getText(f, "name", "Feature");
                String category = GeneratorUtils.getText(f, "category", "General");
                String priority = GeneratorUtils.getText(f, "priority", "MEDIUM");
                String desc = GeneratorUtils.getText(f, "description", "");

                sb.append("### ").append(id).append(": ").append(name).append("\n\n");
                sb.append("- **Category**: ").append(category).append("\n");
                sb.append("- **Priority**: `").append(priority).append("`\n");
                sb.append("- **Description**: ").append(desc).append("\n\n");
            }
        }

        sb.append("## 6. Modules & Subsystems\n\n");
        JsonNode modulesNode = spec.path("modules");
        if (modulesNode.isArray() && modulesNode.size() > 0) {
            for (JsonNode m : modulesNode) {
                String name = GeneratorUtils.getText(m, "name", "Module");
                String desc = GeneratorUtils.getText(m, "description", "");
                List<String> deps = GeneratorUtils.getStringList(m, "dependencies");
                List<String> resp = GeneratorUtils.getStringList(m, "responsibilities");

                sb.append("### ").append(name).append("\n\n");
                sb.append(desc).append("\n\n");
                if (!deps.isEmpty()) {
                    sb.append("- **Dependencies**: `").append(String.join("`, `", deps)).append("`\n");
                }
                if (!resp.isEmpty()) {
                    sb.append("- **Responsibilities**:\n");
                    for (String r : resp) {
                        sb.append("  - ").append(r).append("\n");
                    }
                }
                sb.append("\n");
            }
        }

        sb.append("## 7. Non-Functional Requirements\n\n");
        List<String> nfrs = GeneratorUtils.getStringList(spec, "nonFunctionalRequirements");
        sb.append(GeneratorUtils.toBulletList(nfrs)).append("\n");

        sb.append("## 8. Constraints & Assumptions\n\n");
        sb.append("### Constraints\n\n");
        List<String> constraints = GeneratorUtils.getStringList(spec, "constraints");
        sb.append(GeneratorUtils.toBulletList(constraints)).append("\n");

        sb.append("### Assumptions\n\n");
        List<String> assumptions = GeneratorUtils.getStringList(spec, "assumptions");
        sb.append(GeneratorUtils.toBulletList(assumptions)).append("\n");

        sb.append("## 9. Definition of Done\n\n");
        sb.append("- All approved functional features are implemented and covered by automated unit/integration tests.\n");
        sb.append("- Architecture conforms strictly to the approved specification.\n");
        sb.append("- Zero high or critical security vulnerabilities present.\n");
        sb.append("- CI/CD build passes and container image builds cleanly.\n");

        return sb.toString();
    }
}
