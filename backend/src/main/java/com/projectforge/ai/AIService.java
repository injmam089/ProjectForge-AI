package com.projectforge.ai;

/**
 * Service abstraction for interacting with AI provider APIs (e.g. OpenAI).
 */
public interface AIService {

    /**
     * Executes a completion request against the AI provider.
     *
     * @param request the structured AI prompt request
     * @return the provider response containing text content
     */
    AIResponse generate(AIRequest request);
}
