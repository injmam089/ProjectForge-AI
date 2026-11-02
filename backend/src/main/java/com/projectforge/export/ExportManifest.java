package com.projectforge.export;

import java.time.Instant;
import java.util.List;

/**
 * Model representing the EXPORT_MANIFEST.json included inside every generated ZIP package.
 */
public record ExportManifest(
        String product,
        String projectName,
        Long projectId,
        String exportVersion,
        Instant generatedAt,
        int documentCount,
        List<ExportDocumentItem> documents
) {
    public record ExportDocumentItem(
            String filename,
            int version
    ) {
    }
}
