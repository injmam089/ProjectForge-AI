package com.projectforge.markdown;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deterministic document planner for ProjectForge AI.
 * Inspects the approved project specification JSON and project type to plan
 * the exact list of applicable standard and specialized Markdown documents.
 */
@Component
public class DocumentPlanner {

    /**
     * Plans the ordered collection of documents to generate for an approved project.
     */
    public List<DocumentType> plan(Project project, JsonNode spec) {
        if (project == null) {
            throw new IllegalArgumentException("Project must not be null");
        }
        if (spec == null || spec.isNull()) {
            throw new IllegalArgumentException("Approved specification must not be null");
        }

        List<DocumentType> planned = new ArrayList<>();

        // 1. Core Standard Documents
        planned.add(DocumentType.README);
        planned.add(DocumentType.PROJECT_REQUIREMENTS);
        planned.add(DocumentType.PROJECT_GOALS);
        planned.add(DocumentType.FEATURES);
        planned.add(DocumentType.TECH_STACK);
        planned.add(DocumentType.ARCHITECTURE);

        // 2. Database Documentation (if database entities or database tech present)
        if (hasDatabaseDefinition(project, spec)) {
            planned.add(DocumentType.DATABASE);
        }

        // 3. API Specification (if REST APIs defined or API-centric project)
        if (hasApiDefinition(project, spec)) {
            planned.add(DocumentType.API_SPECIFICATION);
        }

        // 4. Security Policy
        planned.add(DocumentType.SECURITY);

        // 5. UI/UX Requirements (if frontend / client application)
        if (hasUserInterface(project, spec)) {
            planned.add(DocumentType.UI_UX_REQUIREMENTS);
        }

        // 6. Implementation, Testing & AI Guidance
        planned.add(DocumentType.DEVELOPMENT_PLAN);
        planned.add(DocumentType.TESTING);
        planned.add(DocumentType.DEPLOYMENT);
        planned.add(DocumentType.AI_BUILD_INSTRUCTIONS);

        // 7. Specialized Documents for AI / Machine Learning Projects
        if (isAiMlProject(project, spec)) {
            planned.add(DocumentType.MODEL_REQUIREMENTS);
            planned.add(DocumentType.DATASET);
            planned.add(DocumentType.ML_PIPELINE);
        }

        // 8. Specialized Documents for IoT / Embedded Projects
        if (isIotProject(project, spec)) {
            planned.add(DocumentType.HARDWARE_REQUIREMENTS);
            planned.add(DocumentType.SENSOR_SPECIFICATION);
            planned.add(DocumentType.IOT_PROTOCOLS);
        }

        // 9. Specialized Documents for Cybersecurity Projects
        if (isCybersecurityProject(project, spec)) {
            planned.add(DocumentType.THREAT_MODEL);
            planned.add(DocumentType.SECURITY_TESTING);
        }

        // 10. Specialized Documents for Mobile Projects
        if (isMobileProject(project, spec)) {
            planned.add(DocumentType.MOBILE_REQUIREMENTS);
        }

        return Collections.unmodifiableList(planned);
    }

    private boolean hasDatabaseDefinition(Project project, JsonNode spec) {
        JsonNode dbNode = spec.path("database");
        if (dbNode.isObject() && dbNode.has("entities") && dbNode.path("entities").isArray() && dbNode.path("entities").size() > 0) {
            return true;
        }
        JsonNode techDb = spec.path("technology").path("database");
        return techDb.isArray() && techDb.size() > 0;
    }

    private boolean hasApiDefinition(Project project, JsonNode spec) {
        JsonNode apisNode = spec.path("apis");
        if (apisNode.isArray() && apisNode.size() > 0) {
            return true;
        }
        ProjectType type = project.getProjectType();
        return type == ProjectType.API_BACKEND || type == ProjectType.WEB_APPLICATION || type == ProjectType.FULL_STACK_APPLICATION;
    }

    private boolean hasUserInterface(Project project, JsonNode spec) {
        ProjectType type = project.getProjectType();
        if (type == ProjectType.WEB_APPLICATION || type == ProjectType.MOBILE_APPLICATION ||
                type == ProjectType.DESKTOP_APPLICATION || type == ProjectType.FULL_STACK_APPLICATION) {
            return true;
        }
        JsonNode frontendTech = spec.path("technology").path("frontend");
        return frontendTech.isArray() && frontendTech.size() > 0;
    }

    private boolean isAiMlProject(Project project, JsonNode spec) {
        if (project.getProjectType() == ProjectType.AI_ML_PROJECT || project.getProjectType() == ProjectType.DATA_ANALYTICS) {
            return true;
        }
        JsonNode aiTech = spec.path("technology").path("ai");
        return aiTech.isArray() && aiTech.size() > 0;
    }

    private boolean isIotProject(Project project, JsonNode spec) {
        return project.getProjectType() == ProjectType.IOT_PROJECT;
    }

    private boolean isCybersecurityProject(Project project, JsonNode spec) {
        return project.getProjectType() == ProjectType.CYBERSECURITY_PROJECT;
    }

    private boolean isMobileProject(Project project, JsonNode spec) {
        return project.getProjectType() == ProjectType.MOBILE_APPLICATION;
    }
}
