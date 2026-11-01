package com.projectforge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard authentication response payload for register and login flows.
 * Strictly avoids exposing passwords or password hashes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        boolean success,
        String message,
        String token,
        String tokenType,
        UserResponse user
) {
    public static AuthResponse registerSuccess() {
        return new AuthResponse(true, "User registered successfully", null, null, null);
    }

    public static AuthResponse registerSuccess(String message) {
        return new AuthResponse(true, message, null, null, null);
    }

    public static AuthResponse loginSuccess(String token, UserResponse user) {
        return new AuthResponse(true, "Login successful", token, "Bearer", user);
    }
}
