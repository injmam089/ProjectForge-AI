package com.projectforge.dto;

import com.projectforge.entity.Document;
import com.projectforge.entity.DocumentType;

import java.time.Instant;

/**
 * Full details of a generated Markdown document.
 */
public record DocumentResponse(
        Long id,
        Long projectId,
        String filename,
        DocumentType type,
        String content,
        int version,
        int characterCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static DocumentResponse from(Document doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getProject().getId(),
                doc.getFilename(),
                doc.getType(),
                doc.getContent(),
                doc.getVersion(),
                doc.getContent() != null ? doc.getContent().length() : 0,
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
