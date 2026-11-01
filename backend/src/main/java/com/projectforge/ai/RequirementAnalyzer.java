package com.projectforge.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projectforge.dto.OpenQuestionDto;
import com.projectforge.entity.Project;
import com.projectforge.exception.AIServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates AI requirements analysis by preparing prompt contexts, invoking
 * the AI provider, validating the structured specification, and computing quality metrics.
 */
@Component
public class RequirementAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(RequirementAnalyzer.class);

    private final AIService aiService;
    private final PromptLoader promptLoader;
    private final RequirementValidator validator;
    private final QualityEvaluator qualityEvaluator;
    private final ObjectMapper objectMapper;

    public RequirementAnalyzer(
            AIService aiService,
            PromptLoader promptLoader,
            RequirementValidator validator,
            QualityEvaluator qualityEvaluator,
            ObjectMapper objectMapper
    ) {
        this.aiService = aiService;
        this.promptLoader = promptLoader;
        this.validator = validator;
        this.qualityEvaluator = qualityEvaluator;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes AI requirement analysis for the given project.
     *
     * @param project the project containing intake data
     * @return the generated and validated AnalysisResult
     */
    public AnalysisResult analyze(Project project) {
        log.info("Starting AI requirement analysis for project id: {} ('{}')", project.getId(), project.getName());

        String systemPrompt = promptLoader.loadPrompt("requirement-analyzer.txt");

        // Format user project intake as structured JSON clearly separated as untrusted input
        ObjectNode userPayload = objectMapper.createObjectNode();
        ObjectNode inputData = userPayload.putObject("untrusted_user_input");
        inputData.put("projectName", project.getName());
        inputData.put("description", project.getDescription() != null ? project.getDescription() : "");
        inputData.put("problemStatement", project.getProblemStatement() != null ? project.getProblemStatement() : "");
        inputData.put("targetUsers", project.getTargetUsers() != null ? project.getTargetUsers() : "");
        inputData.put("projectType", project.getProjectType() != null ? project.getProjectType().name() : "WEB_APPLICATION");
        inputData.put("difficulty", project.getDifficulty() != null ? project.getDifficulty().name() : "INTERMEDIATE");
        inputData.put("preferredTechStack", project.getPreferredTechStack() != null ? project.getPreferredTechStack() : "");
        inputData.put("constraints", project.getSpecialRequirements() != null ? project.getSpecialRequirements() : "");

        String userPrompt;
        try {
            userPrompt = objectMapper.writeValueAsString(userPayload);
        } catch (JsonProcessingException e) {
            throw new AIServiceException("Failed to serialize project intake into AI request payload", e);
        }

        AIRequest request = AIRequest.of(systemPrompt, userPrompt);
        AIResponse response = aiService.generate(request);

        JsonNode specNode;
        try {
            specNode = objectMapper.readTree(response.content());
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI output as JSON: {}", response.content());
            throw new AIServiceException("AI provider returned invalid JSON: " + e.getMessage(), e);
        }

        // Validate schema and business completeness
        validator.validate(specNode);

        // Deterministic quality scoring
        var qualityScoreDto = qualityEvaluator.evaluate(specNode);

        // Extract open questions
        List<OpenQuestionDto> openQuestions = extractOpenQuestions(specNode);

        log.info("AI requirement analysis completed successfully for project id: {}. Quality score: {}, Open questions: {}",
                project.getId(), qualityScoreDto.overallScore(), openQuestions.size());

        return new AnalysisResult(specNode, qualityScoreDto.overallScore(), openQuestions);
    }

    /**
     * Extracts open clarification questions from the specification JSON.
     */
    public List<OpenQuestionDto> extractOpenQuestions(JsonNode specNode) {
        List<OpenQuestionDto> questions = new ArrayList<>();
        JsonNode qArray = specNode.path("openQuestions");
        if (qArray.isArray()) {
            for (JsonNode q : qArray) {
                String id = q.path("id").asText("Q" + (questions.size() + 1));
                String question = q.path("question").asText("");
                String importance = q.path("importance").asText("MEDIUM");
                String reason = q.path("reason").asText("");
                if (!question.isBlank()) {
                    questions.add(new OpenQuestionDto(id, question, importance, reason));
                }
            }
        }
        return questions;
    }

    public record AnalysisResult(
            JsonNode specification,
            int qualityScore,
            List<OpenQuestionDto> openQuestions
    ) {}
}
