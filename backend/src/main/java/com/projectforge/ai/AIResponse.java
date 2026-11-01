package com.projectforge.ai;

/**
 * Standardized response model returned by AI providers.
 */
public record AIResponse(
        String content,
        int promptTokens,
        int completionTokens,
        String model
) {}
