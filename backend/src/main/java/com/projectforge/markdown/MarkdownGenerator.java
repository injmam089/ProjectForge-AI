package com.projectforge.markdown;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Dispatcher and coordinator for deterministic Markdown document generation.
 * Maps planned document types to dedicated generators consuming the approved specification JSON.
 */
@Component
public class MarkdownGenerator {

    private final Map<DocumentType, DocumentContentGenerator> generators = new EnumMap<>(DocumentType.class);

    public MarkdownGenerator(List<DocumentContentGenerator> generatorList) {
        if (generatorList != null) {
            for (DocumentContentGenerator gen : generatorList) {
                this.generators.put(gen.getDocumentType(), gen);
            }
        }
    }

    /**
     * Generates all planned Markdown documents for the given project.
     *
     * @param project      the approved Project
     * @param spec         the parsed approved specification JSON
     * @param plannedTypes the ordered list of planned document types
     * @return list of generated documents
     */
    public List<GeneratedDocument> generate(Project project, JsonNode spec, List<DocumentType> plannedTypes) {
        if (project == null || spec == null || plannedTypes == null) {
            throw new IllegalArgumentException("Project, specification, and plannedTypes must not be null");
        }

        List<GeneratedDocument> results = new ArrayList<>();
        for (DocumentType type : plannedTypes) {
            DocumentContentGenerator generator = generators.get(type);
            if (generator == null) {
                throw new IllegalStateException("No generator registered for document type: " + type);
            }
            String content = generator.generateContent(project, spec);
            results.add(new GeneratedDocument(type, type.getDefaultFilename(), content));
        }

        return results;
    }

    /**
     * Generates a single Markdown document from the approved specification.
     * Reuses the identical canonical generator registered for this document type.
     *
     * @param project the approved Project
     * @param spec    the parsed approved specification JSON
     * @param type    the target document type
     * @return the generated document
     */
    public GeneratedDocument generateSingle(Project project, JsonNode spec, DocumentType type) {
        if (project == null || spec == null || type == null) {
            throw new IllegalArgumentException("Project, specification, and type must not be null");
        }
        DocumentContentGenerator generator = generators.get(type);
        if (generator == null) {
            throw new IllegalStateException("No generator registered for document type: " + type);
        }
        String content = generator.generateContent(project, spec);
        return new GeneratedDocument(type, type.getDefaultFilename(), content);
    }
}
