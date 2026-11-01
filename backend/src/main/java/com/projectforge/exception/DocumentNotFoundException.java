package com.projectforge.exception;

/**
 * Thrown when a document does not exist or does not belong to the requesting user.
 * Maps to HTTP 404 with zero metadata leakage.
 */
public class DocumentNotFoundException extends ResourceNotFoundException {

    public DocumentNotFoundException(Long id) {
        super("Document not found with id: " + id);
    }

    public DocumentNotFoundException(String message) {
        super(message);
    }
}
