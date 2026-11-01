package com.projectforge.ai;

/**
 * Standardized request model for AI language model completions.
 */
public record AIRequest(
        String systemPrompt,
        String userPrompt,
        double temperature,
        Integer maxTokens
) {
    public static AIRequest of(String systemPrompt, String userPrompt) {
        return new AIRequest(systemPrompt, userPrompt, 0.2, 4000);
    }
}
