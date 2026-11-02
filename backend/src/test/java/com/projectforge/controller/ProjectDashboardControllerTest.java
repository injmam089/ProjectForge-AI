package com.projectforge.controller;

import com.projectforge.dto.PagedResponse;
import com.projectforge.dto.ProjectHistoryResponse;
import com.projectforge.dto.ProjectStatsResponse;
import com.projectforge.dto.ProjectSummaryResponse;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectHistoryEventType;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.ProjectType;
import com.projectforge.exception.GlobalExceptionHandler;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.service.ProjectHistoryService;
import com.projectforge.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectDashboardControllerTest {

    private MockMvc mockMvc;
    private StubProjectService stubProjectService;
    private StubProjectHistoryService stubProjectHistoryService;

    @BeforeEach
    void setUp() {
        stubProjectService = new StubProjectService();
        stubProjectHistoryService = new StubProjectHistoryService();

        ProjectController controller = new ProjectController(
                stubProjectService,
                null,
                null,
                null,
                null,
                stubProjectHistoryService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter,
                                                  ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest,
                                                  WebDataBinderFactory binderFactory) {
                        return User.withUsername("alpha@test.com")
                                .password("unused")
                                .roles("USER")
                                .build();
                    }
                })
                .build();
    }

    @Test
    @DisplayName("1. GET /api/projects without params returns List of projects")
    void getMyProjects_unpaged_returnsList() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alpha One"))
                .andExpect(jsonPath("$[0].documentCount").value(14));
    }

    @Test
    @DisplayName("2. GET /api/projects?page=0&size=10 returns PagedResponse")
    void getMyProjects_paged_returnsPagedResponse() throws Exception {
        mockMvc.perform(get("/api/projects")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Alpha One"));
    }

    @Test
    @DisplayName("3. GET /api/projects/stats returns ProjectStatsResponse")
    void getProjectStats_returnsStats() throws Exception {
        mockMvc.perform(get("/api/projects/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(5))
                .andExpect(jsonPath("$.draft").value(1))
                .andExpect(jsonPath("$.approved").value(2))
                .andExpect(jsonPath("$.generated").value(2));
    }

    @Test
    @DisplayName("4. GET /api/projects/{id}/history returns paginated project history")
    void getProjectHistory_returnsHistory() throws Exception {
        mockMvc.perform(get("/api/projects/1/history")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].eventType").value("DOCUMENTS_GENERATED"))
                .andExpect(jsonPath("$.content[1].eventType").value("PROJECT_CREATED"));
    }

    @Test
    @DisplayName("5. GET /api/projects/{id}/history for unowned project returns 404 Not Found")
    void getProjectHistory_unowned_returns404() throws Exception {
        mockMvc.perform(get("/api/projects/999/history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("6. DELETE /api/projects/{id} returns 204 No Content for owned project")
    void deleteProject_owned_returns204() throws Exception {
        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("7. DELETE /api/projects/{id} returns 404 Not Found for unowned project")
    void deleteProject_unowned_returns404() throws Exception {
        mockMvc.perform(delete("/api/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- Stub Services ---

    private static class StubProjectService extends ProjectService {
        public StubProjectService() {
            super(null, null, null);
        }

        @Override
        public List<ProjectSummaryResponse> getUserProjects(String userEmail) {
            return List.of(
                    new ProjectSummaryResponse(1L, "Alpha One", "Desc One", ProjectType.API_BACKEND, ProjectDifficulty.INTERMEDIATE, ProjectStatus.GENERATED, Instant.now(), Instant.now(), 14, 95),
                    new ProjectSummaryResponse(2L, "Alpha Two", "Desc Two", ProjectType.WEB_APPLICATION, ProjectDifficulty.ADVANCED, ProjectStatus.APPROVED, Instant.now(), Instant.now(), 0, 88)
            );
        }

        @Override
        public Object searchAndFilterProjects(String userEmail, String search, ProjectStatus status, ProjectType type, ProjectDifficulty difficulty, String sortField, String direction, Integer page, Integer size) {
            List<ProjectSummaryResponse> items = getUserProjects(userEmail);
            if (page != null) {
                return new PagedResponse<>(items, page, size != null ? size : 12, items.size(), 1, true, true);
            }
            return items;
        }

        @Override
        public ProjectStatsResponse getProjectStats(String userEmail) {
            return new ProjectStatsResponse(5, 1, 0, 0, 0, 0, 2, 0, 2, 0);
        }

        @Override
        public void deleteProject(Long projectId, String userEmail) {
            if (projectId == 999L) {
                throw new ProjectNotFoundException(projectId);
            }
        }
    }

    private static class StubProjectHistoryService extends ProjectHistoryService {
        public StubProjectHistoryService() {
            super(null, null, null);
        }

        @Override
        public PagedResponse<ProjectHistoryResponse> getProjectHistory(Long projectId, String userEmail, int page, int size) {
            if (projectId == 999L) {
                throw new ProjectNotFoundException(projectId);
            }
            List<ProjectHistoryResponse> content = List.of(
                    new ProjectHistoryResponse(2L, projectId, ProjectHistoryEventType.DOCUMENTS_GENERATED, "14 documents generated", null, userEmail, Instant.now()),
                    new ProjectHistoryResponse(1L, projectId, ProjectHistoryEventType.PROJECT_CREATED, "Project created", null, userEmail, Instant.now().minusSeconds(100))
            );
            return new PagedResponse<>(content, page, size, content.size(), 1, true, true);
        }
    }
}
