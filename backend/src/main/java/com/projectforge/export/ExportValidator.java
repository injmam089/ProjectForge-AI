package com.projectforge.export;

import com.projectforge.entity.Document;
import com.projectforge.entity.Project;
import com.projectforge.exception.InvalidExportException;
import com.projectforge.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates project ownership, document limits, filename integrity, and payload size bounds before ZIP creation.
 */
@Component
public class ExportValidator {

    private static final Logger log = LoggerFactory.getLogger(ExportValidator.class);
    private static final Pattern FILENAME_PATTERN = Pattern.compile(ExportConstants.FILENAME_REGEX);

    /**
     * Verifies project existence and strict user ownership.
     * Enforces anti-IDOR defense: returns ResourceNotFoundException (HTTP 404) on unauthorized access.
     */
    public void validateProject(Project project, String userEmail) {
        if (project == null) {
            throw new ResourceNotFoundException("Project not found");
        }
        if (userEmail == null || project.getUser() == null ||
                !project.getUser().getEmail().equalsIgnoreCase(userEmail.trim())) {
            // Anti-IDOR: Always return 404 with zero metadata leakage
            throw new ResourceNotFoundException("Project", "id", project.getId());
        }
    }

    /**
     * Validates that the active document set is safe to package.
     */
    public void validateDocuments(List<Document> documents, Long projectId) {
        if (documents == null || documents.isEmpty()) {
            throw new InvalidExportException("Project has no documents available for export.");
        }

        if (documents.size() > ExportConstants.MAX_DOCUMENT_COUNT) {
            throw new InvalidExportException("Project exceeds maximum document count limit ("
                    + documents.size() + " > " + ExportConstants.MAX_DOCUMENT_COUNT + ").");
        }

        Set<String> seenFilenames = new HashSet<>();
        long totalBytes = 0;

        for (Document doc : documents) {
            if (doc == null) {
                throw new InvalidExportException("Null document encountered in export list.");
            }

            if (doc.getProject() == null || !projectId.equals(doc.getProject().getId())) {
                throw new InvalidExportException("Document does not belong to project id: " + projectId);
            }

            String filename = doc.getFilename();
            if (filename == null || filename.isBlank()) {
                throw new InvalidExportException("Document filename must not be blank.");
            }

            if (filename.length() > ExportConstants.MAX_FILENAME_LENGTH) {
                throw new InvalidExportException("Document filename exceeds maximum length of "
                        + ExportConstants.MAX_FILENAME_LENGTH + ": " + filename);
            }

            // Path traversal and absolute path defenses
            if (filename.contains("/") || filename.contains("\\") || filename.contains("..") || filename.contains(":")) {
                throw new InvalidExportException("Document filename contains illegal path traversal characters: " + filename);
            }

            if (!FILENAME_PATTERN.matcher(filename).matches()) {
                throw new InvalidExportException("Document filename must match pattern [a-zA-Z0-9_-]+\\.md: " + filename);
            }

            if (!seenFilenames.add(filename.toLowerCase())) {
                throw new InvalidExportException("Duplicate document filename detected in export package: " + filename);
            }

            String content = doc.getContent();
            if (content == null) {
                throw new InvalidExportException("Document content is null for: " + filename);
            }

            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            if (contentBytes.length > ExportConstants.MAX_SINGLE_DOCUMENT_SIZE) {
                throw new InvalidExportException("Document '" + filename + "' size (" + contentBytes.length
                        + " bytes) exceeds maximum single document limit of " + ExportConstants.MAX_SINGLE_DOCUMENT_SIZE + " bytes.");
            }

            totalBytes += contentBytes.length;
        }

        if (totalBytes > ExportConstants.MAX_TOTAL_EXPORT_SIZE) {
            throw new InvalidExportException("Total export package content size (" + totalBytes
                    + " bytes) exceeds maximum limit of " + ExportConstants.MAX_TOTAL_EXPORT_SIZE + " bytes.");
        }
    }
}
