package com.projectforge.markdown.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.GeneratorUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generates DEVELOPMENT_PLAN.md from the approved project specification.
 */
@Component
public class DevelopmentPlanGenerator implements DocumentContentGenerator {

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.DEVELOPMENT_PLAN;
    }

    @Override
    public String generateContent(Project project, JsonNode spec) {
        String projectName = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
        List<String> backend = GeneratorUtils.getStringList(spec.path("technology"), "backend");
        List<String> frontend = GeneratorUtils.getStringList(spec.path("technology"), "frontend");
        List<String> database = GeneratorUtils.getStringList(spec.path("technology"), "database");

        StringBuilder sb = new StringBuilder();
        sb.append("# Implementation Roadmap & Development Plan: ").append(projectName).append("\n\n");
        sb.append("This step-by-step development sequence structures project delivery into incremental, verifiable phases.\n\n");

        sb.append("## Phase 1 — Project Foundation & Scaffold\n\n");
        sb.append("- Initialize source control repository and build tool configuration.\n");
        if (!backend.isEmpty()) {
            sb.append("- Bootstrap backend application framework (`").append(String.join(", ", backend)).append("`).\n");
        }
        if (!frontend.isEmpty()) {
            sb.append("- Scaffold client project workspace (`").append(String.join(", ", frontend)).append("`).\n");
        }
        sb.append("- Establish health check endpoint (`/api/health`) and basic environment properties.\n");
        sb.append("- **Verification Gate**: Project builds cleanly and passes initial unit tests.\n\n");

        sb.append("## Phase 2 — Authentication & Security Foundation\n\n");
        sb.append("- Configure security filter chain, password hashing (BCrypt), and JWT token management.\n");
        sb.append("- Implement registration, login, and token extraction filters.\n");
        sb.append("- Enforce role-based access control (RBAC) and user ownership.\n");
        sb.append("- **Verification Gate**: Security tests verify authenticated access, token expiration, and anti-IDOR isolation.\n\n");

        sb.append("## Phase 3 — Data Models & Persistence\n\n");
        if (!database.isEmpty()) {
            sb.append("- Connect to database engine (`").append(String.join(", ", database)).append("`).\n");
        }
        sb.append("- Implement entity domain models and database repositories according to [DATABASE.md](./DATABASE.md).\n");
        sb.append("- Configure foreign key constraints, indexes, and transactional boundaries.\n");
        sb.append("- **Verification Gate**: Repository tests verify CRUD operations and relational integrity.\n\n");

        sb.append("## Phase 4 — Core Domain Features & REST APIs\n\n");
        sb.append("- Implement domain services for all approved core features listed in [FEATURES.md](./FEATURES.md).\n");
        sb.append("- Expose REST API controllers documented in [API_SPECIFICATION.md](./API_SPECIFICATION.md).\n");
        sb.append("- Wire request validation, error responses, and domain event handlers.\n");
        sb.append("- **Verification Gate**: MockMvc / integration tests cover all API status codes and edge cases.\n\n");

        sb.append("## Phase 5 — Quality Assurance & Comprehensive Testing\n\n");
        sb.append("- Execute comprehensive test suites as described in [TESTING.md](./TESTING.md).\n");
        sb.append("- Run security static analysis (OWASP) and penetration testing checks.\n");
        sb.append("- Verify client-side accessibility and responsive layout across breakpoints.\n");
        sb.append("- **Verification Gate**: >= 80% line coverage and 0 open critical defect tickets.\n\n");

        sb.append("## Phase 6 — Containerization & Deployment\n\n");
        sb.append("- Build production Docker images with multi-stage Dockerfiles.\n");
        sb.append("- Configure deployment manifests according to [DEPLOYMENT.md](./DEPLOYMENT.md).\n");
        sb.append("- Perform dry-run deployment and health check smoke tests.\n");
        sb.append("- **Verification Gate**: Production container passes health checks.\n");

        return sb.toString();
    }
}
