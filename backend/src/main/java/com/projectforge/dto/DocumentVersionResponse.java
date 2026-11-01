package com.projectforge.dto;

import com.projectforge.entity.DocumentChangeType;
import com.projectforge.entity.DocumentVersion;

import java.time.Instant;

/**
 * Detailed view of a historical document version including full content.
 */
public record DocumentVersionResponse(
        Long id,
        Long documentId,
        int version,
        String filename,
        String content,
        DocumentChangeType changeType,
        String instruction,
        Long createdBy,
        Instant createdAt
) {
    public static DocumentVersionResponse from(DocumentVersion v) {
        return new DocumentVersionResponse(
                v.getId(),
                v.getDocument() != null ? v.getDocument().getId() : null,
                v.getVersion(),
                v.getDocument() != null ? v.getDocument().getFilename() : null,
                v.getContent(),
                v.getChangeType(),
                v.getInstruction(),
                v.getCreatedBy() != null ? v.getCreatedBy().getId() : null,
                v.getCreatedAt()
        );
    }
}
