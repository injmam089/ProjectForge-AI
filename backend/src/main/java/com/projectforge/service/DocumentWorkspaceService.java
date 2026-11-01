package com.projectforge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.dto.DocumentEditRequest;
import com.projectforge.dto.DocumentResponse;
import com.projectforge.dto.DocumentSaveResponse;
import com.projectforge.dto.DocumentSummaryResponse;
import com.projectforge.dto.DocumentVersionResponse;
import com.projectforge.dto.DocumentVersionSummaryResponse;
import com.projectforge.dto.WorkspaceResponse;
import com.projectforge.entity.Document;
import com.projectforge.entity.DocumentChangeType;
import com.projectforge.entity.DocumentVersion;
import com.projectforge.entity.Project;
import com.projectforge.entity.User;
import com.projectforge.exception.DocumentGenerationException;
import com.projectforge.exception.DocumentNotFoundException;
import com.projectforge.exception.DocumentVersionNotFoundException;
import com.projectforge.exception.InvalidProjectStateException;
import com.projectforge.exception.ProjectNotFoundException;
import com.projectforge.exception.ResourceNotFoundException;
import com.projectforge.markdown.DocumentContentValidator;
import com.projectforge.markdown.GeneratedDocument;
import com.projectforge.markdown.MarkdownGenerator;
import com.projectforge.repository.DocumentRepository;
import com.projectforge.repository.DocumentVersionRepository;
import com.projectforge.repository.ProjectRepository;
import com.projectforge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing Markdown document editing, versioning, restore, and canonical regeneration.
 * Strictly guarantees that document editing NEVER modifies the approved Project Specification.
 */
