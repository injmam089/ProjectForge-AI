package com.projectforge.markdown;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectType;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import com.projectforge.testutil.TestSpecificationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentPlannerTest {

    private DocumentPlanner planner;
    private ObjectMapper objectMapper;
    private Project project;
    private JsonNode validSpec;

    @BeforeEach
    void setUp() throws Exception {
        planner = new DocumentPlanner();
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
    }

    @Test
    @DisplayName("DocumentPlanner selects standard 14 documents for full-stack API application")
    void planStandardFullStackDocuments() {
        List<DocumentType> planned = planner.plan(project, validSpec);

        assertThat(planned).contains(
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
        assertThat(planned.size()).isEqualTo(14);
    }

    @Test
    @DisplayName("DocumentPlanner adds specialized documents for AI_ML_PROJECT")
    void planAiMlSpecializedDocuments() {
        project.setProjectType(ProjectType.AI_ML_PROJECT);
        List<DocumentType> planned = planner.plan(project, validSpec);

        assertThat(planned).contains(
                DocumentType.MODEL_REQUIREMENTS,
                DocumentType.DATASET,
                DocumentType.ML_PIPELINE
        );
        assertThat(planned.size()).isEqualTo(17);
    }

    @Test
    @DisplayName("DocumentPlanner adds specialized documents for IOT_PROJECT")
    void planIotSpecializedDocuments() {
        project.setProjectType(ProjectType.IOT_PROJECT);
        List<DocumentType> planned = planner.plan(project, validSpec);

        assertThat(planned).contains(
                DocumentType.HARDWARE_REQUIREMENTS,
                DocumentType.SENSOR_SPECIFICATION,
                DocumentType.IOT_PROTOCOLS
        );
    }

    @Test
    @DisplayName("DocumentPlanner adds specialized documents for CYBERSECURITY_PROJECT")
    void planCybersecuritySpecializedDocuments() {
        project.setProjectType(ProjectType.CYBERSECURITY_PROJECT);
        List<DocumentType> planned = planner.plan(project, validSpec);

        assertThat(planned).contains(
                DocumentType.THREAT_MODEL,
                DocumentType.SECURITY_TESTING
        );
    }

    @Test
    @DisplayName("DocumentPlanner adds specialized documents for MOBILE_APPLICATION")
    void planMobileSpecializedDocuments() {
        project.setProjectType(ProjectType.MOBILE_APPLICATION);
        List<DocumentType> planned = planner.plan(project, validSpec);

        assertThat(planned).contains(DocumentType.MOBILE_REQUIREMENTS);
    }

    @Test
    @DisplayName("DocumentPlanner rejects null inputs with IllegalArgumentException")
    void planRejectsNulls() {
        assertThatThrownBy(() -> planner.plan(null, validSpec))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> planner.plan(project, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
