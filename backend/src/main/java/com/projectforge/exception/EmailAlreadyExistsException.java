package com.projectforge.exception;

/**
 * Thrown when attempting to register a user with an email that is already registered.
 */
public class EmailAlreadyExistsException extends ProjectForgeException {

    public EmailAlreadyExistsException(String email) {
        super(String.format("An account with email '%s' already exists", email));
    }
}
