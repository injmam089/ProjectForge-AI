package com.projectforge.markdown.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.GeneratorUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generates AI_BUILD_INSTRUCTIONS.md from the approved project specification.
 * Serves as the prompt guide and strict guardrail document for AI coding assistants.
 */
@Component
public class AiBuildInstructionsGenerator implements DocumentContentGenerator {

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.AI_BUILD_INSTRUCTIONS;
    }

    @Override
    public String generateContent(Project project, JsonNode spec) {
        String projectName = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
        String goal = GeneratorUtils.getText(spec.path("project"), "goal", project.getDescription());
        List<String> backend = GeneratorUtils.getStringList(spec.path("technology"), "backend");
        List<String> frontend = GeneratorUtils.getStringList(spec.path("technology"), "frontend");
        List<String> database = GeneratorUtils.getStringList(spec.path("technology"), "database");

        StringBuilder sb = new StringBuilder();
        sb.append("# AI Build Instructions & Agent Prompt Guide: ").append(projectName).append("\n\n");
        sb.append("> **IMPORTANT NOTICE FOR AI CODING ASSISTANTS & ENGINEERS**:\n");
        sb.append("> This document governs all automated or semi-automated code generation for `").append(projectName).append("`.\n");
        sb.append("> You must adhere strictly to these instructions and never deviate from the approved specification.\n\n");

        sb.append("## 1. Project Context & Mission\n\n");
        sb.append(goal).append("\n\n");
        sb.append("- **Project Type**: `").append(project.getProjectType()).append("`\n");
        sb.append("- **Complexity Level**: `").append(project.getDifficulty()).append("`\n\n");

        sb.append("## 2. Mandatory Architectural Rules\n\n");
        sb.append("1. **Single Source of Truth**: The approved specification JSON and companion documents in this repository are the sole authority. Do NOT invent new database tables, API endpoints, or unapproved technologies.\n");
        sb.append("2. **No Hallucinated Libraries**: Use only approved dependencies:\n");
        if (!backend.isEmpty()) sb.append("   - Backend: `").append(String.join("`, `", backend)).append("`\n");
        if (!frontend.isEmpty()) sb.append("   - Frontend: `").append(String.join("`, `", frontend)).append("`\n");
        if (!database.isEmpty()) sb.append("   - Database: `").append(String.join("`, `", database)).append("`\n");
        sb.append("3. **Layered Separation**: Enforce strict separation between Controllers, Services, Repositories, and Entities. Business logic must never reside in controllers.\n");
        sb.append("4. **Anti-IDOR Guardrails**: All entity queries must verify owning user ID. Unowned lookups must return `404 Not Found` with zero metadata leakage.\n\n");

        sb.append("## 3. Implementation Order (Phase Sequences)\n\n");
        sb.append("When building the codebase, follow this sequential order:\n\n");
        sb.append("1. **Step 1**: Foundation & Health Check (`/api/health`).\n");
        sb.append("2. **Step 2**: Security Filter, JWT Token generation & validation, User entity, and Auth endpoints.\n");
        sb.append("3. **Step 3**: Relational entities and repositories according to [DATABASE.md](./DATABASE.md).\n");
        sb.append("4. **Step 4**: Domain services and REST API controllers according to [API_SPECIFICATION.md](./API_SPECIFICATION.md).\n");
        sb.append("5. **Step 5**: Comprehensive unit and integration test suites according to [TESTING.md](./TESTING.md).\n\n");

        sb.append("## 4. Definition of Done & Quality Checklist\n\n");
        sb.append("- [ ] `mvn clean test` runs with 100% passing tests.\n");
        sb.append("- [ ] Zero hardcoded passwords, tokens, or encryption keys in source files.\n");
        sb.append("- [ ] All request payloads validated using strict schema constraints.\n");
        sb.append("- [ ] Docker build succeeds with clean container execution.\n\n");

        sb.append("## 5. Prohibited Actions (What NOT to Do)\n\n");
        sb.append("- ❌ Do NOT introduce unapproved database engines (e.g. replacing PostgreSQL with MongoDB).\n");
        sb.append("- ❌ Do NOT bypass authentication on private domain endpoints.\n");
        sb.append("- ❌ Do NOT return internal server stack traces to API callers.\n");
        sb.append("- ❌ Do NOT create orphan files or duplicate models outside the established package hierarchy.\n");

        return sb.toString();
    }
}
