package com.projectforge.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projectforge.exception.AIServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * Production-ready OpenAI API client implementing the AIService interface.
 * Connects securely to the OpenAI Chat Completions API using standard Java HTTP Client.
 *
 * Security:
 * - API keys are loaded strictly from server-side environment/configuration
 * - Never logs or exposes API keys or secrets
 * - Enforces connect and request timeouts
 * - Safely handles provider errors and rate limits
 */
@Service
public class OpenAIService implements AIService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final int timeoutSeconds;

    @Autowired
    public OpenAIService(
            @Value("${app.ai.openai.api-key:}") String apiKey,
            @Value("${app.ai.openai.model:gpt-4o-mini}") String model,
            @Value("${app.ai.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.ai.openai.timeout-seconds:60}") int timeoutSeconds,
            ObjectMapper objectMapper
    ) {
        this(apiKey, model, baseUrl, timeoutSeconds, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build());
    }

    public OpenAIService(
            String apiKey,
            String model,
            String baseUrl,
            int timeoutSeconds,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = (model != null && !model.isBlank()) ? model.trim() : "gpt-4o-mini";
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.trim().replaceAll("/+$", "") : "https://api.openai.com/v1";
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 60;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public AIResponse generate(AIRequest request) {
        if (apiKey.isBlank() || apiKey.equalsIgnoreCase("your_openai_api_key_placeholder")) {
            throw new AIServiceException("OpenAI API key is not configured. Please set the OPENAI_API_KEY environment variable or app.ai.openai.api-key configuration property.");
        }

        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", model);
            payload.put("temperature", request.temperature());

            // Enforce structured JSON object response format where supported
            ObjectNode responseFormat = payload.putObject("response_format");
            responseFormat.put("type", "json_object");

            ArrayNode messages = payload.putArray("messages");

            // System prompt
            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", request.systemPrompt());

            // User prompt
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", request.userPrompt());

            String requestBody = objectMapper.writeValueAsString(payload);
            URI endpoint = URI.create(baseUrl + "/chat/completions");

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(endpoint)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            log.info("Sending AI request to model: {} at endpoint: {}", model, endpoint);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode == 200) {
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode choices = root.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    String rawContent = choices.get(0).path("message").path("content").asText();
                    String cleanContent = cleanJsonMarkdown(rawContent);

                    int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
                    int completionTokens = root.path("usage").path("completion_tokens").asInt(0);

                    log.info("AI response received successfully. Tokens used - prompt: {}, completion: {}",
                            promptTokens, completionTokens);

                    return new AIResponse(cleanContent, promptTokens, completionTokens, model);
                } else {
                    throw new AIServiceException("OpenAI returned an empty choice list.");
                }
            } else if (statusCode == 401 || statusCode == 403) {
                log.error("OpenAI authentication failure (HTTP {})", statusCode);
                throw new AIServiceException("Authentication failed with OpenAI API: invalid API key or insufficient permissions.");
            } else if (statusCode == 429) {
                log.warn("OpenAI rate limit or quota exceeded (HTTP 429)");
                throw new AIServiceException("OpenAI rate limit or usage quota exceeded. Please check your account quota and try again.");
            } else {
                log.error("OpenAI API returned error status HTTP {}: {}", statusCode, responseBody);
                throw new AIServiceException("OpenAI service returned error HTTP " + statusCode + ". Please try again later.");
            }

        } catch (HttpTimeoutException e) {
            log.error("OpenAI request timed out after {} seconds", timeoutSeconds);
            throw new AIServiceException("AI provider request timed out after " + timeoutSeconds + " seconds", e);
        } catch (IOException e) {
            log.error("IO failure communicating with OpenAI API: {}", e.getMessage());
            throw new AIServiceException("Failed to communicate with AI provider: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AIServiceException("AI request thread was interrupted", e);
        }
    }

    /**
     * Cleans accidental markdown code fences (e.g. ```json ... ```) sometimes returned by LLMs.
     */
    private String cleanJsonMarkdown(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            // Strip first line (e.g. ```json)
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }
}
