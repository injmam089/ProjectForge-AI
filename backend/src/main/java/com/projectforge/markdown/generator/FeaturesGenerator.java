package com.projectforge.markdown.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.GeneratorUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates FEATURES.md from the approved project specification.
 */
@Component
public class FeaturesGenerator implements DocumentContentGenerator {

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.FEATURES;
    }

    @Override
    public String generateContent(Project project, JsonNode spec) {
        String projectName = GeneratorUtils.getText(spec.path("project"), "name", project.getName());

        StringBuilder sb = new StringBuilder();
        sb.append("# Features Specification: ").append(projectName).append("\n\n");
        sb.append("This document classifies and details all functional requirements established in the approved specification.\n\n");

        List<JsonNode> coreFeatures = new ArrayList<>();
        List<JsonNode> secondaryFeatures = new ArrayList<>();
        List<JsonNode> optionalFeatures = new ArrayList<>();

        JsonNode featuresNode = spec.path("features");
        if (featuresNode.isArray()) {
            for (JsonNode f : featuresNode) {
                String priority = GeneratorUtils.getText(f, "priority", "MEDIUM").toUpperCase();
                if ("CRITICAL".equals(priority) || "HIGH".equals(priority)) {
                    coreFeatures.add(f);
                } else if ("MEDIUM".equals(priority)) {
                    secondaryFeatures.add(f);
                } else {
                    optionalFeatures.add(f);
                }
            }
        }

        renderFeatureSection(sb, "1. Core Features (Critical & High Priority)", coreFeatures);
        renderFeatureSection(sb, "2. Secondary Features (Medium Priority)", secondaryFeatures);
        renderFeatureSection(sb, "3. Optional & Supporting Features (Low Priority)", optionalFeatures);

        sb.append("## 4. Future Enhancements\n\n");
        sb.append("- Specialized capabilities and advanced optimizations planned for subsequent iterations.\n");
        sb.append("- Community and user feedback-driven enhancements.\n");

        return sb.toString();
    }

    private void renderFeatureSection(StringBuilder sb, String title, List<JsonNode> features) {
        sb.append("## ").append(title).append("\n\n");
        if (features.isEmpty()) {
            sb.append("_No features in this priority tier in the approved specification._\n\n");
            return;
        }

        for (JsonNode f : features) {
            String id = GeneratorUtils.getText(f, "id", "FEAT");
            String name = GeneratorUtils.getText(f, "name", "Feature");
            String category = GeneratorUtils.getText(f, "category", "Core");
            String priority = GeneratorUtils.getText(f, "priority", "MEDIUM");
            String description = GeneratorUtils.getText(f, "description", "");

            sb.append("### `").append(id).append("`: ").append(name).append("\n\n");
            sb.append("- **Category**: `").append(category).append("`\n");
            sb.append("- **Priority**: `").append(priority).append("`\n");
            sb.append("- **Description**: ").append(description).append("\n\n");
        }
    }
}
