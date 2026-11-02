package com.projectforge.export;

import com.projectforge.entity.Document;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectType;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.exception.InvalidExportException;
import com.projectforge.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportValidatorTest {

    private ExportValidator validator;
    private User owner;
    private Project project;

    @BeforeEach
    void setUp() {
        validator = new ExportValidator();
        owner = new User("Owner User", "owner@example.com", "hash", Role.USER);
        owner.setId(1L);

        project = new Project(
                owner,
                "CloudPulse Telemetry",
                "Telemetry streaming platform",
                "High throughput streaming logs",
                "DevOps Engineers",
                ProjectType.API_BACKEND,
                ProjectDifficulty.ADVANCED,
                "Java 17, Kafka",
                "Low latency"
        );
        project.setId(10L);
    }

    @Test
    @DisplayName("Validates owner successfully")
    void ownerPasses() {
        assertThatCode(() -> validator.validateProject(project, "owner@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rejects cross-user access with 404 ResourceNotFoundException (Anti-IDOR)")
    void nonOwnerThrows404() {
        assertThatThrownBy(() -> validator.validateProject(project, "attacker@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Rejects empty documents list with InvalidExportException")
    void emptyDocumentsThrows() {
        assertThatThrownBy(() -> validator.validateDocuments(Collections.emptyList(), project.getId()))
                .isInstanceOf(InvalidExportException.class)
                .hasMessageContaining("no documents available for export");
    }

    @Test
    @DisplayName("Rejects documents with path traversal in filenames")
    void pathTraversalFilenameThrows() {
        Document evilDoc = new Document(project, DocumentType.README, "../evil.md", "# Malicious content", 1);
        List<Document> docs = List.of(evilDoc);

        assertThatThrownBy(() -> validator.validateDocuments(docs, project.getId()))
                .isInstanceOf(InvalidExportException.class)
                .hasMessageContaining("illegal path traversal");
    }

    @Test
    @DisplayName("Rejects documents with invalid characters in filenames")
    void invalidFilenameThrows() {
        Document badDoc = new Document(project, DocumentType.README, "file with spaces.md", "# Valid content", 1);
        List<Document> docs = List.of(badDoc);

        assertThatThrownBy(() -> validator.validateDocuments(docs, project.getId()))
                .isInstanceOf(InvalidExportException.class)
                .hasMessageContaining("must match pattern");
    }

    @Test
    @DisplayName("Rejects duplicate filenames case-insensitively")
    void duplicateFilenamesThrow() {
        Document doc1 = new Document(project, DocumentType.README, "README.md", "# First content", 1);
        Document doc2 = new Document(project, DocumentType.README, "readme.md", "# Duplicate content", 2);
        List<Document> docs = List.of(doc1, doc2);

        assertThatThrownBy(() -> validator.validateDocuments(docs, project.getId()))
                .isInstanceOf(InvalidExportException.class)
                .hasMessageContaining("Duplicate document filename");
    }

    @Test
    @DisplayName("Rejects document exceeding single document limit")
    void oversizedDocumentThrows() {
        String largeContent = "a".repeat(ExportConstants.MAX_SINGLE_DOCUMENT_SIZE + 10);
        Document hugeDoc = new Document(project, DocumentType.README, "README.md", largeContent, 1);
        List<Document> docs = List.of(hugeDoc);

        assertThatThrownBy(() -> validator.validateDocuments(docs, project.getId()))
                .isInstanceOf(InvalidExportException.class)
                .hasMessageContaining("exceeds maximum single document limit");
    }

    @Test
    @DisplayName("Rejects package exceeding document count limit")
    void excessiveDocumentCountThrows() {
        List<Document> docs = new ArrayList<>();
        for (int i = 0; i <= ExportConstants.MAX_DOCUMENT_COUNT; i++) {
            docs.add(new Document(project, DocumentType.README, "DOC_" + i + ".md", "# Content " + i, 1));
        }

        assertThatThrownBy(() -> validator.validateDocuments(docs, project.getId()))
                .isInstanceOf(InvalidExportException.class)
                .hasMessageContaining("maximum document count limit");
    }

    @Test
    @DisplayName("Valid documents pass all validation checks")
    void validDocumentsPass() {
        Document doc1 = new Document(project, DocumentType.README, "README.md", "# CloudPulse Readme", 1);
        Document doc2 = new Document(project, DocumentType.ARCHITECTURE, "ARCHITECTURE.md", "# Architecture Guide", 1);
        List<Document> docs = List.of(doc1, doc2);

        assertThatCode(() -> validator.validateDocuments(docs, project.getId()))
                .doesNotThrowAnyException();
    }
}
