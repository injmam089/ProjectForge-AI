package com.projectforge.dto;

/**
 * Response payload returned after saving a document.
 */
public record DocumentSaveResponse(
        Long id,
        String filename,
        int version,
        int characterCount,
        String message
) {
}
