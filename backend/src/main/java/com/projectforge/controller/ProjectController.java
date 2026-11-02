package com.projectforge.controller;

import com.projectforge.dto.ApprovalRequest;
import com.projectforge.dto.ApprovalResponse;
import com.projectforge.dto.ClarificationRequest;
import com.projectforge.dto.DocumentGenerationResponse;
import com.projectforge.dto.DocumentSummaryResponse;
import com.projectforge.dto.ExportResponse;
import com.projectforge.dto.PagedResponse;
import com.projectforge.dto.ProjectCreateRequest;
import com.projectforge.dto.ProjectHistoryResponse;
import com.projectforge.dto.ProjectResponse;
import com.projectforge.dto.ProjectReviewResponse;
import com.projectforge.dto.ProjectReviewUpdateRequest;
import com.projectforge.dto.ProjectSpecificationResponse;
import com.projectforge.dto.ProjectSpecificationUpdateRequest;
import com.projectforge.dto.ProjectStatsResponse;
import com.projectforge.dto.ProjectSummaryResponse;
import com.projectforge.dto.ProjectUpdateRequest;
import com.projectforge.dto.RequirementsAnalysisResponse;
import com.projectforge.dto.WorkspaceResponse;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.ProjectType;
import com.projectforge.export.ExportService;
import com.projectforge.service.DocumentGenerationService;
import com.projectforge.service.DocumentWorkspaceService;
import com.projectforge.service.ProjectHistoryService;
import com.projectforge.service.ProjectRequirementsService;
import com.projectforge.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller exposing authenticated project management, requirements intake,
 * AI requirement analysis, dashboard statistics, search/filtering, and project history.
 * All routes require authentication and strictly enforce user ownership.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectRequirementsService projectRequirementsService;
    private final DocumentGenerationService documentGenerationService;
    private final DocumentWorkspaceService documentWorkspaceService;
    private final ExportService exportService;
    private final ProjectHistoryService projectHistoryService;

    public ProjectController(ProjectService projectService) {
        this(projectService, null, null, null, null, null);
    }

    public ProjectController(ProjectService projectService, ProjectRequirementsService projectRequirementsService) {
        this(projectService, projectRequirementsService, null, null, null, null);
    }

    public ProjectController(
            ProjectService projectService,
            ProjectRequirementsService projectRequirementsService,
            DocumentGenerationService documentGenerationService
    ) {
        this(projectService, projectRequirementsService, documentGenerationService, null, null, null);
    }

    public ProjectController(
            ProjectService projectService,
            ProjectRequirementsService projectRequirementsService,
            DocumentGenerationService documentGenerationService,
            DocumentWorkspaceService documentWorkspaceService
    ) {
        this(projectService, projectRequirementsService, documentGenerationService, documentWorkspaceService, null, null);
    }

    public ProjectController(
            ProjectService projectService,
            ProjectRequirementsService projectRequirementsService,
            DocumentGenerationService documentGenerationService,
            DocumentWorkspaceService documentWorkspaceService,
            ExportService exportService
    ) {
        this(projectService, projectRequirementsService, documentGenerationService, documentWorkspaceService, exportService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ProjectController(
            ProjectService projectService,
            ProjectRequirementsService projectRequirementsService,
            DocumentGenerationService documentGenerationService,
            DocumentWorkspaceService documentWorkspaceService,
            ExportService exportService,
            ProjectHistoryService projectHistoryService
    ) {
        this.projectService = projectService;
        this.projectRequirementsService = projectRequirementsService;
        this.documentGenerationService = documentGenerationService;
        this.documentWorkspaceService = documentWorkspaceService;
        this.exportService = exportService;
        this.projectHistoryService = projectHistoryService;
    }

    /**
     * Creates a new project in DRAFT status for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProjectResponse response = projectService.createProject(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all projects owned by the authenticated user, or searches/filters/paginates if parameters provided.
     */
    @GetMapping
    public ResponseEntity<?> getMyProjects(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) ProjectType type,
            @RequestParam(required = false) ProjectDifficulty difficulty,
            @RequestParam(required = false, defaultValue = "updatedAt") String sort,
            @RequestParam(required = false, defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (page == null && search == null && status == null && type == null && difficulty == null &&
            (sort == null || sort.equals("updatedAt")) && (direction == null || direction.equals("desc"))) {
            List<ProjectSummaryResponse> projects = projectService.getUserProjects(userDetails.getUsername());
            return ResponseEntity.ok(projects);
        }
        Object result = projectService.searchAndFilterProjects(
                userDetails.getUsername(), search, status, type, difficulty, sort, direction, page, size
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves summary dashboard metrics for all projects owned by the authenticated user.
     */
    @GetMapping("/stats")
    public ResponseEntity<ProjectStatsResponse> getProjectStats(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProjectStatsResponse stats = projectService.getProjectStats(userDetails.getUsername());
        return ResponseEntity.ok(stats);
    }

    /**
     * Retrieves an owned project by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProjectResponse response = projectService.getProjectById(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves paginated lifecycle history events for an owned project ordered newest first.
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<PagedResponse<ProjectHistoryResponse>> getProjectHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (projectHistoryService == null) {
            return ResponseEntity.ok(new PagedResponse<>(List.of(), 0, size, 0, 0, true, true));
        }
        PagedResponse<ProjectHistoryResponse> history = projectHistoryService.getProjectHistory(
                id, userDetails.getUsername(), page, size
        );
        return ResponseEntity.ok(history);
    }

    /**
     * Updates an owned project if in an editable status.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProjectResponse response = projectService.updateProject(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes an owned project.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        projectService.deleteProject(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Prepares an owned project for future AI analysis without invoking external AI providers.
     */
    @PostMapping("/{id}/prepare-analysis")
    public ResponseEntity<ProjectResponse> prepareForAnalysis(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProjectResponse response = projectService.prepareForAnalysis(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the structured JSON specification for an owned project.
     */
    @GetMapping("/{id}/specification")
    public ResponseEntity<ProjectSpecificationResponse> getSpecification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProjectSpecificationResponse response = projectService.getProjectSpecification(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the structured JSON specification for an owned project.
     */
    @PutMapping("/{id}/specification")
    public ResponseEntity<ProjectSpecificationResponse> updateSpecification(
            @PathVariable Long id,
            @Valid @RequestBody ProjectSpecificationUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProjectSpecificationResponse response = projectService.updateProjectSpecification(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Executes AI requirements analysis on an owned project.
     */
    @PostMapping("/{id}/analyze")
    public ResponseEntity<RequirementsAnalysisResponse> analyzeProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        RequirementsAnalysisResponse response = projectRequirementsService.analyzeProject(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves structured requirements analysis for an owned project.
     */
    @GetMapping("/{id}/requirements")
    public ResponseEntity<RequirementsAnalysisResponse> getRequirements(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        RequirementsAnalysisResponse response = projectRequirementsService.getRequirements(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Submits clarification answers to open questions on an owned project.
     */
    @PostMapping("/{id}/clarifications")
    public ResponseEntity<RequirementsAnalysisResponse> submitClarifications(
            @PathVariable Long id,
            @Valid @RequestBody ClarificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        RequirementsAnalysisResponse response = projectRequirementsService.submitClarifications(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves human review data including specification, quality score, and blocking questions.
     */
    @GetMapping("/{id}/review")
    public ResponseEntity<ProjectReviewResponse> getReview(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProjectReviewResponse response = projectRequirementsService.getReview(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Updates specification during human-in-the-loop review.
     */
    @PutMapping("/{id}/review")
    public ResponseEntity<ProjectReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ProjectReviewUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProjectReviewResponse response = projectRequirementsService.updateReview(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Explicitly approves project requirements, locking specification as single source of truth.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApprovalResponse> approveProject(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ApprovalRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ApprovalRequest safeRequest = request != null ? request : new ApprovalRequest(true);
        ApprovalResponse response = projectRequirementsService.approveProject(id, safeRequest, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Generates consistent Markdown documentation package from the approved project specification.
     */
    @PostMapping("/{id}/generate")
    public ResponseEntity<DocumentGenerationResponse> generateDocumentation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        DocumentGenerationResponse response = documentGenerationService.generateDocumentation(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all generated documentation files for an owned project.
     */
    @GetMapping("/{id}/documents")
    public ResponseEntity<List<DocumentSummaryResponse>> getProjectDocuments(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<DocumentSummaryResponse> response = documentGenerationService.getProjectDocuments(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves workspace overview for an owned project.
     */
    @GetMapping("/{id}/workspace")
    public ResponseEntity<WorkspaceResponse> getWorkspace(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        WorkspaceResponse response = documentWorkspaceService.getWorkspace(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Prepares ZIP export package metadata for an owned project.
     */
    @PostMapping("/{id}/export")
    public ResponseEntity<ExportResponse> prepareExport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ExportResponse response = exportService.prepareExport(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Generates and downloads the binary ZIP package containing current active Markdown documentation.
     */
    @GetMapping("/{id}/export/download")
    public ResponseEntity<byte[]> downloadExport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ExportService.ExportPackage exportPackage = exportService.generateExportPackage(id, userDetails.getUsername());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportPackage.filename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(exportPackage.content().length))
                .body(exportPackage.content());
    }

    /**
     * Convenience endpoint for direct GET /api/projects/{id}/export download.
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportDirect(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return downloadExport(id, userDetails);
    }
}