@Service
public class DocumentWorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(DocumentWorkspaceService.class);

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final MarkdownGenerator markdownGenerator;
    private final DocumentContentValidator documentContentValidator;
    private final ObjectMapper objectMapper;

    public DocumentWorkspaceService(
            DocumentRepository documentRepository,
            DocumentVersionRepository documentVersionRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            MarkdownGenerator markdownGenerator,
            DocumentContentValidator documentContentValidator,
            ObjectMapper objectMapper
    ) {
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.markdownGenerator = markdownGenerator;
        this.documentContentValidator = documentContentValidator;
        this.objectMapper = objectMapper;
    }

    /**
     * Retrieves workspace overview for an owned project.
     * Enforces anti-IDOR user ownership.
     */
    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspace(Long projectId, String userEmail) {
        User user = resolveUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        List<DocumentSummaryResponse> documentSummaries = documentRepository
                .findAllByProjectIdOrderByFilenameAsc(projectId)
                .stream()
                .map(DocumentSummaryResponse::from)
                .toList();

        return new WorkspaceResponse(
                project.getId(),
                project.getName(),
                project.getStatus(),
                documentSummaries
        );
    }

    /**
     * Manually updates and saves a Markdown document.
     * Validates content, locks the document, creates a new DocumentVersion, and increments version.
     * The approved Project Specification is never modified.
     */
    @Transactional
    public DocumentSaveResponse saveDocument(Long documentId, DocumentEditRequest request, String userEmail) {
        User user = resolveUser(userEmail);
        Document document = findOwnedDocumentWithLock(documentId, user.getId());

        String initialSpec = document.getProject().getProjectSpecification();

        ensureInitialVersionExists(document, user);

        // 1. Validate content
        documentContentValidator.validateContent(document.getFilename(), request.content());

        // 2. Determine monotonic next version
        int nextVersion = document.getVersion() + 1;

        // 3. Create historical version record
        DocumentVersion versionRecord = new DocumentVersion(
                document,
                nextVersion,
                request.content(),
                DocumentChangeType.MANUAL_EDIT,
                "Manual edit",
                user
        );
        documentVersionRepository.save(versionRecord);

        // 4. Update current document
        document.setContent(request.content());
        document.setVersion(nextVersion);
        Document savedDoc = documentRepository.save(document);

        // 5. Invariant check: Specification must be 100% immutable
        assertSpecificationUntouched(document.getProject(), initialSpec);

        log.info("Document id: {} ('{}') manually updated to version {} by user: {}",
                savedDoc.getId(), savedDoc.getFilename(), savedDoc.getVersion(), user.getId());

        return new DocumentSaveResponse(
                savedDoc.getId(),
                savedDoc.getFilename(),
                savedDoc.getVersion(),
                savedDoc.getContent().length(),
                "Document saved successfully"
        );
    }

    /**
     * Retrieves version history metadata for an owned document.
     * Does not return full content to optimize payload size.
     */
    @Transactional
    public List<DocumentVersionSummaryResponse> getVersions(Long documentId, String userEmail) {
        User user = resolveUser(userEmail);
        Document document = findOwnedDocument(documentId, user.getId());

        ensureInitialVersionExists(document, user);

        return documentVersionRepository.findAllByDocumentIdOrderByVersionDesc(documentId)
                .stream()
                .map(DocumentVersionSummaryResponse::from)
                .toList();
    }

    /**
     * Retrieves a single historical document version including full content.
     */
    @Transactional(readOnly = true)
    public DocumentVersionResponse getVersion(Long documentId, int version, String userEmail) {
        User user = resolveUser(userEmail);
        findOwnedDocument(documentId, user.getId()); // ownership check

        DocumentVersion docVersion = documentVersionRepository.findByDocumentIdAndVersion(documentId, version)
                .orElseThrow(() -> new DocumentVersionNotFoundException(documentId, version));

        return DocumentVersionResponse.from(docVersion);
    }

    /**
     * Restores a document to a previous historical version.
     * Appends a new version (RESTORE) containing the historical content without deleting history.
     */
    @Transactional
    public DocumentResponse restoreVersion(Long documentId, int targetVersion, String userEmail) {
        User user = resolveUser(userEmail);
        Document document = findOwnedDocumentWithLock(documentId, user.getId());

        String initialSpec = document.getProject().getProjectSpecification();

        ensureInitialVersionExists(document, user);

        DocumentVersion historicalVersion = documentVersionRepository.findByDocumentIdAndVersion(documentId, targetVersion)
                .orElseThrow(() -> new DocumentVersionNotFoundException(documentId, targetVersion));

        String restoredContent = historicalVersion.getContent();
        documentContentValidator.validateContent(document.getFilename(), restoredContent);

        int nextVersion = document.getVersion() + 1;

        // Create new restore version record
        DocumentVersion restoreRecord = new DocumentVersion(
                document,
                nextVersion,
                restoredContent,
                DocumentChangeType.RESTORE,
                "Restored from version " + targetVersion,
                user
        );
        documentVersionRepository.save(restoreRecord);

        // Update current document
        document.setContent(restoredContent);
        document.setVersion(nextVersion);
        Document savedDoc = documentRepository.save(document);

        assertSpecificationUntouched(document.getProject(), initialSpec);

        log.info("Document id: {} ('{}') restored from version {} to new version {} by user: {}",
                savedDoc.getId(), savedDoc.getFilename(), targetVersion, savedDoc.getVersion(), user.getId());

        return DocumentResponse.from(savedDoc);
    }

    /**
     * Regenerates an individual Markdown document deterministically from the approved project specification.
     * Preserves version history and appends a new REGENERATE version.
     * Never modifies project specification, other documents, or other projects.
     */
    @Transactional
    public DocumentResponse regenerateDocument(Long documentId, String userEmail) {
        User user = resolveUser(userEmail);
        Document document = findOwnedDocumentWithLock(documentId, user.getId());
        Project project = document.getProject();

        String initialSpec = project.getProjectSpecification();

        if (initialSpec == null || initialSpec.isBlank()) {
            throw new InvalidProjectStateException(
                    project.getId(),
                    project.getStatus(),
                    "Cannot regenerate document: Project has no approved specification."
            );
        }

        ensureInitialVersionExists(document, user);

        try {
            JsonNode specNode = objectMapper.readTree(initialSpec);

            // Re-run canonical Phase 6 generator for this document type
            GeneratedDocument canonicalDoc = markdownGenerator.generateSingle(project, specNode, document.getType());
            documentContentValidator.validateContent(document.getFilename(), canonicalDoc.content());

            int nextVersion = document.getVersion() + 1;

            DocumentVersion regenRecord = new DocumentVersion(
                    document,
                    nextVersion,
                    canonicalDoc.content(),
                    DocumentChangeType.REGENERATE,
                    "Regenerated from approved specification",
                    user
            );
            documentVersionRepository.save(regenRecord);

            document.setContent(canonicalDoc.content());
            document.setVersion(nextVersion);
            Document savedDoc = documentRepository.save(document);

            assertSpecificationUntouched(project, initialSpec);

            log.info("Document id: {} ('{}') regenerated from specification to version {} by user: {}",
                    savedDoc.getId(), savedDoc.getFilename(), savedDoc.getVersion(), user.getId());

            return DocumentResponse.from(savedDoc);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse approved specification for project id: {}", project.getId(), e);
            throw new DocumentGenerationException(project.getId(), "Failed to parse approved specification: " + e.getMessage(), e);
        }
    }

    /**
     * Bootstraps version 1 if an existing Phase 6 document lacks a version record.
     */
    private void ensureInitialVersionExists(Document document, User user) {
        if (documentVersionRepository.findByDocumentIdAndVersion(document.getId(), 1).isEmpty()) {
            DocumentVersion v1 = new DocumentVersion(
                    document,
                    1,
                    document.getContent(),
                    DocumentChangeType.GENERATE,
                    "Initial generation from approved specification",
                    user
            );
            documentVersionRepository.save(v1);
        }
    }

    private Document findOwnedDocument(Long documentId, Long userId) {
        return documentRepository.findByIdAndProjectUserId(documentId, userId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    private Document findOwnedDocumentWithLock(Long documentId, Long userId) {
        Document document = documentRepository.findByIdWithLock(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (!document.getProject().getUser().getId().equals(userId)) {
            // Anti-IDOR: Return 404 without leaking metadata
            throw new DocumentNotFoundException(documentId);
        }
        return document;
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private void assertSpecificationUntouched(Project project, String expectedSpec) {
        String currentSpec = project.getProjectSpecification();
        if (expectedSpec == null && currentSpec != null) {
            throw new IllegalStateException("Approved specification was illegally modified during document operation!");
        }
        if (expectedSpec != null && !expectedSpec.equals(currentSpec)) {
            throw new IllegalStateException("Approved specification was illegally modified during document operation!");
        }
    }
}
