package com.projectforge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.dto.DocumentAiAction;
import com.projectforge.dto.DocumentAiEditRequest;
import com.projectforge.dto.DocumentAiEditResponse;
import com.projectforge.dto.DocumentEditRequest;
import com.projectforge.dto.DocumentGenerationResponse;
import com.projectforge.dto.DocumentResponse;
import com.projectforge.dto.DocumentSaveResponse;
import com.projectforge.dto.DocumentSummaryResponse;
import com.projectforge.dto.DocumentVersionResponse;
import com.projectforge.dto.DocumentVersionSummaryResponse;
import com.projectforge.dto.WorkspaceResponse;
import com.projectforge.entity.DocumentChangeType;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.exception.DocumentNotFoundException;
import com.projectforge.exception.GlobalExceptionHandler;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.exception.ResourceNotFoundException;
import com.projectforge.security.SecurityUser;
import com.projectforge.service.DocumentAiEditService;
import com.projectforge.service.DocumentGenerationService;
import com.projectforge.service.DocumentWorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentControllerTest {

    private MockMvc mockMvcProjects;
    private MockMvc mockMvcDocuments;
    private StubDocumentGenerationService documentGenerationService;
    private StubDocumentWorkspaceService documentWorkspaceService;
    private StubDocumentAiEditService documentAiEditService;
    private SecurityUser authenticatedUser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        documentGenerationService = new StubDocumentGenerationService();
        documentWorkspaceService = new StubDocumentWorkspaceService();
        documentAiEditService = new StubDocumentAiEditService();

        User user = new User("Owner User", "owner@example.com", "hash", Role.USER);
        user.setId(10L);
        authenticatedUser = new SecurityUser(user);

        ProjectController projectController = new ProjectController(null, null, documentGenerationService, documentWorkspaceService);
        DocumentController documentController = new DocumentController(documentGenerationService, documentWorkspaceService, documentAiEditService);

        HandlerMethodArgumentResolver authResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return authenticatedUser;
            }
        };

        mockMvcProjects = MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authResolver)
                .build();

        mockMvcDocuments = MockMvcBuilders.standaloneSetup(documentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authResolver)
                .build();
    }

    @Test
    @DisplayName("POST /api/projects/{id}/generate returns 200 OK with generation response")
    void generateDocumentationSuccess() throws Exception {
        mockMvcProjects.perform(post("/api/projects/10/generate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(10))
                .andExpect(jsonPath("$.status").value("GENERATED"))
                .andExpect(jsonPath("$.documentsCount").value(1))
                .andExpect(jsonPath("$.documents[0].filename").value("README.md"));
    }

    @Test
    @DisplayName("GET /api/projects/{id}/workspace returns 200 OK with workspace overview")
    void getWorkspaceSuccess() throws Exception {
        mockMvcProjects.perform(get("/api/projects/10/workspace")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(10))
                .andExpect(jsonPath("$.projectName").value("Test Project"))
                .andExpect(jsonPath("$.documents.length()").value(1));
    }

    @Test
    @DisplayName("PUT /api/documents/{id} returns 200 OK with updated version")
    void saveDocumentSuccess() throws Exception {
        DocumentEditRequest req = new DocumentEditRequest("# New Title\n\nUpdated content with sufficient characters.");
        mockMvcDocuments.perform(put("/api/documents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.message").value("Document saved successfully"));
    }

    @Test
    @DisplayName("PUT /api/documents/{id} returns 404 when document not owned")
    void saveDocumentAntiIdor() throws Exception {
        DocumentEditRequest req = new DocumentEditRequest("# New Title\n\nUpdated content with sufficient characters.");
        mockMvcDocuments.perform(put("/api/documents/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/documents/{id}/versions returns 200 OK with list of version summaries")
    void getVersionsSuccess() throws Exception {
        mockMvcDocuments.perform(get("/api/documents/1/versions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].version").value(2));
    }

    @Test
    @DisplayName("GET /api/documents/{id}/versions/{version} returns 200 OK with historical content")
    void getVersionContentSuccess() throws Exception {
        mockMvcDocuments.perform(get("/api/documents/1/versions/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.content").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/documents/{id}/restore/{version} returns 200 OK with restored document")
    void restoreVersionSuccess() throws Exception {
        mockMvcDocuments.perform(post("/api/documents/1/restore/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    @DisplayName("POST /api/documents/{id}/regenerate returns 200 OK with canonical regenerated document")
    void regenerateDocumentSuccess() throws Exception {
        mockMvcDocuments.perform(post("/api/documents/1/regenerate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @DisplayName("POST /api/documents/{id}/ai-edit returns 200 OK with AI edit response")
    void aiEditSuccess() throws Exception {
        DocumentAiEditRequest req = new DocumentAiEditRequest(DocumentAiAction.IMPROVE, null);
        mockMvcDocuments.perform(post("/api/documents/1/ai-edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.action").value("IMPROVE"));
    }

    private static class StubDocumentGenerationService extends DocumentGenerationService {
        public StubDocumentGenerationService() {
            super(null, null, null, null, null, null, null, null);
        }

        @Override
        public DocumentGenerationResponse generateDocumentation(Long projectId, String userEmail) {
            if (projectId.equals(999L)) {
                throw new ProjectNotFoundException(999L);
            }
            DocumentSummaryResponse summary = new DocumentSummaryResponse(
                    1L, projectId, "README.md", DocumentType.README, "Overview", 1, 1500, Instant.now(), Instant.now()
            );
            return new DocumentGenerationResponse(
                    projectId, ProjectStatus.GENERATED, 1, List.of(summary), "Documentation generated."
            );
        }

        @Override
        public List<DocumentSummaryResponse> getProjectDocuments(Long projectId, String userEmail) {
            if (projectId.equals(999L)) {
                throw new ProjectNotFoundException(999L);
            }
            return List.of(
                    new DocumentSummaryResponse(1L, projectId, "README.md", DocumentType.README, "Overview", 1, 1200, Instant.now(), Instant.now()),
                    new DocumentSummaryResponse(2L, projectId, "ARCHITECTURE.md", DocumentType.ARCHITECTURE, "Arch", 1, 1800, Instant.now(), Instant.now())
            );
        }

        @Override
        public DocumentResponse getDocument(Long documentId, String userEmail) {
            if (documentId.equals(999L)) {
                throw new ResourceNotFoundException("Document not found with id: " + documentId);
            }
            return new DocumentResponse(
                    documentId, 10L, "README.md", DocumentType.README, "# Project Overview\n\nContent here.", 1, 35, Instant.now(), Instant.now()
            );
        }
    }

    private static class StubDocumentWorkspaceService extends DocumentWorkspaceService {
        public StubDocumentWorkspaceService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public WorkspaceResponse getWorkspace(Long projectId, String userEmail) {
            if (projectId.equals(999L)) throw new ProjectNotFoundException(999L);
            DocumentSummaryResponse doc = new DocumentSummaryResponse(1L, projectId, "README.md", DocumentType.README, "Overview", 1, 100, Instant.now(), Instant.now());
            return new WorkspaceResponse(projectId, "Test Project", ProjectStatus.GENERATED, List.of(doc));
        }

        @Override
        public DocumentSaveResponse saveDocument(Long documentId, DocumentEditRequest request, String userEmail) {
            if (documentId.equals(999L)) throw new DocumentNotFoundException(999L);
            return new DocumentSaveResponse(documentId, "README.md", 2, request.content().length(), "Document saved successfully");
        }

        @Override
        public List<DocumentVersionSummaryResponse> getVersions(Long documentId, String userEmail) {
            if (documentId.equals(999L)) throw new DocumentNotFoundException(999L);
            return List.of(
                    new DocumentVersionSummaryResponse(2L, documentId, 2, DocumentChangeType.MANUAL_EDIT, null, 10L, Instant.now()),
                    new DocumentVersionSummaryResponse(1L, documentId, 1, DocumentChangeType.GENERATE, "Init", 10L, Instant.now())
            );
        }

        @Override
        public DocumentVersionResponse getVersion(Long documentId, int version, String userEmail) {
            if (documentId.equals(999L)) throw new DocumentNotFoundException(999L);
            return new DocumentVersionResponse(1L, documentId, version, "README.md", "# Version " + version + " content with sufficient length.", DocumentChangeType.MANUAL_EDIT, null, 10L, Instant.now());
        }

        @Override
        public DocumentResponse restoreVersion(Long documentId, int targetVersion, String userEmail) {
            if (documentId.equals(999L)) throw new DocumentNotFoundException(999L);
            return new DocumentResponse(documentId, 10L, "README.md", DocumentType.README, "# Restored content with sufficient length.", 3, 40, Instant.now(), Instant.now());
        }

        @Override
        public DocumentResponse regenerateDocument(Long documentId, String userEmail) {
            if (documentId.equals(999L)) throw new DocumentNotFoundException(999L);
            return new DocumentResponse(documentId, 10L, "README.md", DocumentType.README, "# Canonical regenerated content with sufficient length.", 2, 50, Instant.now(), Instant.now());
        }
    }

    private static class StubDocumentAiEditService extends DocumentAiEditService {
        public StubDocumentAiEditService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public DocumentAiEditResponse aiEditDocument(Long documentId, DocumentAiEditRequest request, String userEmail) {
            if (documentId.equals(999L)) throw new DocumentNotFoundException(999L);
            return new DocumentAiEditResponse(documentId, "README.md", 2, "# AI Edited content with sufficient length.", request.action(), "Document updated successfully via AI " + request.action().name());
        }
    }
}
