package com.projectforge.controller;

import com.projectforge.dto.ClarificationRequest;
import com.projectforge.dto.ProjectCreateRequest;
import com.projectforge.dto.ProjectResponse;
import com.projectforge.dto.ProjectSpecificationResponse;
import com.projectforge.dto.ProjectSpecificationUpdateRequest;
import com.projectforge.dto.ProjectSummaryResponse;
import com.projectforge.dto.ProjectUpdateRequest;
import com.projectforge.dto.RequirementsAnalysisResponse;
import com.projectforge.service.ProjectRequirementsService;
import com.projectforge.service.ProjectService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller exposing authenticated project management, requirements intake,
 * and AI requirement analysis endpoints.
 * All routes require authentication and strictly enforce user ownership.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectRequirementsService projectRequirementsService;

    public ProjectController(ProjectService projectService) {
        this(projectService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ProjectController(ProjectService projectService, ProjectRequirementsService projectRequirementsService) {
        this.projectService = projectService;
        this.projectRequirementsService = projectRequirementsService;
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
     * Lists all projects owned by the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<ProjectSummaryResponse>> getMyProjects(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<ProjectSummaryResponse> projects = projectService.getUserProjects(userDetails.getUsername());
        return ResponseEntity.ok(projects);
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
     * Retrieves the current structured requirements and open questions for an owned project.
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
     * Submits user clarification answers to refine the structured requirements.
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
}
