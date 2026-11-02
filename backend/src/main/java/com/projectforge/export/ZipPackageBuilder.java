package com.projectforge.export;

import com.projectforge.entity.Document;
import com.projectforge.entity.Project;
import com.projectforge.exception.ExportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Builds and cryptographically validates in-memory ZIP documentation packages.
 * Uses standard java.util.zip with strict path traversal defenses.
 */
@Component
public class ZipPackageBuilder {

    private static final Logger log = LoggerFactory.getLogger(ZipPackageBuilder.class);

    private final ExportManifestBuilder manifestBuilder;

    public ZipPackageBuilder(ExportManifestBuilder manifestBuilder) {
        this.manifestBuilder = manifestBuilder;
    }

    /**
     * Builds and validates a clean ZIP package containing current Markdown documents and manifest.
     */
    public byte[] buildAndValidatePackage(Project project, List<Document> documents) {
        String baseDirName = SafeArchiveName.getArchiveBaseName(project.getName());
        String rootDir = baseDirName + "/";

        ExportManifest manifest = manifestBuilder.buildManifest(project, documents);
        byte[] manifestBytes = manifestBuilder.toBytes(manifest);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            // Write each markdown document
            for (Document doc : documents) {
                String entryPath = rootDir + doc.getFilename();
                validateEntryPath(entryPath, rootDir);

                ZipEntry entry = new ZipEntry(entryPath);
                long epochMilli = doc.getUpdatedAt() != null ? doc.getUpdatedAt().toEpochMilli() : System.currentTimeMillis();
                entry.setTime(epochMilli);

                zos.putNextEntry(entry);
                zos.write(doc.getContent().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            // Write manifest entry
            String manifestEntryPath = rootDir + "projectforge/EXPORT_MANIFEST.json";
            validateEntryPath(manifestEntryPath, rootDir);

            ZipEntry manifestEntry = new ZipEntry(manifestEntryPath);
            manifestEntry.setTime(System.currentTimeMillis());
            zos.putNextEntry(manifestEntry);
            zos.write(manifestBytes);
            zos.closeEntry();

            zos.finish();
        } catch (IOException e) {
            throw new ExportException("Failed to construct ZIP package: " + e.getMessage(), e);
        }

        byte[] zipBytes = baos.toByteArray();

        // Perform strict post-generation verification
        validateGeneratedZip(zipBytes, rootDir, documents);

        return zipBytes;
    }

    /**
     * Re-opens the generated ZIP archive from memory to verify structure, integrity, and entry safety.
     */
    public void validateGeneratedZip(byte[] zipBytes, String rootDir, List<Document> expectedDocuments) {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new ExportException("Generated ZIP archive is empty or null");
        }

        Set<String> expectedFilenames = new HashSet<>();
        for (Document d : expectedDocuments) {
            expectedFilenames.add(rootDir + d.getFilename());
        }

        Set<String> foundEntries = new HashSet<>();
        boolean manifestFound = false;
        String manifestPath = rootDir + "projectforge/EXPORT_MANIFEST.json";

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // Path traversal check
                if (!entryName.startsWith(rootDir) || entryName.contains("..") || entryName.contains("\\") || entryName.contains(":")) {
                    throw new ExportException("Invalid path in generated ZIP entry: " + entryName);
                }

                if (!foundEntries.add(entryName)) {
                    throw new ExportException("Duplicate entry in generated ZIP archive: " + entryName);
                }

                if (manifestPath.equals(entryName)) {
                    manifestFound = true;
                    byte[] manifestBytes = zis.readAllBytes();
                    ExportManifest parsedManifest = manifestBuilder.fromBytes(manifestBytes);
                    if (parsedManifest.documentCount() != expectedDocuments.size()) {
                        throw new ExportException("Manifest document count mismatch: " + parsedManifest.documentCount()
                                + " vs expected " + expectedDocuments.size());
                    }
                } else if (expectedFilenames.contains(entryName)) {
                    byte[] content = zis.readAllBytes();
                    if (content.length > ExportConstants.MAX_SINGLE_DOCUMENT_SIZE) {
                        throw new ExportException("Zip entry content size exceeds maximum limit: " + entryName);
                    }
                } else {
                    throw new ExportException("Unexpected entry found in generated ZIP archive: " + entryName);
                }

                zis.closeEntry();
            }
        } catch (ExportException e) {
            throw e;
        } catch (Exception e) {
            throw new ExportException("Generated ZIP archive could not be parsed: " + e.getMessage(), e);
        }

        if (!manifestFound) {
            throw new ExportException("Export manifest missing from generated ZIP archive");
        }

        if (foundEntries.size() != expectedDocuments.size() + 1) { // documents + manifest
            throw new ExportException("ZIP entry count mismatch: found " + foundEntries.size()
                    + ", expected " + (expectedDocuments.size() + 1));
        }

        for (String expectedPath : expectedFilenames) {
            if (!foundEntries.contains(expectedPath)) {
                throw new ExportException("Expected document missing from ZIP archive: " + expectedPath);
            }
        }
    }

    private void validateEntryPath(String entryPath, String rootDir) {
        if (entryPath == null || !entryPath.startsWith(rootDir) || entryPath.contains("..") || entryPath.contains("\\") || entryPath.contains(":")) {
            throw new ExportException("Illegal ZIP entry path: " + entryPath);
        }
    }
}
