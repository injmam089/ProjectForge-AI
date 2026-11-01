package com.projectforge.exception;

import com.projectforge.dto.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleResourceNotFound should return 404 with error details")
    void handleResourceNotFound_shouldReturn404() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/projects/99");
        ResourceNotFoundException ex = new ResourceNotFoundException("Project", "id", 99);

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("Project not found with id: '99'", response.getBody().message());
        assertEquals("/api/projects/99", response.getBody().path());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("handleApplicationExceptions should return 400 Bad Request")
    void handleApplicationExceptions_shouldReturn400() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        ProjectForgeException ex = new ProjectForgeException("Invalid project state transition");

        ResponseEntity<ErrorResponse> response = handler.handleApplicationExceptions(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("Invalid project state transition", response.getBody().message());
    }

    @Test
    @DisplayName("handleMethodNotSupported should return 405 Method Not Allowed")
    void handleMethodNotSupported_shouldReturn405() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/health");
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(405, response.getBody().status());
        assertEquals("Method Not Allowed", response.getBody().error());
        assertEquals("HTTP method DELETE is not supported for this endpoint", response.getBody().message());
    }

    @Test
    @DisplayName("handleUnhandledException should return 500 without leaking stack traces")
    void handleUnhandledException_shouldReturn500() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/fault");
        RuntimeException ex = new RuntimeException("Sensitive database exception or internal trace");

        ResponseEntity<ErrorResponse> response = handler.handleUnhandledException(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().status());
        assertEquals("Internal Server Error", response.getBody().error());
        assertEquals("An unexpected internal server error occurred", response.getBody().message());
    }
}
