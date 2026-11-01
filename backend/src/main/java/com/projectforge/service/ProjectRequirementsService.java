package com.projectforge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.ai.ClarificationEngine;
import com.projectforge.ai.RequirementAnalyzer;
import com.projectforge.dto.ClarificationRequest;
import com.projectforge.dto.OpenQuestionDto;
import com.projectforge.dto.RequirementsAnalysisResponse;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.User;
import com.projectforge.exception.AIServiceException;
import com.projectforge.exception.InvalidProjectStateException;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.repository.ProjectRepository;
import com.projectforge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final ObjectMapper objectMapper;

    public ProjectRequirementsService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            RequirementAnalyzer requirementAnalyzer,
            ClarificationEngine clarificationEngine,
            ObjectMapper objectMapper
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.requirementAnalyzer = requirementAnalyzer;
        this.clarificationEngine = clarificationEngine;
        this.objectMapper = objectMapper;
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
        if (project.getStatus() == ProjectStatus.GENERATING ||
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
        if (project.getStatus() == ProjectStatus.GENERATING ||
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
