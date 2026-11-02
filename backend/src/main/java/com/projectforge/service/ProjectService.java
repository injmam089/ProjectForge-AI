package com.projectforge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.dto.CreateProjectRequest;
import com.projectforge.dto.PagedResponse;
import com.projectforge.dto.ProjectCreateRequest;
import com.projectforge.dto.ProjectResponse;
import com.projectforge.dto.ProjectSpecificationResponse;
import com.projectforge.dto.ProjectSpecificationUpdateRequest;
import com.projectforge.dto.ProjectStatsResponse;
import com.projectforge.dto.ProjectSummaryResponse;
import com.projectforge.dto.ProjectUpdateRequest;
import com.projectforge.dto.UpdateProjectRequest;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectHistoryEventType;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.ProjectType;
import com.projectforge.entity.User;
import com.projectforge.exception.InvalidProjectStateException;
import com.projectforge.exception.ProjectForgeException;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.exception.ProjectValidationException;
import com.projectforge.exception.ResourceNotFoundException;
import com.projectforge.repository.DocumentRepository;
import com.projectforge.repository.DocumentVersionRepository;
import com.projectforge.repository.ProjectHistoryRepository;
import com.projectforge.repository.ProjectRepository;
import com.projectforge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern QUALITY_SCORE_PATTERN = Pattern.compile("\"qualityScore\"\\s*:\\s*(\\d+)");

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final ProjectHistoryRepository projectHistoryRepository;
    private final ProjectHistoryService projectHistoryService;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this(projectRepository, userRepository, objectMapper, null, null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            DocumentRepository documentRepository,
            DocumentVersionRepository documentVersionRepository,
            ProjectHistoryRepository projectHistoryRepository,
            ProjectHistoryService projectHistoryService
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.projectHistoryRepository = projectHistoryRepository;
        this.projectHistoryService = projectHistoryService;
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

        if (projectHistoryService != null) {
            try {
                projectHistoryService.recordEvent(
                        savedProject,
                        ProjectHistoryEventType.PROJECT_CREATED,
                        String.format("Project '%s' created", savedProject.getName()),
                        null,
                        user.getEmail()
                );
            } catch (Exception e) {
                log.warn("Failed to record history event for project creation: {}", e.getMessage());
            }
        }

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
        List<Long> projectIds = projects.stream().map(Project::getId).toList();
        Map<Long, Integer> docCounts = getDocumentCountsForProjects(projectIds);

        return projects.stream()
                .map(p -> ProjectSummaryResponse.fromEntity(p, docCounts.getOrDefault(p.getId(), 0), extractQualityScore(p)))
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
     * Safely deletes all owned child data (documents, document versions, project history) to prevent FK constraint violations.
     * Throws ProjectNotFoundException (404) if not found or not owned.
     */
    @Transactional
    public void deleteProject(Long projectId, String userEmail) {
        User user = resolveUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        // Cascade cleanup of child entities
        if (documentVersionRepository != null) {
            documentVersionRepository.deleteAllByProjectId(projectId);
        }
        if (documentRepository != null) {
            documentRepository.deleteAllByProjectId(projectId);
        }
        if (projectHistoryRepository != null) {
            projectHistoryRepository.deleteAllByProjectId(projectId);
        }

        projectRepository.delete(project);
        log.info("Project id: {} and all associated child entities deleted successfully by user: {}", projectId, user.getId());
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

        if (project.getName() == null || project.getName().trim().length() < 3) {
            throw new ProjectValidationException("Project name must be at least 3 characters");
        }
        if (project.getDescription() == null || project.getDescription().trim().length() < 10) {
            throw new ProjectValidationException("Project description must be at least 10 characters");
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

        if (projectHistoryService != null) {
            try {
                projectHistoryService.recordEvent(
                        savedProject,
                        ProjectHistoryEventType.ANALYSIS_STARTED,
                        "Project requirements validated and prepared for AI analysis",
                        null,
                        user.getEmail()
                );
            } catch (Exception e) {
                log.warn("Failed to record history event for prepare analysis: {}", e.getMessage());
            }
        }

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

    /**
     * Retrieves summary statistics for all projects owned by the authenticated user.
     */
    @Transactional(readOnly = true)
    public ProjectStatsResponse getProjectStats(String userEmail) {
        User user = resolveUser(userEmail);
        Long userId = user.getId();

        long total = projectRepository.countByUserId(userId);
        long draft = projectRepository.countByUserIdAndStatus(userId, ProjectStatus.DRAFT);
        long readyForAnalysis = projectRepository.countByUserIdAndStatus(userId, ProjectStatus.READY_FOR_ANALYSIS);
        long analyzing = projectRepository.countByUserIdAndStatus(userId, ProjectStatus.ANALYZING);
        long underReview = projectRepository.countByUserIdAndStatus(userId, ProjectStatus.UNDER_REVIEW);
        long requirementsReady = projectRepository.countByUserIdAndStatus(userId, ProjectStatus.REQUIREMENTS_READY);
        long approved = projectRepository.countByUserIdAndStatus(userId, ProjectStatus.APPROVED);
        long generating = projectRepository.countByUserIdAndStatus(userId, ProjectStatus.GENERATING);
        long generated = projectRepository.countByUserIdAndStatus(userId, ProjectStatus.GENERATED);
        long exported = projectRepository.countByUserIdAndStatus(userId, ProjectStatus.EXPORTED);

        return new ProjectStatsResponse(
                total, draft, readyForAnalysis, analyzing, underReview,
                requirementsReady, approved, generating, generated, exported
        );
    }

    /**
     * Searches, filters, sorts, and optionally paginates projects owned by the user.
     * Enforces safe sorting allowlist and bounded page size.
     */
    @Transactional(readOnly = true)
    public Object searchAndFilterProjects(
            String userEmail,
            String search,
            ProjectStatus status,
            ProjectType type,
            ProjectDifficulty difficulty,
            String sortField,
            String direction,
            Integer page,
            Integer size
    ) {
        User user = resolveUser(userEmail);
        Long userId = user.getId();

        // Strict sort allowlist
        String safeSortField = "updatedAt";
        if (sortField != null && !sortField.isBlank()) {
            String trimmed = sortField.trim();
            if ("createdAt".equalsIgnoreCase(trimmed) || "name".equalsIgnoreCase(trimmed) ||
                "status".equalsIgnoreCase(trimmed) || "updatedAt".equalsIgnoreCase(trimmed) ||
                "created_at".equalsIgnoreCase(trimmed) || "updated_at".equalsIgnoreCase(trimmed)) {
                if ("status".equalsIgnoreCase(trimmed)) safeSortField = "status";
                else if ("name".equalsIgnoreCase(trimmed)) safeSortField = "name";
                else if ("createdAt".equalsIgnoreCase(trimmed) || "created_at".equalsIgnoreCase(trimmed)) safeSortField = "createdAt";
                else safeSortField = "updatedAt";
            } else {
                throw new IllegalArgumentException("Invalid sort field: '" + sortField + "'. Allowed sort fields are: updatedAt, createdAt, name, status.");
            }
        }
        Sort.Direction sortDir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(sortDir, safeSortField);

        // Sanitize search keyword (max 100 characters)
        String safeSearch = search != null && !search.isBlank() ? search.trim() : null;
        if (safeSearch != null && safeSearch.length() > 100) {
            safeSearch = safeSearch.substring(0, 100);
        }

        if (page != null) {
            int safePage = Math.max(0, page);
            int safeSize = size != null ? Math.min(100, Math.max(1, size)) : 12;
            Pageable pageable = PageRequest.of(safePage, safeSize, sort);
            Page<Project> projectPage = projectRepository.findWithFilters(userId, status, type, difficulty, safeSearch, pageable);

            List<Long> projectIds = projectPage.getContent().stream().map(Project::getId).toList();
            Map<Long, Integer> docCounts = getDocumentCountsForProjects(projectIds);

            Page<ProjectSummaryResponse> summaryPage = projectPage.map(p ->
                    ProjectSummaryResponse.fromEntity(p, docCounts.getOrDefault(p.getId(), 0), extractQualityScore(p))
            );
            return PagedResponse.fromPage(summaryPage);
        } else {
            List<Project> projects = projectRepository.findWithFilters(userId, status, type, difficulty, safeSearch, sort);
            List<Long> projectIds = projects.stream().map(Project::getId).toList();
            Map<Long, Integer> docCounts = getDocumentCountsForProjects(projectIds);

            return projects.stream()
                    .map(p -> ProjectSummaryResponse.fromEntity(p, docCounts.getOrDefault(p.getId(), 0), extractQualityScore(p)))
                    .toList();
        }
    }

    private Map<Long, Integer> getDocumentCountsForProjects(List<Long> projectIds) {
        if (documentRepository == null || projectIds == null || projectIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<Object[]> results = documentRepository.countByProjectIds(projectIds);
            Map<Long, Integer> counts = new HashMap<>();
            for (Object[] row : results) {
                if (row.length >= 2 && row[0] instanceof Long id && row[1] instanceof Number count) {
                    counts.put(id, count.intValue());
                }
            }
            return counts;
        } catch (Exception e) {
            log.warn("Failed to batch count documents: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Integer extractQualityScore(Project project) {
        String spec = project.getProjectSpecification();
        if (spec == null || spec.isBlank()) {
            return null;
        }
        Matcher matcher = QUALITY_SCORE_PATTERN.matcher(spec);
        if (matcher.find()) {
            try {
                return Integer.valueOf(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private User resolveUser(String email) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", normalizedEmail));
    }
}
