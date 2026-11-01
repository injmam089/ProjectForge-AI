package com.projectforge.dto;

/**
 * Standard health check response payload.
 * Matches Phase 1 specification:
 * {
 *   "status": "UP",
 *   "service": "ProjectForge AI Backend"
 * }
 */
public record HealthResponse(
        String status,
        String service
) {
    public static HealthResponse up() {
        return new HealthResponse("UP", "ProjectForge AI Backend");
    }
}
