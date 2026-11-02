package com.projectforge.export;

import com.projectforge.markdown.DocumentConstants;

/**
 * Centralized constant limits and configuration for ZIP export packages.
 */
public final class ExportConstants {

    private ExportConstants() {
    }

    public static final String EXPORT_VERSION = "1.0";
    public static final String PRODUCT_NAME = "ProjectForge AI";

    public static final int MAX_DOCUMENT_COUNT = 100;
    public static final int MAX_SINGLE_DOCUMENT_SIZE = DocumentConstants.MAX_DOCUMENT_CONTENT_LENGTH; // 500,000 bytes
    public static final int MAX_TOTAL_EXPORT_SIZE = 10_000_000; // 10 MB uncompressed limit
    public static final int MAX_FILENAME_LENGTH = 100;

    public static final String FILENAME_REGEX = "^[a-zA-Z0-9_-]+\\.md$";
}
