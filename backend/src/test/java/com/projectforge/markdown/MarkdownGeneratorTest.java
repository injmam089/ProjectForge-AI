package com.projectforge.markdown;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectType;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.markdown.generator.AiBuildInstructionsGenerator;
import com.projectforge.markdown.generator.ApiSpecificationGenerator;
import com.projectforge.markdown.generator.ArchitectureGenerator;
import com.projectforge.markdown.generator.DatabaseGenerator;
import com.projectforge.markdown.generator.DeploymentGenerator;
import com.projectforge.markdown.generator.DevelopmentPlanGenerator;
import com.projectforge.markdown.generator.FeaturesGenerator;
import com.projectforge.markdown.generator.ProjectGoalsGenerator;
import com.projectforge.markdown.generator.ProjectRequirementsGenerator;
import com.projectforge.markdown.generator.ReadmeGenerator;
import com.projectforge.markdown.generator.SecurityGenerator;
import com.projectforge.markdown.generator.SpecializedDocumentGenerators;
import com.projectforge.markdown.generator.TechStackGenerator;
import com.projectforge.markdown.generator.TestingGenerator;
import com.projectforge.markdown.generator.UiUxRequirementsGenerator;
import com.projectforge.testutil.TestSpecificationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownGeneratorTest {

    private MarkdownGenerator generator;
    private ObjectMapper objectMapper;
    private Project project;
    private JsonNode validSpec;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
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

        List<DocumentContentGenerator> generators = List.of(
                new ReadmeGenerator(),
                new ProjectRequirementsGenerator(),
                new ProjectGoalsGenerator(),
                new FeaturesGenerator(),
                new TechStackGenerator(),
                new ArchitectureGenerator(),
                new DatabaseGenerator(),
                new ApiSpecificationGenerator(),
                new SecurityGenerator(),
                new UiUxRequirementsGenerator(),
                new DevelopmentPlanGenerator(),
                new TestingGenerator(),
                new DeploymentGenerator(),
                new AiBuildInstructionsGenerator(),
                new SpecializedDocumentGenerators.ModelRequirementsGenerator(),
                new SpecializedDocumentGenerators.DatasetGenerator(),
                new SpecializedDocumentGenerators.MlPipelineGenerator(),
                new SpecializedDocumentGenerators.HardwareRequirementsGenerator(),
                new SpecializedDocumentGenerators.SensorSpecificationGenerator(),
                new SpecializedDocumentGenerators.IotProtocolsGenerator(),
                new SpecializedDocumentGenerators.ThreatModelGenerator(),
                new SpecializedDocumentGenerators.SecurityTestingGenerator(),
                new SpecializedDocumentGenerators.MobileRequirementsGenerator()
        );

        generator = new MarkdownGenerator(generators);
    }

    @Test
    @DisplayName("MarkdownGenerator generates consistent Markdown files matching planned types")
    void generateAllPlannedDocuments() {
        List<DocumentType> planned = List.of(
                DocumentType.README,
                DocumentType.PROJECT_REQUIREMENTS,
                DocumentType.PROJECT_GOALS,
                DocumentType.FEATURES,
                DocumentType.TECH_STACK,
                DocumentType.ARCHITECTURE,
                DocumentType.DATABASE,
                DocumentType.API_SPECIFICATION,
                DocumentType.SECURITY,
                DocumentType.UI_UX_REQUIREMENTS,
                DocumentType.DEVELOPMENT_PLAN,
                DocumentType.TESTING,
                DocumentType.DEPLOYMENT,
                DocumentType.AI_BUILD_INSTRUCTIONS
        );

        List<GeneratedDocument> docs = generator.generate(project, validSpec, planned);

        assertThat(docs).hasSize(14);

        // Check README
        GeneratedDocument readme = docs.stream().filter(d -> d.type() == DocumentType.README).findFirst().orElseThrow();
        assertThat(readme.filename()).isEqualTo("README.md");
        assertThat(readme.content()).contains("CloudPulse Telemetry");
        assertThat(readme.content()).contains("## Overview");
        assertThat(readme.content()).contains("## Technology Stack");

        // Check DATABASE.md
        GeneratedDocument database = docs.stream().filter(d -> d.type() == DocumentType.DATABASE).findFirst().orElseThrow();
        assertThat(database.filename()).isEqualTo("DATABASE.md");
        assertThat(database.content()).contains("telemetry_metrics");
        assertThat(database.content()).contains("PostgreSQL");

        // Check API_SPECIFICATION.md
        GeneratedDocument apis = docs.stream().filter(d -> d.type() == DocumentType.API_SPECIFICATION).findFirst().orElseThrow();
        assertThat(apis.filename()).isEqualTo("API_SPECIFICATION.md");
        assertThat(apis.content()).contains("/api/metrics");
        assertThat(apis.content()).contains("POST");

        // Check TECH_STACK.md
        GeneratedDocument tech = docs.stream().filter(d -> d.type() == DocumentType.TECH_STACK).findFirst().orElseThrow();
        assertThat(tech.filename()).isEqualTo("TECH_STACK.md");
        assertThat(tech.content()).contains("Spring Boot");
        assertThat(tech.content()).contains("React");

        // Check AI_BUILD_INSTRUCTIONS.md
        GeneratedDocument aiBuild = docs.stream().filter(d -> d.type() == DocumentType.AI_BUILD_INSTRUCTIONS).findFirst().orElseThrow();
        assertThat(aiBuild.filename()).isEqualTo("AI_BUILD_INSTRUCTIONS.md");
        assertThat(aiBuild.content()).contains("Mandatory Architectural Rules");
        assertThat(aiBuild.content()).contains("Single Source of Truth");
    }
}
