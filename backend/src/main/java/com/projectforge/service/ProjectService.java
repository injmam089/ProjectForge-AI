package com.projectforge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.dto.CreateProjectRequest;
import com.projectforge.dto.ProjectCreateRequest;
import com.projectforge.dto.ProjectResponse;
import com.projectforge.dto.ProjectSpecificationResponse;
import com.projectforge.dto.ProjectSpecificationUpdateRequest;
import com.projectforge.dto.ProjectSummaryResponse;
import com.projectforge.dto.ProjectUpdateRequest;
import com.projectforge.dto.UpdateProjectRequest;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.ProjectType;
import com.projectforge.entity.User;
import com.projectforge.exception.InvalidProjectStateException;
import com.projectforge.exception.ProjectForgeException;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.exception.ProjectValidationException;
import com.projectforge.exception.ResourceNotFoundException;
import com.projectforge.repository.ProjectRepository;
import com.projectforge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing project lifecycle and enforcing strict ownership boundaries.
 *
 * Core Security:
 * - Every project query and mutation is strictly partitioned by the authenticated user's ID.
 * - Unauthorized cross-user requests (IDOR attempts) throw ProjectNotFoundException (404 Not Found)
 *   to avoid leaking the existence of other users' projects.
 */
@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new Project in DRAFT status for the authenticated user.
     */
    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request, String userEmail) {
        User user = resolveUser(userEmail);

        Project project = new Project();
        project.setUser(user);
        project.setName(request.name().trim());
        project.setDescription(request.description().trim());
        project.setProblemStatement(request.problemStatement() != null ? request.problemStatement().trim() : null);
        project.setTargetUsers(request.targetUsers() != null ? request.targetUsers().trim() : null);
        project.setProjectType(request.projectType() != null ? request.projectType() : ProjectType.WEB_APPLICATION);
        project.setDifficulty(request.difficulty() != null ? request.difficulty() : ProjectDifficulty.INTERMEDIATE);
        project.setPreferredTechStack(request.preferredTechStack() != null ? request.preferredTechStack().trim() : null);
        project.setSpecialRequirements(request.specialRequirements() != null ? request.specialRequirements().trim() : null);
        project.setStatus(ProjectStatus.DRAFT);
        project.setProjectSpecification(null);

        Project savedProject = projectRepository.save(project);
        log.info("Project created successfully with id: {} for user: {}", savedProject.getId(), user.getId());

        return ProjectResponse.fromEntity(savedProject);
    }

    /**
     * Backward-compatible overload for CreateProjectRequest.
     */
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, String userEmail) {
        return createProject(request.toProjectCreateRequest(), userEmail);
    }

    /**
     * Lists all projects owned by the authenticated user, ordered by most recently updated.
     */
    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> getUserProjects(String userEmail) {
        User user = resolveUser(userEmail);
        List<Project> projects = projectRepository.findAllByUserIdOrderByUpdatedAtDesc(user.getId());
        return projects.stream()
                .map(ProjectSummaryResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves an owned project by ID.
     * Throws ProjectNotFoundException (404) if the project does not exist or belongs to another user.
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId, String userEmail) {
        User user = resolveUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        return ProjectResponse.fromEntity(project);
    }

    /**
     * Updates an owned project if in an editable state (DRAFT or READY_FOR_ANALYSIS).
     */
    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectUpdateRequest request, String userEmail) {
        User user = resolveUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (!project.getStatus().canBeEdited()) {
            throw new InvalidProjectStateException("Project cannot be edited in current status: " + project.getStatus());
        }

        project.setName(request.name().trim());
        project.setDescription(request.description().trim());
        project.setProblemStatement(request.problemStatement() != null ? request.problemStatement().trim() : null);
        project.setTargetUsers(request.targetUsers() != null ? request.targetUsers().trim() : null);
        if (request.projectType() != null) {
            project.setProjectType(request.projectType());
        }
        if (request.difficulty() != null) {
            project.setDifficulty(request.difficulty());
        }
        project.setPreferredTechStack(request.preferredTechStack() != null ? request.preferredTechStack().trim() : null);
        project.setSpecialRequirements(request.specialRequirements() != null ? request.specialRequirements().trim() : null);

        Project updatedProject = projectRepository.save(project);
        log.info("Project id: {} updated successfully by user: {}", updatedProject.getId(), user.getId());

        return ProjectResponse.fromEntity(updatedProject);
    }

    /**
     * Backward-compatible overload for UpdateProjectRequest.
     */
    @Transactional
    public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request, String userEmail) {
        return updateProject(projectId, request.toProjectUpdateRequest(), userEmail);
    }

    /**
     * Deletes an owned project.
     * Throws ProjectNotFoundException (404) if not found or not owned.
     */
    @Transactional
    public void deleteProject(Long projectId, String userEmail) {
        User user = resolveUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        projectRepository.delete(project);
        log.info("Project id: {} deleted successfully by user: {}", projectId, user.getId());
    }

    /**
     * Prepares an owned project for future AI analysis without calling external AI providers.
     * Validates minimum requirements information and transitions status to READY_FOR_ANALYSIS.
     */
    @Transactional
    public ProjectResponse prepareForAnalysis(Long projectId, String userEmail) {
        User user = resolveUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (!project.getStatus().canPrepareForAnalysis()) {
            throw new InvalidProjectStateException("Project cannot be prepared for analysis from status: " + project.getStatus());
        }

        if (project.getName() == null || project.getName().trim().length() < 2) {
            throw new ProjectValidationException("Project name must have at least 2 characters for analysis");
        }

        if (project.getDescription() == null || project.getDescription().trim().length() < 10) {
            throw new ProjectValidationException("Project description must have at least 10 characters for analysis");
        }

        if (project.getProjectType() == null) {
            throw new ProjectValidationException("Project type must be specified for analysis");
        }

        if (project.getDifficulty() == null) {
            throw new ProjectValidationException("Project difficulty must be specified for analysis");
        }

        project.setStatus(ProjectStatus.READY_FOR_ANALYSIS);
        Project savedProject = projectRepository.save(project);
        log.info("Project id: {} successfully prepared for analysis by user: {}", projectId, user.getId());

        return ProjectResponse.fromEntity(savedProject);
    }

    /**
     * Retrieves the structured JSON specification for an owned project.
     */
    @Transactional(readOnly = true)
    public ProjectSpecificationResponse getProjectSpecification(Long projectId, String userEmail) {
        User user = resolveUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        JsonNode specNode = null;
        if (project.getProjectSpecification() != null && !project.getProjectSpecification().trim().isEmpty()) {
            try {
                specNode = objectMapper.readTree(project.getProjectSpecification());
            } catch (JsonProcessingException e) {
                log.warn("Corrupted project specification stored for project id: {}", projectId);
            }
        }

        return ProjectSpecificationResponse.of(project, specNode);
    }

    /**
     * Updates the structured JSON specification for an owned project.
     */
    @Transactional
    public ProjectSpecificationResponse updateProjectSpecification(
            Long projectId,
            ProjectSpecificationUpdateRequest request,
            String userEmail
    ) {
        User user = resolveUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (!project.getStatus().canEditSpecification()) {
            throw new InvalidProjectStateException("Cannot update specification for project in protected status: " + project.getStatus());
        }

        String serializedSpec;
        try {
            serializedSpec = objectMapper.writeValueAsString(request.specification());
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize project specification for project id: {}", projectId);
            throw new ProjectForgeException("Invalid project specification JSON format");
        }

        project.setProjectSpecification(serializedSpec);
        Project savedProject = projectRepository.save(project);
        log.info("Specification updated for project id: {} by user: {}", projectId, user.getId());

        return ProjectSpecificationResponse.of(savedProject, request.specification());
    }

    private User resolveUser(String email) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", normalizedEmail));
    }
}
