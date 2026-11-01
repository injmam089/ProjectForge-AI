package com.projectforge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.ai.ClarificationEngine;
import com.projectforge.ai.QualityEvaluator;
import com.projectforge.ai.RequirementAnalyzer;
import com.projectforge.ai.RequirementValidator;
import com.projectforge.dto.ApprovalRequest;
import com.projectforge.dto.ApprovalResponse;
import com.projectforge.dto.ClarificationRequest;
import com.projectforge.dto.OpenQuestionDto;
import com.projectforge.dto.ProjectReviewResponse;
import com.projectforge.dto.ProjectReviewUpdateRequest;
import com.projectforge.dto.QualityScoreDto;
import com.projectforge.dto.RequirementsAnalysisResponse;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.User;
import com.projectforge.exception.AIServiceException;
import com.projectforge.exception.InvalidProjectStateException;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.exception.ProjectValidationException;
import com.projectforge.repository.ProjectRepository;
import com.projectforge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Service orchestrating AI requirement analysis, clarification questions, and
 * structured specification management for authenticated user projects.
 */
@Service
public class ProjectRequirementsService {

    private static final Logger log = LoggerFactory.getLogger(ProjectRequirementsService.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final RequirementAnalyzer requirementAnalyzer;
    private final ClarificationEngine clarificationEngine;
    private final RequirementValidator requirementValidator;
    private final QualityEvaluator qualityEvaluator;
    private final ObjectMapper objectMapper;

    @Autowired
    public ProjectRequirementsService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            RequirementAnalyzer requirementAnalyzer,
            ClarificationEngine clarificationEngine,
            RequirementValidator requirementValidator,
            QualityEvaluator qualityEvaluator,
            ObjectMapper objectMapper
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.requirementAnalyzer = requirementAnalyzer;
        this.clarificationEngine = clarificationEngine;
        this.requirementValidator = requirementValidator;
        this.qualityEvaluator = qualityEvaluator;
        this.objectMapper = objectMapper;
    }

