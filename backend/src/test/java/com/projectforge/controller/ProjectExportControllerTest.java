package com.projectforge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.projectforge.dto.ExportResponse;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.exception.GlobalExceptionHandler;
import com.projectforge.exception.InvalidExportException;
import com.projectforge.exception.ResourceNotFoundException;
import com.projectforge.export.ExportService;
import com.projectforge.security.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectExportControllerTest {

    private MockMvc mockMvc;
    private StubExportService exportService;
    private SecurityUser authenticatedSecurityUser;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        exportService = new StubExportService();
        ProjectController controller = new ProjectController(null, null, null, null, exportService);

        User testUser = new User("Test Owner", "owner@example.com", "hash", Role.USER);
        testUser.setId(10L);
        authenticatedSecurityUser = new SecurityUser(testUser);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(
                            MethodParameter parameter,
                            ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest,
                            WebDataBinderFactory binderFactory
                    ) {
                        return authenticatedSecurityUser;
                    }
                })
                .build();
    }

    @Test
    @DisplayName("POST /api/projects/{id}/export returns 200 OK with ExportResponse metadata")
    void prepareExportSuccess() throws Exception {
        mockMvc.perform(post("/api/projects/1/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.projectName").value("CloudPulse Telemetry"))
                .andExpect(jsonPath("$.documentCount").value(14))
                .andExpect(jsonPath("$.filename").value("ProjectForge_CloudPulse_Telemetry.zip"));
    }

    @Test
    @DisplayName("GET /api/projects/{id}/export/download returns 200 OK with application/zip and binary bytes")
    void downloadExportSuccess() throws Exception {
        mockMvc.perform(get("/api/projects/1/export/download"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/zip"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ProjectForge_CloudPulse_Telemetry.zip\""))
                .andExpect(content().bytes("PK\003\004fake-zip-binary-content".getBytes()));
    }

    @Test
    @DisplayName("GET /api/projects/{id}/export (direct endpoint) returns 200 OK with application/zip")
    void directExportSuccess() throws Exception {
        mockMvc.perform(get("/api/projects/1/export"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/zip"))
                .andExpect(content().bytes("PK\003\004fake-zip-binary-content".getBytes()));
    }

    @Test
    @DisplayName("Export on non-owned project returns 404 ResourceNotFoundException (Anti-IDOR)")
    void exportNonOwnedProjectThrows404() throws Exception {
        mockMvc.perform(post("/api/projects/999/export"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Export on empty project returns 400 Bad Request with safe message")
    void exportEmptyProjectThrows400() throws Exception {
        mockMvc.perform(post("/api/projects/500/export"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Project has no documents available for export."));
    }

    private static class StubExportService extends ExportService {
        public StubExportService() {
            super(null, null, null, null);
        }

        @Override
        public ExportResponse prepareExport(Long projectId, String userEmail) {
            if (projectId.equals(999L)) {
                throw new ResourceNotFoundException("Project", "id", projectId);
            }
            if (projectId.equals(500L)) {
                throw new InvalidExportException("Project has no documents available for export.");
            }
            return new ExportResponse(
                    projectId,
                    "CloudPulse Telemetry",
                    14,
                    "ProjectForge_CloudPulse_Telemetry.zip",
                    Instant.now()
            );
        }

        @Override
        public ExportPackage generateExportPackage(Long projectId, String userEmail) {
            if (projectId.equals(999L)) {
                throw new ResourceNotFoundException("Project", "id", projectId);
            }
            if (projectId.equals(500L)) {
                throw new InvalidExportException("Project has no documents available for export.");
            }
            return new ExportPackage(
                    "ProjectForge_CloudPulse_Telemetry.zip",
                    "PK\003\004fake-zip-binary-content".getBytes()
            );
        }
    }
}
