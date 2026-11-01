package com.projectforge.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.projectforge.dto.ProjectCreateRequest;
import com.projectforge.dto.ProjectResponse;
import com.projectforge.dto.ProjectSpecificationResponse;
import com.projectforge.dto.ProjectSpecificationUpdateRequest;
import com.projectforge.dto.ProjectSummaryResponse;
import com.projectforge.dto.ProjectUpdateRequest;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.ProjectType;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.exception.GlobalExceptionHandler;
import com.projectforge.exception.InvalidProjectStateException;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.exception.ProjectValidationException;
import com.projectforge.security.SecurityUser;
import com.projectforge.service.ProjectService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private StubProjectService projectService;
    private SecurityUser authenticatedSecurityUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        projectService = new StubProjectService();
        ProjectController controller = new ProjectController(projectService);

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
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return authenticatedSecurityUser;
                    }
                })
                .build();
    }

    @Test
    @DisplayName("1. POST /api/projects with valid request returns 201 Created")
    void createProject_shouldReturn201() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "HealthGuard AI",
                "Automated clinical risk prediction platform for inpatient monitoring",
                "Hospitals face delayed patient interventions",
                "Doctors, clinicians",
                ProjectType.AI_ML_PROJECT,
                ProjectDifficulty.EXPERT,
                "Python, PyTorch, React",
                "HIPAA compliance"
        );

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("HealthGuard AI"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("2. GET /api/projects returns 200 OK with project summaries")
    void getMyProjects_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Existing Project"));
    }

    @Test
    @DisplayName("3. GET /api/projects/{id} for owned project returns 200 OK")
    void getProject_existing_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Existing Project"));
    }

    @Test
    @DisplayName("4. PUT /api/projects/{id} with valid request returns 200 OK")
    void updateProject_valid_shouldReturn200() throws Exception {
        ProjectUpdateRequest updateReq = new ProjectUpdateRequest(
                "Updated Project Name",
                "Updated comprehensive project description",
                "Updated problem statement",
                "DevOps Engineers",
                ProjectType.FULL_STACK_APPLICATION,
                ProjectDifficulty.ADVANCED,
                "Spring Boot, React",
                "OAuth2"
        );

        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Project Name"));
    }

    @Test
    @DisplayName("5. DELETE /api/projects/{id} returns 204 No Content")
    void deleteProject_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("6. POST /api/projects/{id}/prepare-analysis returns 200 OK with READY_FOR_ANALYSIS status")
    void prepareForAnalysis_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/projects/1/prepare-analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("READY_FOR_ANALYSIS"));
    }

    @Test
    @DisplayName("7. Validation failures return 400 Bad Request")
    void createProject_invalidValidation_shouldReturn400() throws Exception {
        ProjectCreateRequest invalid = new ProjectCreateRequest(
                "", // Blank name
                "", // Blank description
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    @DisplayName("8. Not-found behavior returns 404 Not Found")
    void getProject_nonExisting_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("9. Reject prepare-analysis when invalid data returns 400 Bad Request")
    void prepareForAnalysis_invalidData_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/projects/500/prepare-analysis"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // Stub ProjectService for isolated web slice unit testing
    private static class StubProjectService extends ProjectService {

        public StubProjectService() {
            super(null, null, new ObjectMapper());
        }

        @Override
        public ProjectResponse createProject(ProjectCreateRequest request, String userEmail) {
            return new ProjectResponse(
                    1L,
                    request.name(),
                    request.description(),
                    request.problemStatement(),
                    request.targetUsers(),
                    request.projectType(),
                    request.difficulty(),
                    request.preferredTechStack(),
                    request.specialRequirements(),
                    ProjectStatus.DRAFT,
                    10L,
                    "Test Owner",
                    Instant.now(),
                    Instant.now()
            );
        }

        @Override
        public List<ProjectSummaryResponse> getUserProjects(String userEmail) {
            return List.of(new ProjectSummaryResponse(
                    1L,
                    "Existing Project",
                    "Description",
                    ProjectType.WEB_APPLICATION,
                    ProjectDifficulty.INTERMEDIATE,
                    ProjectStatus.DRAFT,
                    Instant.now(),
                    Instant.now()
            ));
        }

        @Override
        public ProjectResponse getProjectById(Long projectId, String userEmail) {
            if (projectId.equals(999L)) {
                throw new ProjectNotFoundException(projectId);
            }
            return new ProjectResponse(
                    projectId,
                    "Existing Project",
                    "Description",
                    null,
                    null,
                    ProjectType.WEB_APPLICATION,
                    ProjectDifficulty.INTERMEDIATE,
                    null,
                    null,
                    ProjectStatus.DRAFT,
                    10L,
                    "Test Owner",
                    Instant.now(),
                    Instant.now()
            );
        }

        @Override
        public ProjectResponse updateProject(Long projectId, ProjectUpdateRequest request, String userEmail) {
            if (projectId.equals(999L)) {
                throw new ProjectNotFoundException(projectId);
            }
            return new ProjectResponse(
                    projectId,
                    request.name(),
                    request.description(),
                    request.problemStatement(),
                    request.targetUsers(),
                    request.projectType(),
                    request.difficulty(),
                    request.preferredTechStack(),
                    request.specialRequirements(),
                    ProjectStatus.DRAFT,
                    10L,
                    "Test Owner",
                    Instant.now(),
                    Instant.now()
            );
        }

        @Override
        public void deleteProject(Long projectId, String userEmail) {
            if (projectId.equals(999L)) {
                throw new ProjectNotFoundException(projectId);
            }
        }

        @Override
        public ProjectResponse prepareForAnalysis(Long projectId, String userEmail) {
            if (projectId.equals(999L)) {
                throw new ProjectNotFoundException(projectId);
            }
            if (projectId.equals(500L)) {
                throw new ProjectValidationException("Project description must have at least 10 characters for analysis");
            }
            return new ProjectResponse(
                    projectId,
                    "Existing Project",
                    "Description meeting length requirements",
                    null,
                    null,
                    ProjectType.WEB_APPLICATION,
                    ProjectDifficulty.INTERMEDIATE,
                    null,
                    null,
                    ProjectStatus.READY_FOR_ANALYSIS,
                    10L,
                    "Test Owner",
                    Instant.now(),
                    Instant.now()
            );
        }

        @Override
        public ProjectSpecificationResponse getProjectSpecification(Long projectId, String userEmail) {
            if (projectId.equals(999L)) {
                throw new ProjectNotFoundException(projectId);
            }
            return new ProjectSpecificationResponse(projectId, "Existing Project", ProjectStatus.DRAFT, null);
        }

        @Override
        public ProjectSpecificationResponse updateProjectSpecification(Long projectId, ProjectSpecificationUpdateRequest request, String userEmail) {
            if (projectId.equals(999L)) {
                throw new ProjectNotFoundException(projectId);
            }
            return new ProjectSpecificationResponse(projectId, "Existing Project", ProjectStatus.DRAFT, request.specification());
        }
    }
}
