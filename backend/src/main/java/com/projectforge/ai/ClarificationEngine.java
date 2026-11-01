package com.projectforge.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projectforge.dto.ClarificationAnswerDto;
import com.projectforge.dto.ClarificationRequest;
import com.projectforge.dto.OpenQuestionDto;
import com.projectforge.entity.Project;
import com.projectforge.exception.AIServiceException;
import com.projectforge.exception.InvalidProjectStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Engine that accepts user answers to open clarification questions and orchestrates
 * updating the structured project specification through AI refinement.
 */
@Component
public class ClarificationEngine {

    private static final Logger log = LoggerFactory.getLogger(ClarificationEngine.class);

    private final AIService aiService;
    private final PromptLoader promptLoader;
    private final RequirementValidator validator;
    private final QualityEvaluator qualityEvaluator;
    private final RequirementAnalyzer requirementAnalyzer;
    private final ObjectMapper objectMapper;

    public ClarificationEngine(
            AIService aiService,
            PromptLoader promptLoader,
            RequirementValidator validator,
            QualityEvaluator qualityEvaluator,
            RequirementAnalyzer requirementAnalyzer,
            ObjectMapper objectMapper
    ) {
        this.aiService = aiService;
        this.promptLoader = promptLoader;
        this.validator = validator;
        this.qualityEvaluator = qualityEvaluator;
        this.requirementAnalyzer = requirementAnalyzer;
        this.objectMapper = objectMapper;
    }

    /**
     * Updates an existing project specification by incorporating answers to clarification questions.
     *
     * @param project the project containing existing specification
     * @param request the clarification answers submitted by the user
     * @return the updated and validated AnalysisResult
     */
    public RequirementAnalyzer.AnalysisResult clarify(Project project, ClarificationRequest request) {
        if (project.getProjectSpecification() == null || project.getProjectSpecification().isBlank()) {
            throw new InvalidProjectStateException(
                    "Cannot clarify project: No structured specification exists yet. Run analysis first."
            );
        }

        log.info("Refining requirements with {} clarification answers for project id: {}",
                request.answers().size(), project.getId());

        String systemPrompt = promptLoader.loadPrompt("clarification-engine.txt");

        JsonNode currentSpecNode;
        try {
            currentSpecNode = objectMapper.readTree(project.getProjectSpecification());
        } catch (JsonProcessingException e) {
            throw new AIServiceException("Corrupted existing specification in database: " + e.getMessage(), e);
        }

        // Build user prompt
        ObjectNode userPayload = objectMapper.createObjectNode();
        ObjectNode inputData = userPayload.putObject("untrusted_user_input");
        inputData.set("currentSpecification", currentSpecNode);

        ArrayNode answersArray = inputData.putArray("clarificationAnswers");
        for (ClarificationAnswerDto ans : request.answers()) {
            ObjectNode ansNode = answersArray.addObject();
            ansNode.put("questionId", ans.questionId());
            ansNode.put("answer", ans.answer());
        }

        String userPrompt;
        try {
            userPrompt = objectMapper.writeValueAsString(userPayload);
        } catch (JsonProcessingException e) {
            throw new AIServiceException("Failed to serialize clarification payload", e);
        }

        AIRequest aiRequest = AIRequest.of(systemPrompt, userPrompt);
        AIResponse response = aiService.generate(aiRequest);

        JsonNode updatedSpecNode;
        try {
            updatedSpecNode = objectMapper.readTree(response.content());
        } catch (JsonProcessingException e) {
            log.error("Failed to parse updated AI output as JSON: {}", response.content());
            throw new AIServiceException("AI provider returned invalid JSON during clarification: " + e.getMessage(), e);
        }

        // Validate updated structure
        validator.validate(updatedSpecNode);

        // Re-evaluate quality score
        var qualityScoreDto = qualityEvaluator.evaluate(updatedSpecNode);

        // Extract remaining open questions
        List<OpenQuestionDto> remainingQuestions = requirementAnalyzer.extractOpenQuestions(updatedSpecNode);

        log.info("Clarification completed for project id: {}. New quality score: {}, Remaining questions: {}",
                project.getId(), qualityScoreDto.overallScore(), remainingQuestions.size());

        return new RequirementAnalyzer.AnalysisResult(updatedSpecNode, qualityScoreDto.overallScore(), remainingQuestions);
    }
}
