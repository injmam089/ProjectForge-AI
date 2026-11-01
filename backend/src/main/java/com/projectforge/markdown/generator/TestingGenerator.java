package com.projectforge.markdown.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.GeneratorUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generates TESTING.md from the approved project specification.
 */
@Component
public class TestingGenerator implements DocumentContentGenerator {

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.TESTING;
    }

    @Override
    public String generateContent(Project project, JsonNode spec) {
        String projectName = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
        List<String> unitTests = GeneratorUtils.getStringList(spec.path("testing"), "unitTesting");
        List<String> intTests = GeneratorUtils.getStringList(spec.path("testing"), "integrationTesting");
        List<String> secTests = GeneratorUtils.getStringList(spec.path("testing"), "securityTesting");

        StringBuilder sb = new StringBuilder();
        sb.append("# Testing Strategy & Verification: ").append(projectName).append("\n\n");
        sb.append("This document outlines the testing pyramid, tools, and verification gates mandatory for this project.\n\n");

        sb.append("## 1. Unit Testing Strategy\n\n");
        if (!unitTests.isEmpty()) {
            sb.append("- **Frameworks / Libraries**: `").append(String.join("`, `", unitTests)).append("`\n");
        } else {
            sb.append("- **Frameworks**: Standard unit testing harness.\n");
        }
        sb.append("- **Scope**: Pure business logic, validators, utility classes, and service domain methods.\n");
        sb.append("- **Mocking**: External dependencies, databases, and network clients must be isolated using mocks or in-memory fakes.\n");
        sb.append("- **Coverage Goal**: >= 80% line and branch coverage on core domain logic.\n\n");

        sb.append("## 2. Integration & API Testing\n\n");
        if (!intTests.isEmpty()) {
            sb.append("- **Frameworks / Libraries**: `").append(String.join("`, `", intTests)).append("`\n");
        } else {
            sb.append("- **Frameworks**: Integration test runner.\n");
        }
        sb.append("- **Scope**: REST controllers, persistence queries, database transactions, and filter chains.\n");
        sb.append("- **Verification**: Test valid payloads (200/201), invalid payloads (400), unauthenticated requests (401), unauthorized actions (403), and missing resources (404).\n\n");

        sb.append("## 3. Security & Anti-IDOR Testing\n\n");
        if (!secTests.isEmpty()) {
            sb.append("- **Security Scanners**: `").append(String.join("`, `", secTests)).append("`\n");
        }
        sb.append("- **Cross-User Authorization**: Verify that User A cannot read, modify, or delete resources owned by User B (strictly expecting 404 Not Found).\n");
        sb.append("- **Token Security**: Verify rejection of expired, forged, or unauthenticated JWT tokens.\n");
        sb.append("- **Injection Vulnerability**: Execute automated tests verifying protection against SQL injection and cross-site scripting.\n\n");

        sb.append("## 4. Test Execution Commands\n\n");
        sb.append("```bash\n");
        sb.append("# Run backend unit and integration test suite\n");
        sb.append("mvn test\n\n");
        sb.append("# Run frontend test suite (if client application)\n");
        sb.append("npm test\n");
        sb.append("```\n");

        return sb.toString();
    }
}
