package com.projectforge.exception;

/**
 * Thrown when an export request fails validation rules (e.g. empty project, invalid filenames, oversized content).
 */
public class InvalidExportException extends ProjectForgeException {

    public InvalidExportException(String message) {
        super(message);
    }
}
