package com.projectforge.exception;

/**
 * Exception thrown when generated requirements or structured specifications fail validation rules.
 */
public class AIValidationException extends ProjectForgeException {

    public AIValidationException(String message) {
        super(message);
    }

    public AIValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
