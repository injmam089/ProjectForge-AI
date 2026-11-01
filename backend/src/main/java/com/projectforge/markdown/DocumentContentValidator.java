package com.projectforge.markdown;

import com.projectforge.exception.MarkdownValidationException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Reusable validator for Markdown document content across manual saves,
 * AI edits, version restores, and regeneration.
 */
@Component
public class DocumentContentValidator {

    private static final Pattern DANGEROUS_HTML_PATTERN = Pattern.compile(
            "(?i)<\\s*(script|iframe|object|embed|applet|meta|link|style)[^>]*>" +
            "|(?i)<[^>]+(on[a-z]+|javascript:)\\s*=" +
            "|(?i)javascript:\\s*" +
            "|(?i)data:text/html"
    );

    private static final Pattern MARKDOWN_HEADER_PATTERN = Pattern.compile("(?m)^#{1,6}\\s+.+");

    /**
     * Validates Markdown content against safety, length, and structural requirements.
     *
     * @param filename the target filename
     * @param content  the Markdown content string
     * @throws MarkdownValidationException if content is invalid or unsafe
     */
    public void validateContent(String filename, String content) {
        if (content == null || content.isBlank()) {
            throw new MarkdownValidationException("Document content for " + filename + " must not be blank.");
        }

        int length = content.length();
        if (length < DocumentConstants.MIN_DOCUMENT_CONTENT_LENGTH) {
            throw new MarkdownValidationException(
                    "Document content for " + filename + " is too short (" + length + " characters). Minimum required is " +
                            DocumentConstants.MIN_DOCUMENT_CONTENT_LENGTH + " characters."
            );
        }

        if (length > DocumentConstants.MAX_DOCUMENT_CONTENT_LENGTH) {
            throw new MarkdownValidationException(
                    "Document content for " + filename + " exceeds maximum limit of " +
                            DocumentConstants.MAX_DOCUMENT_CONTENT_LENGTH + " characters (was " + length + ")."
            );
        }

        // Safety: verify no XSS or active content injection in raw HTML
        if (DANGEROUS_HTML_PATTERN.matcher(content).find()) {
            throw new MarkdownValidationException(
                    "Document content for " + filename + " contains forbidden HTML or script elements."
            );
        }

        // Structure: verify document contains at least one markdown header
        if (!MARKDOWN_HEADER_PATTERN.matcher(content).find()) {
            throw new MarkdownValidationException(
                    "Document content for " + filename + " must contain at least one valid Markdown heading (# Heading)."
            );
        }
    }
}
