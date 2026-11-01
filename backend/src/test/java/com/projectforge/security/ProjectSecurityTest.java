package com.projectforge.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.projectforge.controller.ProjectController;
import com.projectforge.dto.ProjectCreateRequest;
import com.projectforge.dto.ProjectResponse;
import com.projectforge.dto.ProjectSpecificationResponse;
import com.projectforge.dto.ProjectSpecificationUpdateRequest;
import com.projectforge.dto.ProjectSummaryResponse;
import com.projectforge.dto.ProjectUpdateRequest;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.ProjectType;
import com.projectforge.exception.GlobalExceptionHandler;
import com.projectforge.exception.ProjectNotFoundException;
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
import com.projectforge.entity.Role;
import com.projectforge.entity.User;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security and IDOR tests for Project endpoints.
 * Verifies that cross-user access attempts (e.g. User A requesting User B's project)
 * result strictly in HTTP 404 Not Found across GET, PUT, DELETE, and prepare-analysis.
 */
class ProjectSecurityTest {

    private MockMvc mockMvcUserA;
    private MockMvc mockMvcUserB;
    private ObjectMapper objectMapper;
    private MockOwnershipProjectService projectService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        projectService = new MockOwnershipProjectService();
        ProjectController controller = new ProjectController(projectService);

        // User A context
        mockMvcUserA = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new MockUserPrincipalResolver("userA@example.com"))
                .build();

        // User B context
        mockMvcUserB = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new MockUserPrincipalResolver("userB@example.com"))
                .build();

        // Populate projects: Project 1 belongs to User A, Project 2 belongs to User B
        projectService.createMockProject(1L, "userA@example.com", "Project A");
        projectService.createMockProject(2L, "userB@example.com", "Project B");
    }

    @Test
    @DisplayName("User A can view their own project (Project 1)")
    void userA_canAccessOwnProject() throws Exception {
        mockMvcUserA.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Project A"));
    }

    @Test
    @DisplayName("IDOR Prevention: User A accessing User B's project (Project 2) returns 404 Not Found")
    void idor_userA_accessingProjectB_returns404() throws Exception {
        mockMvcUserA.perform(get("/api/projects/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("IDOR Prevention: User A updating User B's project (Project 2) returns 404 Not Found")
    void idor_userA_updatingProjectB_returns404() throws Exception {
        ProjectUpdateRequest updateReq = new ProjectUpdateRequest(
                "Hacked Name", "Valid updated idea description", "Prob", "Users", ProjectType.WEB_APPLICATION, ProjectDifficulty.BEGINNER, "Tech", "Req"
        );

        mockMvcUserA.perform(put("/api/projects/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("IDOR Prevention: User A deleting User B's project (Project 2) returns 404 Not Found")
    void idor_userA_deletingProjectB_returns404() throws Exception {
        mockMvcUserA.perform(delete("/api/projects/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("IDOR Prevention: User A preparing User B's project (Project 2) for analysis returns 404 Not Found")
    void idor_userA_preparingProjectB_returns404() throws Exception {
        mockMvcUserA.perform(post("/api/projects/2/prepare-analysis"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("IDOR Prevention: User A accessing User B's specification returns 404 Not Found")
    void idor_userA_accessingProjectB_specification_returns404() throws Exception {
        mockMvcUserA.perform(get("/api/projects/2/specification"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("IDOR Prevention: User A updating User B's specification returns 404 Not Found")
    void idor_userA_updatingProjectB_specification_returns404() throws Exception {
        ProjectSpecificationUpdateRequest specUpdate = new ProjectSpecificationUpdateRequest(objectMapper.createObjectNode());

        mockMvcUserA.perform(put("/api/projects/2/specification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(specUpdate)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("User A listing projects only sees their own projects, not User B's")
    void userA_listingProjects_onlySeesOwnProjects() throws Exception {
        mockMvcUserA.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Project A"));
    }

    // Mock ProjectService enforcing user ownership
    private static class MockOwnershipProjectService extends ProjectService {

        private static class StoredMockProject {
            Long id;
            String ownerEmail;
            String name;

            StoredMockProject(Long id, String ownerEmail, String name) {
                this.id = id;
                this.ownerEmail = ownerEmail;
                this.name = name;
            }
        }

        private final Map<Long, StoredMockProject> store = new HashMap<>();

        public MockOwnershipProjectService() {
            super(null, null, new ObjectMapper());
        }

        public void createMockProject(Long id, String ownerEmail, String name) {
            store.put(id, new StoredMockProject(id, ownerEmail, name));
        }

        @Override
        public ProjectResponse getProjectById(Long projectId, String userEmail) {
            StoredMockProject p = store.get(projectId);
            if (p == null || !p.ownerEmail.equalsIgnoreCase(userEmail)) {
                throw new ProjectNotFoundException(projectId);
            }
            return new ProjectResponse(
                    p.id, p.name, "Idea", null, null, ProjectType.WEB_APPLICATION, ProjectDifficulty.BEGINNER, null, null, ProjectStatus.DRAFT, 1L, "Owner", Instant.now(), Instant.now()
            );
        }

        @Override
        public ProjectResponse updateProject(Long projectId, ProjectUpdateRequest request, String userEmail) {
            StoredMockProject p = store.get(projectId);
            if (p == null || !p.ownerEmail.equalsIgnoreCase(userEmail)) {
                throw new ProjectNotFoundException(projectId);
            }
            p.name = request.name();
            return new ProjectResponse(
                    p.id, p.name, request.description(), null, null, ProjectType.WEB_APPLICATION, ProjectDifficulty.BEGINNER, null, null, ProjectStatus.DRAFT, 1L, "Owner", Instant.now(), Instant.now()
            );
        }

        @Override
        public void deleteProject(Long projectId, String userEmail) {
            StoredMockProject p = store.get(projectId);
            if (p == null || !p.ownerEmail.equalsIgnoreCase(userEmail)) {
                throw new ProjectNotFoundException(projectId);
            }
            store.remove(projectId);
        }

        @Override
        public ProjectResponse prepareForAnalysis(Long projectId, String userEmail) {
            StoredMockProject p = store.get(projectId);
            if (p == null || !p.ownerEmail.equalsIgnoreCase(userEmail)) {
                throw new ProjectNotFoundException(projectId);
            }
            return new ProjectResponse(
                    p.id, p.name, "Valid description", null, null, ProjectType.WEB_APPLICATION, ProjectDifficulty.BEGINNER, null, null, ProjectStatus.READY_FOR_ANALYSIS, 1L, "Owner", Instant.now(), Instant.now()
            );
        }

        @Override
        public List<ProjectSummaryResponse> getUserProjects(String userEmail) {
            return store.values().stream()
                    .filter(p -> p.ownerEmail.equalsIgnoreCase(userEmail))
                    .map(p -> new ProjectSummaryResponse(p.id, p.name, "Idea", ProjectType.WEB_APPLICATION, ProjectDifficulty.BEGINNER, ProjectStatus.DRAFT, Instant.now(), Instant.now()))
                    .toList();
        }

        @Override
        public ProjectSpecificationResponse getProjectSpecification(Long projectId, String userEmail) {
            StoredMockProject p = store.get(projectId);
            if (p == null || !p.ownerEmail.equalsIgnoreCase(userEmail)) {
                throw new ProjectNotFoundException(projectId);
            }
            return new ProjectSpecificationResponse(projectId, p.name, ProjectStatus.DRAFT, null);
        }

        @Override
        public ProjectSpecificationResponse updateProjectSpecification(Long projectId, ProjectSpecificationUpdateRequest request, String userEmail) {
            StoredMockProject p = store.get(projectId);
            if (p == null || !p.ownerEmail.equalsIgnoreCase(userEmail)) {
                throw new ProjectNotFoundException(projectId);
            }
            return new ProjectSpecificationResponse(projectId, p.name, ProjectStatus.DRAFT, request.specification());
        }
    }

    private static class MockUserPrincipalResolver implements HandlerMethodArgumentResolver {
        private final String email;

        MockUserPrincipalResolver(String email) {
            this.email = email;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            User user = new User("User", email, "hash", Role.USER);
            user.setId(email.contains("userA") ? 1L : 2L);
            return new SecurityUser(user);
        }
    }
}
