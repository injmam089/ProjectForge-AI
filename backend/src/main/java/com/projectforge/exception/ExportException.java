package com.projectforge.exception;

/**
 * Thrown when an unexpected error occurs during ZIP packaging, post-generation validation, or compression.
 */
public class ExportException extends ProjectForgeException {

    public ExportException(String message) {
        super(message);
    }

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
