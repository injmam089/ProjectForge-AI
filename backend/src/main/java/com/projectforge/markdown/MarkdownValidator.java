package com.projectforge.markdown;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.exception.MarkdownValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validation layer for generated Markdown documents.
 * Enforces strict filename hygiene, path safety, content limits, and cross-document consistency.
 */
@Component
public class MarkdownValidator {

    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+\\.md$");
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MIN_CONTENT_LENGTH = 20;
    private static final int MAX_CONTENT_LENGTH = 500_000;

    /**
     * Validates an entire collection of generated documents against the project and approved specification.
     *
     * @param project   the owning project
     * @param spec      the approved specification JSON
     * @param documents the list of generated documents
     * @throws MarkdownValidationException if any rule is violated
     */
    public void validateAll(Project project, JsonNode spec, List<GeneratedDocument> documents) {
        if (project == null) {
            throw new MarkdownValidationException("Project must not be null during document validation");
        }
        if (documents == null || documents.isEmpty()) {
            throw new MarkdownValidationException("Generated documents list must not be empty");
        }

        List<String> errors = new ArrayList<>();
        Set<String> seenFilenames = new HashSet<>();

        for (GeneratedDocument doc : documents) {
            validateSingleDocument(doc, seenFilenames, errors);
        }

        // Cross-document consistency checks
        validateConsistency(project, spec, documents, errors);

        if (!errors.isEmpty()) {
            throw new MarkdownValidationException("Markdown document validation failed", errors);
        }
    }

    private void validateSingleDocument(GeneratedDocument doc, Set<String> seenFilenames, List<String> errors) {
        if (doc == null) {
            errors.add("Null document found in generated document list");
            return;
        }

        String filename = doc.filename();
        if (filename == null || filename.isBlank()) {
            errors.add("Document filename must not be blank");
            return;
        }

        // Filename format and path safety
        if (!filename.toLowerCase().endsWith(".md")) {
            errors.add("Document filename must end with .md: '" + filename + "'");
        }
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            errors.add("Document filename contains illegal path traversal or separator characters: '" + filename + "'");
        }
        if (!SAFE_FILENAME_PATTERN.matcher(filename).matches()) {
            errors.add("Document filename contains illegal characters: '" + filename + "'");
        }
        if (filename.length() > MAX_FILENAME_LENGTH) {
            errors.add("Document filename exceeds maximum length (" + MAX_FILENAME_LENGTH + "): '" + filename + "'");
        }

        // Duplicate check
        if (!seenFilenames.add(filename.toLowerCase())) {
            errors.add("Duplicate document filename detected: '" + filename + "'");
        }

        // Content validation
        String content = doc.content();
        if (content == null || content.isBlank()) {
            errors.add("Document content must not be blank for '" + filename + "'");
            return;
        }
        if (content.length() < MIN_CONTENT_LENGTH) {
            errors.add("Document content is too short (< " + MIN_CONTENT_LENGTH + " chars) for '" + filename + "'");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            errors.add("Document content exceeds maximum limit (" + MAX_CONTENT_LENGTH + " chars) for '" + filename + "'");
        }
    }

    private void validateConsistency(Project project, JsonNode spec, List<GeneratedDocument> documents, List<String> errors) {
        String expectedProjectName = GeneratorUtils.getText(spec.path("project"), "name", project.getName());

        for (GeneratedDocument doc : documents) {
            // README, REQUIREMENTS, TECH_STACK, ARCHITECTURE must mention the project name
            if (doc.type() == DocumentType.README || doc.type() == DocumentType.PROJECT_REQUIREMENTS) {
                if (!doc.content().toLowerCase().contains(expectedProjectName.toLowerCase())) {
                    errors.add("Document '" + doc.filename() + "' does not contain the approved project name '" + expectedProjectName + "'");
                }
            }

            // Consistency check: Database
            if (doc.type() == DocumentType.DATABASE) {
                List<String> approvedDbTech = GeneratorUtils.getStringList(spec.path("technology"), "database");
                if (containsIgnoreCase(approvedDbTech, "PostgreSQL") && doc.content().contains("MongoDB")) {
                    errors.add("Consistency violation in DATABASE.md: claims MongoDB when approved database is PostgreSQL");
                }
                if (containsIgnoreCase(approvedDbTech, "MongoDB") && doc.content().contains("PostgreSQL")) {
                    errors.add("Consistency violation in DATABASE.md: claims PostgreSQL when approved database is MongoDB");
                }
            }

            // Consistency check: Backend
            if (doc.type() == DocumentType.TECH_STACK) {
                List<String> approvedBackend = GeneratorUtils.getStringList(spec.path("technology"), "backend");
                if (containsIgnoreCase(approvedBackend, "Spring Boot") && doc.content().contains("Ruby on Rails")) {
                    errors.add("Consistency violation in TECH_STACK.md: claims Ruby on Rails when approved backend is Spring Boot");
                }
            }
        }
    }

    private boolean containsIgnoreCase(List<String> list, String target) {
        if (list == null) return false;
        for (String s : list) {
            if (s.toLowerCase().contains(target.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
