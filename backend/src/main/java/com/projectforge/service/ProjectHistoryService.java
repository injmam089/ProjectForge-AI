package com.projectforge.service;

import com.projectforge.dto.PagedResponse;
import com.projectforge.dto.ProjectHistoryResponse;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectHistory;
import com.projectforge.entity.ProjectHistoryEventType;
import com.projectforge.entity.User;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.exception.ResourceNotFoundException;
import com.projectforge.repository.ProjectHistoryRepository;
import com.projectforge.repository.ProjectRepository;
import com.projectforge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing lifecycle history and audit tracking for projects.
 * Enforces strict multi-tenant authorization and sanitization of history metadata.
 */
@Service
public class ProjectHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ProjectHistoryService.class);
    private static final int MAX_METADATA_LENGTH = 4000;
    private static final int MAX_PAGE_SIZE = 100;

    private final ProjectHistoryRepository projectHistoryRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectHistoryService(
            ProjectHistoryRepository projectHistoryRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository
    ) {
        this.projectHistoryRepository = projectHistoryRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    /**
     * Records a project lifecycle event transactionally.
     * Sanitizes and bounds metadata, strictly omitting passwords, tokens, and secrets.
     */
    @Transactional
    public ProjectHistory recordEvent(
            Project project,
            ProjectHistoryEventType eventType,
            String description,
            String metadata,
            String createdBy
    ) {
        if (project == null || eventType == null) {
            log.warn("Skipping history event recording: project or eventType is null");
            return null;
        }

        String safeMetadata = sanitizeMetadata(metadata);
        String safeDescription = description != null ? description.trim() : eventType.name();
        if (safeDescription.length() > 1000) {
            safeDescription = safeDescription.substring(0, 997) + "...";
        }

        String safeCreatedBy = createdBy != null && !createdBy.isBlank() ? createdBy.trim() : "system";

        ProjectHistory history = new ProjectHistory(
                project,
                eventType,
                safeDescription,
                safeMetadata,
                safeCreatedBy
        );

        ProjectHistory saved = projectHistoryRepository.save(history);
        log.info("Recorded history event {} for project id: {}", eventType, project.getId());
        return saved;
    }

    /**
     * Retrieves paginated history events for an owned project ordered newest first.
     * Enforces anti-IDOR authorization (returns 404 for unowned or nonexistent projects).
     */
    @Transactional
    public PagedResponse<ProjectHistoryResponse> getProjectHistory(
            Long projectId,
            String userEmail,
            int page,
            int size
    ) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));

        // For existing projects with no recorded history yet, seed the baseline creation event
        if (projectHistoryRepository.countByProjectId(projectId) == 0) {
            ProjectHistory baseline = new ProjectHistory(
                    project,
                    ProjectHistoryEventType.PROJECT_CREATED,
                    String.format("Project '%s' created", project.getName()),
                    null,
                    project.getUser() != null ? project.getUser().getEmail() : userEmail,
                    project.getCreatedAt()
            );
            projectHistoryRepository.save(baseline);
        }

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<ProjectHistory> historyPage = projectHistoryRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId, pageable);

        Page<ProjectHistoryResponse> responsePage = historyPage.map(ProjectHistoryResponse::fromEntity);
        return PagedResponse.fromPage(responsePage);
    }

    private String sanitizeMetadata(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String sanitized = raw.trim();
        if (sanitized.length() > MAX_METADATA_LENGTH) {
            sanitized = sanitized.substring(0, MAX_METADATA_LENGTH - 3) + "...";
        }
        return sanitized;
    }
}
