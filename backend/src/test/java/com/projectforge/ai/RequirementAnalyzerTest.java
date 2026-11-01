package com.projectforge.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.dto.OpenQuestionDto;
import com.projectforge.entity.Project;
import com.projectforge.entity.User;
import com.projectforge.exception.AIServiceException;
import com.projectforge.exception.AIValidationException;
import com.projectforge.testutil.TestSpecificationFactory;
import com.projectforge.testutil.TestSpecificationFactory.StubAIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequirementAnalyzerTest {

    private StubAIService stubAiService;
    private PromptLoader promptLoader;
    private RequirementValidator validator;
    private QualityEvaluator qualityEvaluator;
    private ObjectMapper objectMapper;
    private RequirementAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        stubAiService = new StubAIService("{}");
        promptLoader = new PromptLoader();
        validator = new RequirementValidator();
        qualityEvaluator = new QualityEvaluator();
        objectMapper = new ObjectMapper();

        analyzer = new RequirementAnalyzer(stubAiService, promptLoader, validator, qualityEvaluator, objectMapper);
    }

    @Test
    @DisplayName("Analyzes project intake and produces structured specification with open questions")
    void analyzeSuccess() {
        User user = TestSpecificationFactory.createTestUser();
        Project project = TestSpecificationFactory.createTestProject(user);

        String validJson = TestSpecificationFactory.createValidSpecificationJson();
        stubAiService.setResponse(new AIResponse(validJson, 200, 450, "gpt-4o-mini"));

        RequirementAnalyzer.AnalysisResult result = analyzer.analyze(project);

        assertThat(result.specification()).isNotNull();
        assertThat(result.qualityScore()).isGreaterThanOrEqualTo(80);
        assertThat(result.openQuestions()).hasSize(1);
        OpenQuestionDto q = result.openQuestions().get(0);
        assertThat(q.id()).isEqualTo("Q1");
        assertThat(q.importance()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Throws AIServiceException when AI returns malformed JSON")
    void analyzeMalformedJsonThrows() {
        User user = TestSpecificationFactory.createTestUser();
        Project project = TestSpecificationFactory.createTestProject(user);

        stubAiService.setResponse(new AIResponse("not valid json at all", 100, 20, "gpt-4o-mini"));

        assertThatThrownBy(() -> analyzer.analyze(project))
                .isInstanceOf(AIServiceException.class)
                .hasMessageContaining("AI provider returned invalid JSON");
    }

    @Test
    @DisplayName("Throws AIValidationException when AI returns valid JSON but missing required sections")
    void analyzeMissingSectionsThrows() {
        User user = TestSpecificationFactory.createTestUser();
        Project project = TestSpecificationFactory.createTestProject(user);

        String incompleteJson = "{\"project\": {\"name\": \"Test\"}}";
        stubAiService.setResponse(new AIResponse(incompleteJson, 100, 20, "gpt-4o-mini"));

        assertThatThrownBy(() -> analyzer.analyze(project))
                .isInstanceOf(AIValidationException.class)
                .hasMessageContaining("validation failed");
    }
}
