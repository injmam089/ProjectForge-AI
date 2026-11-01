package com.projectforge.exception;

import com.projectforge.entity.ProjectStatus;

/**
 * Exception thrown when an invalid project lifecycle transition or action is attempted.
 * Maps to HTTP 400 Bad Request.
 */
public class InvalidProjectStateException extends ProjectForgeException {

    private final Long projectId;
    private final ProjectStatus status;

    public InvalidProjectStateException(String message) {
        super(message);
        this.projectId = null;
        this.status = null;
    }

    public InvalidProjectStateException(Long projectId, ProjectStatus status, String message) {
        super(message);
        this.projectId = projectId;
        this.status = status;
    }

    public Long getProjectId() {
        return projectId;
    }

    public ProjectStatus getStatus() {
        return status;
    }
}
