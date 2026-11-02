package com.projectforge.export;

import com.projectforge.dto.ExportResponse;
import com.projectforge.entity.Document;
import com.projectforge.entity.Project;
import com.projectforge.exception.ResourceNotFoundException;
import com.projectforge.repository.DocumentRepository;
import com.projectforge.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service orchestrating the secure, read-only ZIP export of finalized project documentation.
 */
@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final ExportValidator exportValidator;
    private final ZipPackageBuilder zipPackageBuilder;

    public ExportService(
            ProjectRepository projectRepository,
            DocumentRepository documentRepository,
            ExportValidator exportValidator,
            ZipPackageBuilder zipPackageBuilder
    ) {
        this.projectRepository = projectRepository;
        this.documentRepository = documentRepository;
        this.exportValidator = exportValidator;
        this.zipPackageBuilder = zipPackageBuilder;
    }

    /**
     * Prepares export metadata without mutating project state.
     */
    @Transactional(readOnly = true)
    public ExportResponse prepareExport(Long projectId, String userEmail) {
        Project project = getProjectAndValidate(projectId, userEmail);
        List<Document> documents = documentRepository.findAllByProjectIdOrderByFilenameAsc(projectId);
        exportValidator.validateDocuments(documents, projectId);

        String filename = SafeArchiveName.getZipFileName(project.getName());
        return new ExportResponse(
                project.getId(),
                project.getName(),
                documents.size(),
                filename,
                Instant.now()
        );
    }

    /**
     * Generates and validates the binary ZIP package containing current active documents.
     * Strictly read-only operation.
     */
    @Transactional(readOnly = true)
    public ExportPackage generateExportPackage(Long projectId, String userEmail) {
        Project project = getProjectAndValidate(projectId, userEmail);
        String initialSpec = project.getProjectSpecification();

        List<Document> documents = documentRepository.findAllByProjectIdOrderByFilenameAsc(projectId);
        exportValidator.validateDocuments(documents, projectId);

        byte[] zipBytes = zipPackageBuilder.buildAndValidatePackage(project, documents);
        String filename = SafeArchiveName.getZipFileName(project.getName());

        // Invariant assertion: Verify specification was completely untouched
        if (initialSpec != null && !initialSpec.equals(project.getProjectSpecification())) {
            throw new IllegalStateException("CRITICAL INVARIANT VIOLATION: Project specification was modified during export!");
        }

        log.info("Project id: {} ('{}') successfully exported as ZIP '{}' ({} documents, {} bytes)",
                projectId, project.getName(), filename, documents.size(), zipBytes.length);

        return new ExportPackage(filename, zipBytes);
    }

    private Project getProjectAndValidate(Long projectId, String userEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        exportValidator.validateProject(project, userEmail);
        return project;
    }

    public record ExportPackage(String filename, byte[] content) {
    }
}
