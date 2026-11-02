package com.projectforge.export;

import com.projectforge.entity.Document;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectType;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ZipPackageBuilderTest {

    private ZipPackageBuilder packageBuilder;
    private Project project;
    private Document doc1;
    private Document doc2;

    @BeforeEach
    void setUp() {
        ExportManifestBuilder manifestBuilder = new ExportManifestBuilder();
        packageBuilder = new ZipPackageBuilder(manifestBuilder);

        User owner = new User("Owner", "owner@example.com", "hash", Role.USER);
        owner.setId(1L);

        project = new Project(
                owner,
                "CloudPulse Telemetry",
                "Telemetry pipeline",
                "Logs",
                "Engineers",
                ProjectType.API_BACKEND,
                ProjectDifficulty.ADVANCED,
                "Java",
                "None"
        );
        project.setId(100L);

        doc1 = new Document(project, DocumentType.README, "README.md", "# CloudPulse Readme Content", 2);
        doc1.setId(10L);

        doc2 = new Document(project, DocumentType.ARCHITECTURE, "ARCHITECTURE.md", "# Architecture Content", 3);
        doc2.setId(11L);
    }

    @Test
    @DisplayName("Builds valid ZIP package containing documents and valid manifest")
    void buildAndValidatePackageSuccess() throws Exception {
        List<Document> docs = List.of(doc1, doc2);
        byte[] zipBytes = packageBuilder.buildAndValidatePackage(project, docs);

        assertThat(zipBytes).isNotEmpty();

        // Inspect zip entries
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(zis.readAllBytes(), StandardCharsets.UTF_8));
                zis.closeEntry();
            }
        }

        String baseDir = "ProjectForge_CloudPulse_Telemetry/";

        assertThat(entries).containsKey(baseDir + "README.md");
        assertThat(entries.get(baseDir + "README.md")).isEqualTo("# CloudPulse Readme Content");

        assertThat(entries).containsKey(baseDir + "ARCHITECTURE.md");
        assertThat(entries.get(baseDir + "ARCHITECTURE.md")).isEqualTo("# Architecture Content");

        assertThat(entries).containsKey(baseDir + "projectforge/EXPORT_MANIFEST.json");
        String manifestJson = entries.get(baseDir + "projectforge/EXPORT_MANIFEST.json");
        assertThat(manifestJson).contains("\"product\" : \"ProjectForge AI\"");
        assertThat(manifestJson).contains("\"projectId\" : 100");
        assertThat(manifestJson).contains("\"documentCount\" : 2");
        assertThat(manifestJson).contains("\"version\" : 2"); // doc1 version
        assertThat(manifestJson).contains("\"version\" : 3"); // doc2 version
    }
}
