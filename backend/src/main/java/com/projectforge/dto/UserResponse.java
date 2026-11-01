package com.projectforge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;

import java.time.Instant;

/**
 * Safe user response payload.
 * Never exposes passwords or password hashes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        Instant createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
