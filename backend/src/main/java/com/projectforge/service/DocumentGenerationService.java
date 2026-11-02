package com.projectforge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.dto.DocumentGenerationResponse;
import com.projectforge.dto.DocumentResponse;
import com.projectforge.dto.DocumentSummaryResponse;
import com.projectforge.entity.Document;
import com.projectforge.entity.DocumentChangeType;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.DocumentVersion;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.User;
import com.projectforge.exception.DocumentGenerationException;
import com.projectforge.exception.InvalidProjectStateException;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.exception.ResourceNotFoundException;
import com.projectforge.markdown.DocumentPlanner;
import com.projectforge.markdown.GeneratedDocument;
import com.projectforge.markdown.MarkdownGenerator;
import com.projectforge.markdown.MarkdownValidator;
import com.projectforge.repository.DocumentRepository;
import com.projectforge.repository.DocumentVersionRepository;
import com.projectforge.repository.ProjectRepository;
import com.projectforge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service managing Markdown documentation generation from the approved project specification.
 * Enforces project ownership, lifecycle transitions (APPROVED -> GENERATING -> GENERATED),
 * deterministic document planning, validation, and safe error recovery.
 */
@Service
public class DocumentGenerationService {

    private static final Logger log = LoggerFactory.getLogger(DocumentGenerationService.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentPlanner documentPlanner;
    private final MarkdownGenerator markdownGenerator;
    private final MarkdownValidator markdownValidator;
    private final ObjectMapper objectMapper;
    private final DocumentVersionRepository documentVersionRepository;
    private final ProjectHistoryService projectHistoryService;

    public DocumentGenerationService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            DocumentRepository documentRepository,
            DocumentPlanner documentPlanner,
            MarkdownGenerator markdownGenerator,
            MarkdownValidator markdownValidator,
            ObjectMapper objectMapper,
            DocumentVersionRepository documentVersionRepository
    ) {
        this(projectRepository, userRepository, documentRepository, documentPlanner,
                markdownGenerator, markdownValidator, objectMapper, documentVersionRepository, null);
    }

