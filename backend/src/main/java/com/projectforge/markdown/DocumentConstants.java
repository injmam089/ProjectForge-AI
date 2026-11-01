package com.projectforge.markdown;

/**
 * Centralized constant limits for Markdown documents and AI editing.
 */
public final class DocumentConstants {

    private DocumentConstants() {
    }

    public static final int MIN_DOCUMENT_CONTENT_LENGTH = 20;
    public static final int MAX_DOCUMENT_CONTENT_LENGTH = 500_000;
    public static final int MAX_AI_INSTRUCTION_LENGTH = 2_000;
    public static final int MAX_AI_OUTPUT_LENGTH = 500_000;
}
