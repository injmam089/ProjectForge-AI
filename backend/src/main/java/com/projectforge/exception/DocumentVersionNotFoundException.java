package com.projectforge.exception;

/**
 * Thrown when a document version does not exist.
 * Maps to HTTP 404 with zero metadata leakage.
 */
public class DocumentVersionNotFoundException extends ResourceNotFoundException {

    public DocumentVersionNotFoundException(Long documentId, int version) {
        super("Document version " + version + " not found for document id: " + documentId);
    }
}
