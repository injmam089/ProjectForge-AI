package com.projectforge.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projectforge.dto.ClarificationAnswerDto;
import com.projectforge.dto.ClarificationRequest;
import com.projectforge.entity.Project;
import com.projectforge.entity.User;
import com.projectforge.exception.AIServiceException;
import com.projectforge.exception.InvalidProjectStateException;
import com.projectforge.testutil.TestSpecificationFactory;
import com.projectforge.testutil.TestSpecificationFactory.StubAIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClarificationEngineTest {

    private StubAIService stubAiService;
    private PromptLoader promptLoader;
    private RequirementValidator validator;
    private QualityEvaluator qualityEvaluator;
    private RequirementAnalyzer requirementAnalyzer;
    private ObjectMapper objectMapper;
    private ClarificationEngine clarificationEngine;

    @BeforeEach
    void setUp() {
        stubAiService = new StubAIService("{}");
        promptLoader = new PromptLoader();
        validator = new RequirementValidator();
        qualityEvaluator = new QualityEvaluator();
        objectMapper = new ObjectMapper();
        requirementAnalyzer = new RequirementAnalyzer(stubAiService, promptLoader, validator, qualityEvaluator, objectMapper);

        clarificationEngine = new ClarificationEngine(
                stubAiService, promptLoader, validator, qualityEvaluator, requirementAnalyzer, objectMapper
        );
    }

    @Test
    @DisplayName("Clarification updates specification, resolves questions, and computes new quality score")
    void clarifySuccess() {
        User user = TestSpecificationFactory.createTestUser();
        Project project = TestSpecificationFactory.createTestProject(user);
        project.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJson());

        ClarificationRequest request = new ClarificationRequest(List.of(
                new ClarificationAnswerDto("Q1", "Yes, Zstandard compression must be mandatory for all network streams.")
        ));

        // AI returns updated spec with open questions resolved
        ObjectNode updatedNode = (ObjectNode) TestSpecificationFactory.createValidSpecificationNode();
        updatedNode.putArray("openQuestions"); // cleared questions

        stubAiService.setResponse(new AIResponse(updatedNode.toString(), 250, 480, "gpt-4o-mini"));

        RequirementAnalyzer.AnalysisResult result = clarificationEngine.clarify(project, request);

        assertThat(result.specification()).isNotNull();
        assertThat(result.openQuestions()).isEmpty();
        assertThat(result.qualityScore()).isGreaterThanOrEqualTo(85);
    }

    @Test
    @DisplayName("Throws InvalidProjectStateException when attempting to clarify project without existing specification")
    void clarifyWithoutSpecificationThrows() {
        User user = TestSpecificationFactory.createTestUser();
        Project project = TestSpecificationFactory.createTestProject(user);
        project.setProjectSpecification(null);

        ClarificationRequest request = new ClarificationRequest(List.of(
                new ClarificationAnswerDto("Q1", "Answer")
        ));

        assertThatThrownBy(() -> clarificationEngine.clarify(project, request))
                .isInstanceOf(InvalidProjectStateException.class)
                .hasMessageContaining("No structured specification exists");
    }

    @Test
    @DisplayName("Throws AIServiceException when AI returns malformed JSON during clarification")
    void clarifyMalformedJsonThrows() {
        User user = TestSpecificationFactory.createTestUser();
        Project project = TestSpecificationFactory.createTestProject(user);
        project.setProjectSpecification(TestSpecificationFactory.createValidSpecificationJson());

        ClarificationRequest request = new ClarificationRequest(List.of(
                new ClarificationAnswerDto("Q1", "Answer")
        ));

        stubAiService.setResponse(new AIResponse("Broken JSON {", 100, 20, "gpt-4o-mini"));

        assertThatThrownBy(() -> clarificationEngine.clarify(project, request))
                .isInstanceOf(AIServiceException.class)
                .hasMessageContaining("invalid JSON during clarification");
    }
}
