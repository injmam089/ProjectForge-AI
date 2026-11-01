package com.projectforge.exception;

/**
 * Base unchecked exception for ProjectForge AI application errors.
 */
public class ProjectForgeException extends RuntimeException {

    public ProjectForgeException(String message) {
        super(message);
    }

    public ProjectForgeException(String message, Throwable cause) {
        super(message, cause);
    }
}
