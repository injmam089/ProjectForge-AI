package com.projectforge.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.projectforge.entity.Document;
import com.projectforge.entity.Project;
import com.projectforge.exception.ExportException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Builds and serializes/deserializes the EXPORT_MANIFEST.json file.
 */
@Component
public class ExportManifestBuilder {

    private final ObjectMapper objectMapper;

    public ExportManifestBuilder() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Constructs a manifest model from the current project and document state.
     */
    public ExportManifest buildManifest(Project project, List<Document> documents) {
        List<ExportManifest.ExportDocumentItem> documentItems = documents.stream()
                .map(doc -> new ExportManifest.ExportDocumentItem(doc.getFilename(), doc.getVersion()))
                .toList();

        return new ExportManifest(
                ExportConstants.PRODUCT_NAME,
                project.getName(),
                project.getId(),
                ExportConstants.EXPORT_VERSION,
                Instant.now(),
                documents.size(),
                documentItems
        );
    }

    /**
     * Serializes the manifest into formatted UTF-8 JSON bytes.
     */
    public byte[] toBytes(ExportManifest manifest) {
        try {
            return objectMapper.writeValueAsBytes(manifest);
        } catch (JsonProcessingException e) {
            throw new ExportException("Failed to serialize export manifest JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Deserializes manifest bytes for post-generation archive validation.
     */
    public ExportManifest fromBytes(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, ExportManifest.class);
        } catch (Exception e) {
            throw new ExportException("Failed to deserialize export manifest JSON: " + e.getMessage(), e);
        }
    }
}
