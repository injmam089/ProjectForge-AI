package com.projectforge.exception;

/**
 * Exception thrown when document planning, generation, or transactional persistence fails.
 */
public class DocumentGenerationException extends ProjectForgeException {

    private final Long projectId;

    public DocumentGenerationException(String message) {
        this(null, message);
    }

    public DocumentGenerationException(String message, Throwable cause) {
        this(null, message, cause);
    }

    public DocumentGenerationException(Long projectId, String message) {
        super(message);
        this.projectId = projectId;
    }

    public DocumentGenerationException(Long projectId, String message, Throwable cause) {
        super(message, cause);
        this.projectId = projectId;
    }

    public Long getProjectId() {
        return projectId;
    }
}
