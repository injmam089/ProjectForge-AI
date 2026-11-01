package com.projectforge.markdown;

import com.projectforge.entity.DocumentType;

/**
 * In-memory representation of a generated Markdown document prior to validation and persistence.
 */
public record GeneratedDocument(
        DocumentType type,
        String filename,
        String content
) {
    public GeneratedDocument {
        if (type == null) {
            throw new IllegalArgumentException("DocumentType must not be null");
        }
        if (filename == null || filename.isBlank()) {
            filename = type.getDefaultFilename();
        }
    }

    public static GeneratedDocument of(DocumentType type, String content) {
        return new GeneratedDocument(type, type.getDefaultFilename(), content);
    }
}
