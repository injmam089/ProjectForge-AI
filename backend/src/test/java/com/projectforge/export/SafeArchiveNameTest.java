package com.projectforge.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SafeArchiveNameTest {

    @Test
    @DisplayName("Sanitizes standard project names with special characters and colons")
    void sanitizeSpecialCharacters() {
        String result = SafeArchiveName.sanitizeProjectName("My Project: AI/ML");
        assertThat(result).isEqualTo("My_Project_AI_ML");
        assertThat(SafeArchiveName.getZipFileName("My Project: AI/ML"))
                .isEqualTo("ProjectForge_My_Project_AI_ML.zip");
    }

    @Test
    @DisplayName("Handles slashes, backslashes, and path traversal tokens")
    void sanitizePathTraversalTokens() {
        String result = SafeArchiveName.sanitizeProjectName("../../evil/path\\name");
        assertThat(result).doesNotContain("..").doesNotContain("/").doesNotContain("\\");
        assertThat(result).isEqualTo("evil_path_name");
    }

    @Test
    @DisplayName("Falls back to 'project' for empty, blank, or null names")
    void fallbackForBlank() {
        assertThat(SafeArchiveName.sanitizeProjectName(null)).isEqualTo("project");
        assertThat(SafeArchiveName.sanitizeProjectName("")).isEqualTo("project");
        assertThat(SafeArchiveName.sanitizeProjectName("   ")).isEqualTo("project");
        assertThat(SafeArchiveName.sanitizeProjectName(":::///\\\\")).isEqualTo("project");
    }

    @Test
    @DisplayName("Truncates overly long project names to 50 characters")
    void truncateLongNames() {
        String veryLongName = "This Is An Extremely Long Project Name That Definitely Exceeds The Maximum Allowed Slug Length Limit";
        String result = SafeArchiveName.sanitizeProjectName(veryLongName);
        assertThat(result.length()).isLessThanOrEqualTo(50);
        assertThat(result).doesNotEndWith("_");
    }

    @Test
    @DisplayName("Generates proper archive base directory and zip filenames")
    void baseDirectoryAndZipFileName() {
        assertThat(SafeArchiveName.getArchiveBaseName("CloudMatrix"))
                .isEqualTo("ProjectForge_CloudMatrix");
        assertThat(SafeArchiveName.getZipFileName("CloudMatrix"))
                .isEqualTo("ProjectForge_CloudMatrix.zip");
    }
}
