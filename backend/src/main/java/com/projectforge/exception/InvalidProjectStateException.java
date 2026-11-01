package com.projectforge.exception;

/**
 * Exception thrown when an invalid project lifecycle transition or action is attempted.
 * Maps to HTTP 400 Bad Request.
 */
public class InvalidProjectStateException extends ProjectForgeException {

    public InvalidProjectStateException(String message) {
        super(message);
    }
}
