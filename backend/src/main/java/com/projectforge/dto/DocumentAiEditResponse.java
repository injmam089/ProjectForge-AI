package com.projectforge.dto;

/**
 * Result payload returned upon successful AI document edit.
 */
public record DocumentAiEditResponse(
        Long id,
        String filename,
        int version,
        String content,
        DocumentAiAction action,
        String message
) {
}
