package com.projectforge.controller;

import com.projectforge.dto.HealthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HealthControllerTest {

    private final HealthController healthController = new HealthController();

    @Test
    @DisplayName("GET /api/health should return HTTP 200 with UP status and service name")
    void getHealth_shouldReturnUpStatus() {
        ResponseEntity<HealthResponse> response = healthController.getHealth();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().status());
        assertEquals("ProjectForge AI Backend", response.getBody().service());
    }
}
