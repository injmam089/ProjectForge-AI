package com.projectforge.exception;

/**
 * Exception thrown when project domain data fails validation rules (e.g. during prepare-analysis).
 * Maps to HTTP 400 Bad Request.
 */
public class ProjectValidationException extends ProjectForgeException {

    public ProjectValidationException(String message) {
        super(message);
    }
}
