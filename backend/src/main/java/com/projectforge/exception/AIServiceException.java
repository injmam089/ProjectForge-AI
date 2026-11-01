package com.projectforge.exception;

/**
 * Exception thrown when the AI service encounters a communication, provider,
 * configuration, or parsing failure.
 */
public class AIServiceException extends ProjectForgeException {

    public AIServiceException(String message) {
        super(message);
    }

    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
