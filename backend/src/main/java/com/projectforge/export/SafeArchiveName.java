package com.projectforge.export;

/**
 * Sanitizes project names for safe ZIP archive filenames and root directories.
 * Strictly prevents path traversal, control characters, and unsafe filesystem symbols.
 */
public final class SafeArchiveName {

    private static final int MAX_SLUG_LENGTH = 50;

    private SafeArchiveName() {
    }

    /**
     * Sanitizes a project name into a filesystem-safe identifier.
     * Replaces unsafe symbols, colons, slashes, and control characters with underscores.
     */
    public static String sanitizeProjectName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "project";
        }

        String sanitized = rawName.trim()
                .replaceAll("[\\\\/:*?\"<>|\\cA-\\cZ\\x00-\\x1F\\x7F]+", "_")
                .replaceAll("[^a-zA-Z0-9_-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        if (sanitized.isBlank()) {
            return "project";
        }

        if (sanitized.length() > MAX_SLUG_LENGTH) {
            sanitized = sanitized.substring(0, MAX_SLUG_LENGTH).replaceAll("_+$", "");
        }

        return sanitized.isEmpty() ? "project" : sanitized;
    }

    /**
     * Returns the base root folder name within the ZIP archive.
     */
    public static String getArchiveBaseName(String rawProjectName) {
        return "ProjectForge_" + sanitizeProjectName(rawProjectName);
    }

    /**
     * Returns the full archive filename with .zip extension.
     */
    public static String getZipFileName(String rawProjectName) {
        return getArchiveBaseName(rawProjectName) + ".zip";
    }
}
