package com.projectforge.dto;

import java.time.Instant;

/**
 * Metadata response returned when preparing an export package.
 */
public record ExportResponse(
        Long projectId,
        String projectName,
        int documentCount,
        String filename,
        Instant exportedAt
) {
}
