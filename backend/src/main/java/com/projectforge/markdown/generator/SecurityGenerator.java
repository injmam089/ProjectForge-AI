package com.projectforge.markdown.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.GeneratorUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generates SECURITY.md from the approved project specification.
 */
@Component
public class SecurityGenerator implements DocumentContentGenerator {

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.SECURITY;
    }

    @Override
    public String generateContent(Project project, JsonNode spec) {
        String projectName = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
        String auth = GeneratorUtils.getText(spec.path("security"), "authentication", "JWT Authentication");
        String authz = GeneratorUtils.getText(spec.path("security"), "authorization", "Role-Based Access Control (RBAC)");
        List<String> secReqs = GeneratorUtils.getStringList(spec.path("security"), "securityRequirements");

        StringBuilder sb = new StringBuilder();
        sb.append("# Security Requirements & Controls: ").append(projectName).append("\n\n");
        sb.append("> **Notice**: This document defines mandatory security controls and requirements that developers and AI systems **must implement** during software development.\n\n");

        sb.append("## 1. Authentication Requirements\n\n");
        sb.append("- **Mechanism**: ").append(auth).append("\n");
        sb.append("- **Token Signing**: Cryptographically signed tokens (e.g. HMAC-SHA256 or RSA-256) with short expiration.\n");
        sb.append("- **Password Storage**: Passwords must be hashed using adaptive key derivation (BCrypt / Argon2) with per-user salt.\n\n");

        sb.append("## 2. Authorization & Tenant Isolation\n\n");
        sb.append("- **Access Model**: ").append(authz).append("\n");
        sb.append("- **Anti-IDOR Policy**: Direct Object Reference lookups must verify ownership via authenticated user identity. Unowned resource requests must return `404 Not Found` to prevent resource enumeration.\n\n");

        sb.append("## 3. Approved Security Controls\n\n");
        if (!secReqs.isEmpty()) {
            for (String req : secReqs) {
                sb.append("- **Must Implement**: ").append(req).append("\n");
            }
        } else {
            sb.append("- Standard TLS encryption, input sanitization, and secure header configuration required.\n");
        }
        sb.append("\n");

        sb.append("## 4. Input Validation & Defense-in-Depth\n\n");
        sb.append("- **Strict Schema Validation**: All request bodies, query parameters, and path variables must be validated using Bean Validation or strict schema checks.\n");
        sb.append("- **SQL Injection Prevention**: Object-Relational Mapping (ORM) and parameterized queries must be used exclusively.\n");
        sb.append("- **Cross-Site Scripting (XSS)**: All user-supplied output rendered in browsers must be appropriately context-escaped.\n\n");

        sb.append("## 5. Secrets Management & Logging\n\n");
        sb.append("- **No Hardcoded Secrets**: Passwords, encryption keys, and tokens must be injected via environment variables.\n");
        sb.append("- **Sanitized Logging**: Log messages must never output passwords, tokens, full credit card numbers, or PII.\n");
        sb.append("- **Information Leakage**: Stack traces and internal database error messages must never be exposed to API callers.\n");

        return sb.toString();
    }
}
