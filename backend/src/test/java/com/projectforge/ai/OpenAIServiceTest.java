package com.projectforge.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectforge.exception.AIServiceException;
import com.projectforge.testutil.TestSpecificationFactory.FakeHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAIServiceTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Successfully parses OpenAI chat completion response and cleans markdown fences")
    void generateSuccessWithMarkdownFences() {
        String jsonPayload = """
        {
          "id": "chatcmpl-123",
          "model": "gpt-4o-mini",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "```json\\n{\\"project\\":{\\"name\\":\\"Test\\"},\\"qualityScore\\":90}\\n```"
              }
            }
          ],
          "usage": {
            "prompt_tokens": 120,
            "completion_tokens": 85
          }
        }
        """;

        FakeHttpClient fakeClient = new FakeHttpClient(200, jsonPayload);
        OpenAIService service = new OpenAIService("sk-valid-key", "gpt-4o-mini", "https://api.openai.com/v1", 30, objectMapper, fakeClient);

        AIResponse response = service.generate(AIRequest.of("System prompt", "User prompt"));

        assertThat(response.content()).isEqualTo("{\"project\":{\"name\":\"Test\"},\"qualityScore\":90}");
        assertThat(response.promptTokens()).isEqualTo(120);
        assertThat(response.completionTokens()).isEqualTo(85);
        assertThat(response.model()).isEqualTo("gpt-4o-mini");
    }

    @Test
    @DisplayName("Throws AIServiceException when API key is blank or placeholder")
    void throwsWhenApiKeyIsBlank() {
        FakeHttpClient fakeClient = new FakeHttpClient(200, "{}");
        OpenAIService service = new OpenAIService("", "gpt-4o-mini", "https://api.openai.com/v1", 30, objectMapper, fakeClient);

        assertThatThrownBy(() -> service.generate(AIRequest.of("System", "User")))
                .isInstanceOf(AIServiceException.class)
                .hasMessageContaining("OpenAI API key is not configured");
    }

    @Test
    @DisplayName("Throws AIServiceException on HTTP 401 authentication failure")
    void throwsOnAuthenticationFailure() {
        FakeHttpClient fakeClient = new FakeHttpClient(401, "{\"error\":{\"message\":\"Invalid API key\"}}");
        OpenAIService service = new OpenAIService("sk-invalid", "gpt-4o-mini", "https://api.openai.com/v1", 30, objectMapper, fakeClient);

        assertThatThrownBy(() -> service.generate(AIRequest.of("System", "User")))
                .isInstanceOf(AIServiceException.class)
                .hasMessageContaining("Authentication failed with OpenAI API");
    }

    @Test
    @DisplayName("Throws AIServiceException on HTTP 429 rate limit exceeded")
    void throwsOnRateLimit() {
        FakeHttpClient fakeClient = new FakeHttpClient(429, "{\"error\":{\"message\":\"Rate limit reached\"}}");
        OpenAIService service = new OpenAIService("sk-key", "gpt-4o-mini", "https://api.openai.com/v1", 30, objectMapper, fakeClient);

        assertThatThrownBy(() -> service.generate(AIRequest.of("System", "User")))
                .isInstanceOf(AIServiceException.class)
                .hasMessageContaining("rate limit or usage quota exceeded");
    }

    @Test
    @DisplayName("Throws AIServiceException on HTTP 500 server error")
    void throwsOnServerError() {
        FakeHttpClient fakeClient = new FakeHttpClient(500, "Internal Server Error");
        OpenAIService service = new OpenAIService("sk-key", "gpt-4o-mini", "https://api.openai.com/v1", 30, objectMapper, fakeClient);

        assertThatThrownBy(() -> service.generate(AIRequest.of("System", "User")))
                .isInstanceOf(AIServiceException.class)
                .hasMessageContaining("OpenAI service returned error HTTP 500");
    }

    @Test
    @DisplayName("Throws AIServiceException on request timeout")
    void throwsOnTimeout() {
        FakeHttpClient fakeClient = new FakeHttpClient(200, "{}");
        fakeClient.setExceptionToThrow(new HttpTimeoutException("Connection timed out"));

        OpenAIService service = new OpenAIService("sk-key", "gpt-4o-mini", "https://api.openai.com/v1", 10, objectMapper, fakeClient);

        assertThatThrownBy(() -> service.generate(AIRequest.of("System", "User")))
                .isInstanceOf(AIServiceException.class)
                .hasMessageContaining("timed out");
    }
}
