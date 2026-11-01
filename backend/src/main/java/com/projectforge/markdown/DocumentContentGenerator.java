package com.projectforge.markdown;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;

/**
 * Strategy interface for generating Markdown content for a specific document type.
 * Generators strictly consume the approved specification JSON and never invent unsupported facts.
 */
public interface DocumentContentGenerator {

    /**
     * The document type supported by this generator.
     */
    DocumentType getDocumentType();

    /**
     * Generates Markdown content derived strictly from the approved project specification.
     *
     * @param project the project entity
     * @param spec    the parsed approved JSON specification (single source of truth)
     * @return non-empty Markdown content
     */
    String generateContent(Project project, JsonNode spec);
}
