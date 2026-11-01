package com.projectforge.markdown;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectType;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.exception.MarkdownValidationException;
import com.projectforge.testutil.TestSpecificationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkdownValidatorTest {

    private MarkdownValidator validator;
    private Project project;
    private JsonNode validSpec;

    @BeforeEach
    void setUp() throws Exception {
        validator = new MarkdownValidator();
        ObjectMapper objectMapper = new ObjectMapper();
        User user = new User("Owner User", "owner@example.com", "hash", Role.USER);
        user.setId(1L);

        project = new Project(
                user,
                "CloudPulse Telemetry",
                "Observability collector",
                "Latency spikes",
                "DevOps Engineers",
                ProjectType.API_BACKEND,
                ProjectDifficulty.ADVANCED,
                "Spring Boot, React, PostgreSQL",
                null
        );
        project.setId(10L);

        validSpec = objectMapper.readTree(TestSpecificationFactory.createValidSpecificationJson());
    }

    @Test
    @DisplayName("Valid documents pass validation without throwing")
    void validDocumentsPass() {
        List<GeneratedDocument> docs = List.of(
                new GeneratedDocument(DocumentType.README, "README.md", "# CloudPulse Telemetry\n\nComprehensive overview of the system."),
                new GeneratedDocument(DocumentType.DATABASE, "DATABASE.md", "# Database Schema\n\nUses approved PostgreSQL database engine.")
        );

        assertThatCode(() -> validator.validateAll(project, validSpec, docs))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Validation fails when filename contains path traversal or invalid extension")
    void invalidFilenameThrows() {
        List<GeneratedDocument> traversal = List.of(
                new GeneratedDocument(DocumentType.README, "../README.md", "# CloudPulse Telemetry overview content here.")
        );
        assertThatThrownBy(() -> validator.validateAll(project, validSpec, traversal))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining("path traversal");

        List<GeneratedDocument> invalidExt = List.of(
                new GeneratedDocument(DocumentType.README, "README.txt", "# CloudPulse Telemetry overview content here.")
        );
        assertThatThrownBy(() -> validator.validateAll(project, validSpec, invalidExt))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining(".md");
    }

    @Test
    @DisplayName("Validation fails when duplicate filenames exist in generated set")
    void duplicateFilenameThrows() {
        List<GeneratedDocument> duplicates = List.of(
                new GeneratedDocument(DocumentType.README, "README.md", "# CloudPulse Telemetry first content overview."),
                new GeneratedDocument(DocumentType.PROJECT_REQUIREMENTS, "README.md", "# CloudPulse Telemetry duplicate content.")
        );

        assertThatThrownBy(() -> validator.validateAll(project, validSpec, duplicates))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining("Duplicate document filename");
    }

    @Test
    @DisplayName("Validation fails when document content is blank or too short")
    void invalidContentThrows() {
        List<GeneratedDocument> blank = List.of(
                new GeneratedDocument(DocumentType.README, "README.md", "   ")
        );
        assertThatThrownBy(() -> validator.validateAll(project, validSpec, blank))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining("blank");

        List<GeneratedDocument> shortContent = List.of(
                new GeneratedDocument(DocumentType.README, "README.md", "Too short")
        );
        assertThatThrownBy(() -> validator.validateAll(project, validSpec, shortContent))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining("too short");
    }

    @Test
    @DisplayName("Consistency check fails when document claims contradicting database technology")
    void databaseContradictionThrows() {
        // Approved spec uses PostgreSQL, but document claims MongoDB
        List<GeneratedDocument> contradicting = List.of(
                new GeneratedDocument(DocumentType.README, "README.md", "# CloudPulse Telemetry\n\nValid overview of the project."),
                new GeneratedDocument(DocumentType.DATABASE, "DATABASE.md", "# Database Schema\n\nThis application relies on MongoDB NoSQL collections.")
        );

        assertThatThrownBy(() -> validator.validateAll(project, validSpec, contradicting))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining("claims MongoDB when approved database is PostgreSQL");
    }

    @Test
    @DisplayName("Consistency check fails when README does not contain approved project name")
    void missingProjectNameThrows() {
        List<GeneratedDocument> missingName = List.of(
                new GeneratedDocument(DocumentType.README, "README.md", "# Unrelated Generic Name\n\nOverview of something else.")
        );

        assertThatThrownBy(() -> validator.validateAll(project, validSpec, missingName))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining("does not contain the approved project name");
    }
}
