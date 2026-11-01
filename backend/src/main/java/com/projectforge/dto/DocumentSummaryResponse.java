package com.projectforge.dto;

import com.projectforge.entity.Document;
import com.projectforge.entity.DocumentType;

import java.time.Instant;

/**
 * Summary view of a generated Markdown document for lists and manifests.
 */
public record DocumentSummaryResponse(
        Long id,
        Long projectId,
        String filename,
        DocumentType type,
        String description,
        int version,
        int characterCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static DocumentSummaryResponse from(Document doc) {
        return new DocumentSummaryResponse(
                doc.getId(),
                doc.getProject().getId(),
                doc.getFilename(),
                doc.getType(),
                doc.getType() != null ? doc.getType().getDescription() : "",
                doc.getVersion(),
                doc.getContent() != null ? doc.getContent().length() : 0,
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