    public ProjectRequirementsService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            RequirementAnalyzer requirementAnalyzer,
            ClarificationEngine clarificationEngine,
            ObjectMapper objectMapper
    ) {
        this(projectRepository, userRepository, requirementAnalyzer, clarificationEngine,
                new RequirementValidator(), new QualityEvaluator(), objectMapper);
    }

    /**
     * Executes AI requirements analysis on an owned project.
     * Enforces ownership, valid lifecycle state, and anti-IDOR protection.
     *
     * @param projectId the project ID
     * @param userEmail the authenticated user's email
     * @return the structured RequirementsAnalysisResponse
     */
    @Transactional
    public RequirementsAnalysisResponse analyzeProject(Long projectId, String userEmail) {
        User user = resolveUser(userEmail);
        Project project = findOwnedProject(projectId, user);

        // State validation
        if (project.getStatus() == ProjectStatus.ANALYZING) {
            throw new InvalidProjectStateException("Analysis is already currently executing for project id: " + projectId);
        }
        if (project.getStatus() == ProjectStatus.APPROVED ||
                project.getStatus() == ProjectStatus.GENERATING ||
                project.getStatus() == ProjectStatus.GENERATED ||
                project.getStatus() == ProjectStatus.EXPORTED) {
            throw new InvalidProjectStateException("Cannot re-analyze project in status: " + project.getStatus());
        }

        // Set status to ANALYZING
        project.setStatus(ProjectStatus.ANALYZING);
        projectRepository.save(project);

        try {
            // Run AI analysis
            RequirementAnalyzer.AnalysisResult result = requirementAnalyzer.analyze(project);

            // Persist specification JSON
            project.setProjectSpecification(objectMapper.writeValueAsString(result.specification()));

            // Determine whether unresolved critical or high questions block requirements readiness
            ProjectStatus nextStatus = determineRequirementsStatus(result.openQuestions());
            project.setStatus(nextStatus);
            Project savedProject = projectRepository.save(project);

            String message = (nextStatus == ProjectStatus.UNDER_REVIEW)
                    ? "AI requirements analysis completed. Unresolved critical/high decisions require clarification before requirements are ready."
                    : "AI requirements analysis completed successfully. Requirements are ready.";

            log.info("Project id: {} transitioned to {} with quality score: {}",
                    savedProject.getId(), nextStatus, result.qualityScore());

            return new RequirementsAnalysisResponse(
                    savedProject.getId(),
                    savedProject.getStatus(),
                    result.qualityScore(),
                    result.specification(),
                    result.openQuestions(),
                    message
            );

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize analyzed specification for project id: {}", projectId, e);
            project.setStatus(ProjectStatus.READY_FOR_ANALYSIS);
            projectRepository.save(project);
            throw new AIServiceException("Failed to persist structured specification", e);
        } catch (RuntimeException e) {
            log.error("AI analysis failed for project id: {}", projectId, e);
            // Revert status to READY_FOR_ANALYSIS so user can retry safely
            project.setStatus(ProjectStatus.READY_FOR_ANALYSIS);
            projectRepository.save(project);
            throw e;
        }
    }

    /**
     * Retrieves current structured requirements and open questions for an owned project.
     *
     * @param projectId the project ID
     * @param userEmail the authenticated user's email
     * @return the structured RequirementsAnalysisResponse
     */
    @Transactional(readOnly = true)
    public RequirementsAnalysisResponse getRequirements(Long projectId, String userEmail) {
        User user = resolveUser(userEmail);
        Project project = findOwnedProject(projectId, user);

        if (project.getProjectSpecification() == null || project.getProjectSpecification().isBlank()) {
            throw new InvalidProjectStateException("No requirements analysis has been generated for project id: " + projectId);
        }

        try {
            JsonNode specNode = objectMapper.readTree(project.getProjectSpecification());
            int qualityScore = specNode.path("qualityScore").asInt(75);
            List<OpenQuestionDto> openQuestions = requirementAnalyzer.extractOpenQuestions(specNode);

            return new RequirementsAnalysisResponse(
                    project.getId(),
                    project.getStatus(),
                    qualityScore,
                    specNode,
                    openQuestions,
                    "Project requirements retrieved successfully."
            );
        } catch (JsonProcessingException e) {
            throw new AIServiceException("Corrupted specification in database for project id: " + projectId, e);
        }
    }

    /**
     * Submits clarification answers to open questions and refines the structured specification.
     *
     * @param projectId the project ID
     * @param request the user answers
     * @param userEmail the authenticated user's email
     * @return the updated RequirementsAnalysisResponse
     */
    @Transactional
    public RequirementsAnalysisResponse submitClarifications(Long projectId, ClarificationRequest request, String userEmail) {
        User user = resolveUser(userEmail);
        Project project = findOwnedProject(projectId, user);

        if (project.getProjectSpecification() == null || project.getProjectSpecification().isBlank()) {
            throw new InvalidProjectStateException("Cannot submit clarifications: No requirements analysis exists for project id: " + projectId);
        }

        if (project.getStatus() == ProjectStatus.ANALYZING) {
            throw new InvalidProjectStateException("Analysis is currently in progress for project id: " + projectId);
        }
        if (project.getStatus() == ProjectStatus.APPROVED ||
                project.getStatus() == ProjectStatus.GENERATING ||
                project.getStatus() == ProjectStatus.GENERATED ||
                project.getStatus() == ProjectStatus.EXPORTED) {
            throw new InvalidProjectStateException("Cannot submit clarifications for project in status: " + project.getStatus());
        }

        try {
            RequirementAnalyzer.AnalysisResult result = clarificationEngine.clarify(project, request);

            project.setProjectSpecification(objectMapper.writeValueAsString(result.specification()));
            ProjectStatus nextStatus = determineRequirementsStatus(result.openQuestions());
            project.setStatus(nextStatus);
            Project savedProject = projectRepository.save(project);

            String message = (nextStatus == ProjectStatus.UNDER_REVIEW)
                    ? "Clarifications incorporated, but additional critical/high decisions require clarification before requirements are ready."
                    : "Clarifications incorporated successfully. Project requirements are ready.";

            log.info("Project id: {} clarified and updated with new status: {} and quality score: {}",
                    savedProject.getId(), nextStatus, result.qualityScore());

            return new RequirementsAnalysisResponse(
                    savedProject.getId(),
                    savedProject.getStatus(),
                    result.qualityScore(),
                    result.specification(),
                    result.openQuestions(),
                    message
            );

        } catch (JsonProcessingException e) {
            log.error("Failed to persist clarified specification for project id: {}", projectId, e);
            throw new AIServiceException("Failed to persist updated specification", e);
        } catch (RuntimeException e) {
            log.error("Clarification failed for project id: {}. Stored specification preserved.", projectId, e);
            throw e;
        }
    }

    /**
     * Retrieves the human-in-the-loop review summary for an owned project.
     * Enforces ownership, anti-IDOR, and computes blocking questions and approval eligibility.
     *
     * @param projectId the project ID
     * @param userEmail the authenticated user's email
     * @return the structured ProjectReviewResponse
     */
    @Transactional(readOnly = true)
    public ProjectReviewResponse getReview(Long projectId, String userEmail) {
        User user = resolveUser(userEmail);
        Project project = findOwnedProject(projectId, user);

        if (project.getProjectSpecification() == null || project.getProjectSpecification().isBlank()) {
            throw new InvalidProjectStateException("No requirements specification exists yet for project id: " + projectId);
        }

        try {
            JsonNode specNode = objectMapper.readTree(project.getProjectSpecification());
            QualityScoreDto qualityDto = qualityEvaluator.evaluate(specNode);
            List<OpenQuestionDto> openQuestions = requirementAnalyzer.extractOpenQuestions(specNode);
            List<OpenQuestionDto> blockingQuestions = openQuestions.stream()
                    .filter(q -> q.importance() != null &&
                            ("CRITICAL".equalsIgnoreCase(q.importance().trim()) || "HIGH".equalsIgnoreCase(q.importance().trim())))
                    .toList();

            boolean approved = (project.getStatus() == ProjectStatus.APPROVED);
            boolean canApprove = !approved && project.getStatus().canBeApproved() && blockingQuestions.isEmpty();

            String message;
            if (approved) {
                message = "Project requirements are approved and locked as the single source of truth.";
            } else if (blockingQuestions.isEmpty()) {
                message = "Specification is ready for human review and explicit approval.";
            } else {
                message = "Specification is under review. " + blockingQuestions.size()
                        + " blocking clarification question(s) must be resolved prior to approval.";
            }

            return new ProjectReviewResponse(
                    project.getId(),
                    project.getStatus(),
                    qualityDto.overallScore(),
                    qualityDto,
                    specNode,
                    openQuestions,
                    blockingQuestions,
                    approved,
                    canApprove,
                    message
            );
        } catch (JsonProcessingException e) {
            throw new AIServiceException("Corrupted specification in database for project id: " + projectId, e);
        }
    }

    /**
     * Updates and validates the specification during human review.
     * Recalculates quality score server-side, validates schema, evaluates blocking questions,
     * updates lifecycle status, and prevents modification of approved specifications.
     *
     * @param projectId the project ID
     * @param request the specification update request
     * @param userEmail the authenticated user's email
     * @return the updated ProjectReviewResponse
     */
    @Transactional
    public ProjectReviewResponse updateReview(Long projectId, ProjectReviewUpdateRequest request, String userEmail) {
        User user = resolveUser(userEmail);
        Project project = findOwnedProject(projectId, user);

        if (project.getStatus() == ProjectStatus.APPROVED ||
                project.getStatus() == ProjectStatus.GENERATING ||
                project.getStatus() == ProjectStatus.GENERATED ||
                project.getStatus() == ProjectStatus.EXPORTED) {
            throw new InvalidProjectStateException("Cannot modify specification: Project is in protected status "
                    + project.getStatus() + ". Approved requirements are locked.");
        }

        if (project.getStatus() == ProjectStatus.ANALYZING) {
            throw new InvalidProjectStateException("Analysis is currently executing for project id: " + projectId);
        }

        if (request == null || request.specification() == null || !request.specification().isObject()) {
            throw new ProjectValidationException("Specification payload must be a non-empty JSON object.");
        }

        JsonNode specNode = request.specification().deepCopy();

        // Validate structure and required sections
        try {
            requirementValidator.validate(specNode);
        } catch (RuntimeException e) {
            log.warn("Specification validation failed during review update for project id {}: {}", projectId, e.getMessage());
            throw new ProjectValidationException("Invalid specification structure: " + e.getMessage());
        }

        // Server-side deterministic quality evaluation (replaces/stamps qualityScore & qualityBreakdown)
        QualityScoreDto qualityDto = qualityEvaluator.evaluate(specNode);

        // Extract open questions
        List<OpenQuestionDto> openQuestions = requirementAnalyzer.extractOpenQuestions(specNode);
        List<OpenQuestionDto> blockingQuestions = openQuestions.stream()
                .filter(q -> q.importance() != null &&
                        ("CRITICAL".equalsIgnoreCase(q.importance().trim()) || "HIGH".equalsIgnoreCase(q.importance().trim())))
                .toList();

        // Determine next lifecycle state based on blocking questions
        ProjectStatus nextStatus = determineRequirementsStatus(openQuestions);
        project.setStatus(nextStatus);

        try {
            project.setProjectSpecification(objectMapper.writeValueAsString(specNode));
        } catch (JsonProcessingException e) {
            throw new ProjectValidationException("Failed to serialize updated specification JSON: " + e.getMessage());
        }

        Project savedProject = projectRepository.save(project);
        log.info("Project id: {} updated via review in status: {} with quality score: {}",
                savedProject.getId(), nextStatus, qualityDto.overallScore());

        boolean approved = false;
        boolean canApprove = blockingQuestions.isEmpty();
        String message = blockingQuestions.isEmpty()
                ? "Specification updated and validated successfully. Requirements are ready for approval."
                : "Specification updated. " + blockingQuestions.size() + " blocking clarification question(s) remain under review.";

        return new ProjectReviewResponse(
                savedProject.getId(),
                savedProject.getStatus(),
                qualityDto.overallScore(),
                qualityDto,
                specNode,
                openQuestions,
                blockingQuestions,
                approved,
                canApprove,
                message
        );
    }

    /**
     * Explicitly approves the structured project requirements as the single source of truth.
     * Enforces explicit user confirmation, ownership, valid state, validation pass,
     * zero blocking questions, and idempotent re-approvals.
     *
     * @param projectId the project ID
     * @param request the approval confirmation request
     * @param userEmail the authenticated user's email
     * @return the ApprovalResponse
     */
    @Transactional
    public ApprovalResponse approveProject(Long projectId, ApprovalRequest request, String userEmail) {
        if (request == null || request.confirmation() == null || !request.confirmation()) {
            throw new ProjectValidationException("Approval confirmation must be explicitly set to true");
        }

        User user = resolveUser(userEmail);
        Project project = findOwnedProject(projectId, user);

        // Idempotency: if already approved, safely return existing approval response
        if (project.getStatus() == ProjectStatus.APPROVED) {
            log.info("Project id: {} is already approved. Returning idempotent response.", projectId);
            JsonNode existingSpec;
            int existingScore = 80;
            try {
                existingSpec = objectMapper.readTree(project.getProjectSpecification());
                existingScore = existingSpec.path("qualityScore").asInt(80);
            } catch (Exception e) {
                existingSpec = objectMapper.createObjectNode();
            }
            return new ApprovalResponse(
                    project.getId(),
                    ProjectStatus.APPROVED,
                    true,
                    existingScore,
                    "Project requirements are already approved.",
                    existingSpec
            );
        }

        // Lifecycle state eligibility check
        if (!project.getStatus().canBeApproved()) {
            throw new InvalidProjectStateException("Cannot approve project from status: " + project.getStatus()
                    + ". Project must be in REQUIREMENTS_READY or UNDER_REVIEW state.");
        }

        // Specification presence check
        if (project.getProjectSpecification() == null || project.getProjectSpecification().isBlank()) {
            throw new InvalidProjectStateException("Cannot approve project: No requirements specification exists.");
        }

        JsonNode specNode;
        try {
            specNode = objectMapper.readTree(project.getProjectSpecification());
        } catch (JsonProcessingException e) {
            throw new AIServiceException("Corrupted specification in database for project id: " + projectId, e);
        }

        // Validate specification schema
        try {
            requirementValidator.validate(specNode);
        } catch (RuntimeException e) {
            throw new InvalidProjectStateException("Cannot approve project: Specification fails validation checks: " + e.getMessage());
        }

        // Blocking questions check: must have NO unresolved CRITICAL or HIGH questions
        List<OpenQuestionDto> openQuestions = requirementAnalyzer.extractOpenQuestions(specNode);
        if (hasBlockingQuestions(openQuestions)) {
            List<OpenQuestionDto> blocking = openQuestions.stream()
                    .filter(q -> q.importance() != null &&
                            ("CRITICAL".equalsIgnoreCase(q.importance().trim()) || "HIGH".equalsIgnoreCase(q.importance().trim())))
                    .toList();
            throw new InvalidProjectStateException("Cannot approve project: " + blocking.size()
                    + " unresolved critical or high clarification questions remain. All blocking questions must be resolved prior to approval.");
        }

        // Transition to APPROVED
        project.setStatus(ProjectStatus.APPROVED);
        Project savedProject = projectRepository.save(project);
        int qualityScore = specNode.path("qualityScore").asInt(80);

        log.info("Project id: {} explicitly approved by user: {}", savedProject.getId(), user.getId());

        return new ApprovalResponse(
                savedProject.getId(),
                savedProject.getStatus(),
                true,
                qualityScore,
                "Project requirements approved successfully. The specification is locked as the single source of truth.",
                specNode
        );
    }

    /**
     * Determines whether unresolved critical or high questions block requirements readiness.
     * CRITICAL or HIGH questions place/keep project in UNDER_REVIEW.
     * LOW, MEDIUM, or empty questions allow transition to REQUIREMENTS_READY.
     *
     * @param openQuestions the list of open questions
     * @return the appropriate ProjectStatus
     */
    public ProjectStatus determineRequirementsStatus(List<OpenQuestionDto> openQuestions) {
        if (hasBlockingQuestions(openQuestions)) {
            return ProjectStatus.UNDER_REVIEW;
        }
        return ProjectStatus.REQUIREMENTS_READY;
    }

    /**
     * Checks if any open questions have CRITICAL or HIGH importance.
     *
     * @param openQuestions the list of open questions
     * @return true if any question is CRITICAL or HIGH, false otherwise
     */
    public boolean hasBlockingQuestions(List<OpenQuestionDto> openQuestions) {
        if (openQuestions == null || openQuestions.isEmpty()) {
            return false;
        }
        for (OpenQuestionDto q : openQuestions) {
            if (q != null && q.importance() != null) {
                String imp = q.importance().trim().toUpperCase();
                if ("CRITICAL".equals(imp) || "HIGH".equals(imp)) {
                    return true;
                }
            }
        }
        return false;
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    private Project findOwnedProject(Long projectId, User user) {
        return projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }
}
