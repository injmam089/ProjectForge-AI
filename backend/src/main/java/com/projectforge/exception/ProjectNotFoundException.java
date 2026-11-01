package com.projectforge.exception;

/**
 * Exception thrown when a project is not found or does not belong to the authenticated user.
 * Maps to HTTP 404 Not Found to prevent IDOR enumeration.
 */
public class ProjectNotFoundException extends ResourceNotFoundException {

    public ProjectNotFoundException(Long id) {
        super("Project", "id", id);
    }
}
