package com.projectforge.exception;

import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when generated Markdown documents fail structure, filename, or consistency checks.
 */
public class MarkdownValidationException extends ProjectForgeException {

    private final List<String> validationErrors;

    public MarkdownValidationException(String message) {
        super(message);
        this.validationErrors = List.of(message);
    }

    public MarkdownValidationException(String message, List<String> validationErrors) {
        super(message + ": " + String.join("; ", validationErrors));
        this.validationErrors = validationErrors != null ? List.copyOf(validationErrors) : Collections.emptyList();
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