    @Autowired
    public DocumentGenerationService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            DocumentRepository documentRepository,
            DocumentPlanner documentPlanner,
            MarkdownGenerator markdownGenerator,
            MarkdownValidator markdownValidator,
            ObjectMapper objectMapper,
            DocumentVersionRepository documentVersionRepository,
            ProjectHistoryService projectHistoryService
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.documentPlanner = documentPlanner;
        this.markdownGenerator = markdownGenerator;
        this.markdownValidator = markdownValidator;
        this.objectMapper = objectMapper;
        this.documentVersionRepository = documentVersionRepository;
        this.projectHistoryService = projectHistoryService;
    }

    public DocumentGenerationService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            DocumentRepository documentRepository,
            DocumentPlanner documentPlanner,
            MarkdownGenerator markdownGenerator,
            MarkdownValidator markdownValidator,
            ObjectMapper objectMapper
    ) {
        this(projectRepository, userRepository, documentRepository, documentPlanner, markdownGenerator, markdownValidator, objectMapper, null, null);
    }

    /**
     * Generates a complete Markdown documentation package for an approved project.
     *
     * @param projectId the project identifier
     * @param userEmail the authenticated user email
     * @return summary response containing generated documents
     */
    @Transactional
    public DocumentGenerationResponse generateDocumentation(Long projectId, String userEmail) {
        User user = resolveUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        // 1. Enforce lifecycle rule: Only APPROVED or GENERATED projects can generate documents
        if (!project.getStatus().canGenerateDocuments()) {
            throw new InvalidProjectStateException(
                    projectId,
                    project.getStatus(),
                    "Cannot generate documentation for project in status: " + project.getStatus() +
                            ". Documentation can only be generated from APPROVED projects."
            );
        }

        // 2. Validate approved specification presence
        if (project.getProjectSpecification() == null || project.getProjectSpecification().isBlank()) {
            throw new InvalidProjectStateException(
                    projectId,
                    project.getStatus(),
                    "Approved project specification is missing or empty."
            );
        }

        ProjectStatus originalStatus = project.getStatus();
        log.info("Starting documentation generation for project id: {} ('{}')", projectId, project.getName());

        // 3. Transition to GENERATING
        project.setStatus(ProjectStatus.GENERATING);
        projectRepository.save(project);

        try {
            // 4. Parse approved specification JSON (Single Source of Truth)
            JsonNode spec = objectMapper.readTree(project.getProjectSpecification());

            // 5. Plan applicable documents
            List<DocumentType> plannedTypes = documentPlanner.plan(project, spec);
            log.info("Planned {} documents for project id: {}", plannedTypes.size(), projectId);

            // 6. Generate Markdown documents deterministically
            List<GeneratedDocument> generatedDocuments = markdownGenerator.generate(project, spec, plannedTypes);

            // 7. Validate generated Markdown
            markdownValidator.validateAll(project, spec, generatedDocuments);

            // 8. Persist documents transactionally (clear existing on re-generation)
            if (documentVersionRepository != null) {
                documentVersionRepository.deleteAllByProjectId(projectId);
            }
            documentRepository.deleteAllByProjectId(projectId);

            List<Document> savedDocuments = new ArrayList<>();
            for (GeneratedDocument genDoc : generatedDocuments) {
                Document doc = new Document(project, genDoc.type(), genDoc.filename(), genDoc.content(), 1);
                Document savedDoc = documentRepository.save(doc);
                if (documentVersionRepository != null) {
                    DocumentVersion v1 = new DocumentVersion(
                            savedDoc,
                            1,
                            savedDoc.getContent(),
                            DocumentChangeType.GENERATE,
                            "Initial generation from approved specification",
                            project.getUser()
                    );
                    documentVersionRepository.save(v1);
                }
                savedDocuments.add(savedDoc);
            }

            // 9. Transition project to GENERATED
            project.setStatus(ProjectStatus.GENERATED);
            projectRepository.save(project);

            if (projectHistoryService != null) {
                try {
                    projectHistoryService.recordEvent(
                            project,
                            com.projectforge.entity.ProjectHistoryEventType.DOCUMENTS_GENERATED,
                            String.format("%d Markdown documentation files generated from approved specification", savedDocuments.size()),
                            null,
                            user.getEmail()
                    );
                } catch (Exception e) {
                    log.warn("Failed to record history event for document generation: {}", e.getMessage());
                }
            }

            log.info("Successfully generated and persisted {} documents for project id: {}", savedDocuments.size(), projectId);

            List<DocumentSummaryResponse> summaryList = savedDocuments.stream()
                    .map(DocumentSummaryResponse::from)
                    .toList();

            return new DocumentGenerationResponse(
                    projectId,
                    ProjectStatus.GENERATED,
                    savedDocuments.size(),
                    summaryList,
                    "Documentation successfully generated from the approved project specification."
            );

        } catch (Exception e) {
            log.error("Documentation generation failed for project id: {}. Rolling back state.", projectId, e);
            // Safe recovery path: restore project status back to APPROVED so it remains operable
            project.setStatus(originalStatus == ProjectStatus.GENERATED ? ProjectStatus.GENERATED : ProjectStatus.APPROVED);
            projectRepository.save(project);

            if (e instanceof com.projectforge.exception.ProjectForgeException pfe) {
                throw pfe;
            }
            throw new DocumentGenerationException(projectId, "Documentation generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all generated documents for an owned project.
     */
    @Transactional(readOnly = true)
    public List<DocumentSummaryResponse> getProjectDocuments(Long projectId, String userEmail) {
        User user = resolveUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        List<Document> documents = documentRepository.findAllByProjectIdOrderByFilenameAsc(project.getId());
        return documents.stream().map(DocumentSummaryResponse::from).toList();
    }

    /**
     * Retrieves a single generated document by ID, enforcing project ownership.
     */
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long documentId, String userEmail) {
        User user = resolveUser(userEmail);
        Document document = documentRepository.findByIdAndProjectUserId(documentId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        return DocumentResponse.from(document);
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
