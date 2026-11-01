package com.projectforge.exception;

/**
 * Thrown when login credentials fail verification.
 * Provides a safe generic error message without revealing user existence.
 */
public class InvalidCredentialsException extends ProjectForgeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
