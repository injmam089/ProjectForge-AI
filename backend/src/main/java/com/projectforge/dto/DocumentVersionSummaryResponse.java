package com.projectforge.dto;

import com.projectforge.entity.DocumentChangeType;
import com.projectforge.entity.DocumentVersion;

import java.time.Instant;

/**
 * Summary view of a document version revision for history listing.
 */
public record DocumentVersionSummaryResponse(
        Long id,
        Long documentId,
        int version,
        DocumentChangeType changeType,
        String instruction,
        Long createdBy,
        Instant createdAt
) {
    public static DocumentVersionSummaryResponse from(DocumentVersion v) {
        return new DocumentVersionSummaryResponse(
                v.getId(),
                v.getDocument() != null ? v.getDocument().getId() : null,
                v.getVersion(),
                v.getChangeType(),
                v.getInstruction(),
                v.getCreatedBy() != null ? v.getCreatedBy().getId() : null,
                v.getCreatedAt()
        );
    }
}
