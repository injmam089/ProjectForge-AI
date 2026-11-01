package com.projectforge.ai;

/**
 * Standardized request model for AI language model completions.
 */
public record AIRequest(
        String systemPrompt,
        String userPrompt,
        double temperature,
        Integer maxTokens,
        boolean jsonResponseFormat
) {
    public AIRequest(String systemPrompt, String userPrompt, double temperature, Integer maxTokens) {
        this(systemPrompt, userPrompt, temperature, maxTokens, true);
    }

    public static AIRequest of(String systemPrompt, String userPrompt) {
        return new AIRequest(systemPrompt, userPrompt, 0.2, 4000, true);
    }

    public static AIRequest text(String systemPrompt, String userPrompt) {
        return new AIRequest(systemPrompt, userPrompt, 0.3, 4000, false);
    }
}
