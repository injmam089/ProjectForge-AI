package com.projectforge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.dto.ClarificationAnswerDto;
import com.projectforge.dto.ClarificationRequest;
import com.projectforge.dto.OpenQuestionDto;
import com.projectforge.dto.RequirementsAnalysisResponse;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.exception.GlobalExceptionHandler;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.security.SecurityUser;
import com.projectforge.service.ProjectRequirementsService;
import com.projectforge.testutil.TestSpecificationFactory;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectRequirementsControllerTest {

    private MockMvc mockMvc;
    private StubProjectRequirementsService requirementsService;
    private ObjectMapper objectMapper;
    private SecurityUser authenticatedUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        requirementsService = new StubProjectRequirementsService();

        User user = new User("Owner User", "owner@example.com", "hash", Role.USER);
        user.setId(10L);
        authenticatedUser = new SecurityUser(user);

        ProjectController controller = new ProjectController(null, requirementsService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return authenticatedUser;
                    }
                })
                .build();
    }

    @Test
    @DisplayName("POST /api/projects/{id}/analyze triggers analysis and returns 200 OK with UNDER_REVIEW when open questions exist")
    void analyzeProjectEndpointSuccess() throws Exception {
        mockMvc.perform(post("/api/projects/1/analyze")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.qualityScore").value(88))
                .andExpect(jsonPath("$.openQuestions[0].id").value("Q1"));
    }

    @Test
    @DisplayName("POST /api/projects/{id}/analyze returns 404 when project not owned (anti-IDOR)")
    void analyzeProjectAntiIdor() throws Exception {
        mockMvc.perform(post("/api/projects/999/analyze")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Project not found with id: '999'"));
    }

    @Test
    @DisplayName("GET /api/projects/{id}/requirements returns 200 OK with specification and open questions")
    void getRequirementsEndpointSuccess() throws Exception {
        mockMvc.perform(get("/api/projects/1/requirements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.qualityScore").value(88))
                .andExpect(jsonPath("$.specification.project.name").value("CloudPulse Telemetry"));
    }

    @Test
    @DisplayName("POST /api/projects/{id}/clarifications accepts valid answers and returns 200 OK")
    void submitClarificationsSuccess() throws Exception {
        ClarificationRequest request = new ClarificationRequest(List.of(
                new ClarificationAnswerDto("Q1", "Zstandard compression should be mandatory.")
        ));

        mockMvc.perform(post("/api/projects/1/clarifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qualityScore").value(92));
    }

    @Test
    @DisplayName("POST /api/projects/{id}/clarifications rejects empty answers with 400 Bad Request")
    void submitClarificationsValidationFailure() throws Exception {
        String invalidPayload = "{\"answers\": []}";

        mockMvc.perform(post("/api/projects/1/clarifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    private static class StubProjectRequirementsService extends ProjectRequirementsService {

        public StubProjectRequirementsService() {
            super(null, null, null, null, new ObjectMapper());
        }

        @Override
        public RequirementsAnalysisResponse analyzeProject(Long projectId, String userEmail) {
            if (projectId.equals(999L)) {
                throw new ProjectNotFoundException(999L);
            }
            return new RequirementsAnalysisResponse(
                    projectId,
                    ProjectStatus.UNDER_REVIEW,
                    88,
                    TestSpecificationFactory.createValidSpecificationNode(),
                    List.of(new OpenQuestionDto("Q1", "Compression mandatory?", "HIGH", "Performance")),
                    "AI requirements analysis completed successfully. Unresolved critical/high decisions require clarification before requirements are ready."
            );
        }

        @Override
        public RequirementsAnalysisResponse getRequirements(Long projectId, String userEmail) {
            if (projectId.equals(999L)) {
                throw new ProjectNotFoundException(999L);
            }
            return new RequirementsAnalysisResponse(
                    projectId,
                    ProjectStatus.REQUIREMENTS_READY,
                    88,
                    TestSpecificationFactory.createValidSpecificationNode(),
                    List.of(),
                    "Retrieved"
            );
        }

        @Override
        public RequirementsAnalysisResponse submitClarifications(Long projectId, ClarificationRequest request, String userEmail) {
            if (projectId.equals(999L)) {
                throw new ProjectNotFoundException(999L);
            }
            return new RequirementsAnalysisResponse(
                    projectId,
                    ProjectStatus.REQUIREMENTS_READY,
                    92,
                    TestSpecificationFactory.createValidSpecificationNode(),
                    List.of(),
                    "Clarified"
            );
        }
    }
}
