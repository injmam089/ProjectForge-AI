package com.projectforge.markdown.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.GeneratorUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generates ARCHITECTURE.md from the approved project specification.
 */
@Component
public class ArchitectureGenerator implements DocumentContentGenerator {

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.ARCHITECTURE;
    }

    @Override
    public String generateContent(Project project, JsonNode spec) {
        String projectName = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
        String archStyle = GeneratorUtils.getText(spec.path("architecture"), "style", "Layered Modular Architecture");

        StringBuilder sb = new StringBuilder();
        sb.append("# System Architecture: ").append(projectName).append("\n\n");

        sb.append("## 1. Architectural Overview\n\n");
        sb.append("- **Pattern**: ").append(archStyle).append("\n");
        sb.append("- **Primary Goal**: High maintainability, strict domain boundaries, and testable modular design.\n\n");

        sb.append("## 2. System Components\n\n");
        List<String> components = GeneratorUtils.getStringList(spec.path("architecture"), "components");
        if (!components.isEmpty()) {
            for (String comp : components) {
                sb.append("- **").append(comp).append("**: Dedicated functional subsystem.\n");
            }
        } else {
            sb.append("- **Client Tier**: User presentation and interaction layer.\n");
            sb.append("- **Application Tier**: Business logic, domain services, and validation.\n");
            sb.append("- **Data Tier**: Persistence store and transactional repository layer.\n");
        }
        sb.append("\n");

        sb.append("## 3. Data Flow & Interaction\n\n");
        List<String> dataFlows = GeneratorUtils.getStringList(spec.path("architecture"), "dataFlow");
        if (!dataFlows.isEmpty()) {
            for (String flow : dataFlows) {
                sb.append("```text\n").append(flow).append("\n```\n\n");
            }
        } else {
            sb.append("```text\n");
            sb.append("Client (HTTP/REST) ──► Controller ──► Service Layer ──► Repository ──► Database\n");
            sb.append("```\n\n");
        }

        sb.append("## 4. Module Decomposition\n\n");
        JsonNode modulesNode = spec.path("modules");
        if (modulesNode.isArray() && modulesNode.size() > 0) {
            for (JsonNode m : modulesNode) {
                String name = GeneratorUtils.getText(m, "name", "Module");
                String desc = GeneratorUtils.getText(m, "description", "");
                List<String> deps = GeneratorUtils.getStringList(m, "dependencies");

                sb.append("### `").append(name).append("`\n\n");
                sb.append(desc).append("\n\n");
                sb.append("- **Dependencies**: ").append(!deps.isEmpty() ? "`" + String.join("`, `", deps) + "`" : "_None (Root module)_").append("\n\n");
            }
        }

        sb.append("## 5. Authentication Flow\n\n");
        String authMethod = GeneratorUtils.getText(spec.path("security"), "authentication", "JWT Authentication");
        sb.append("The system relies on ").append(authMethod).append(":\n\n");
        sb.append("1. Client submits credentials to authentication endpoint.\n");
        sb.append("2. Server verifies password hash and issues cryptographically signed token.\n");
        sb.append("3. Subsequent API calls transmit token in `Authorization: Bearer <token>` header.\n");
        sb.append("4. Security filter intercepts requests, resolves principal, and enforces role permissions.\n\n");

        sb.append("## 6. Scalability & Reliability Considerations\n\n");
        sb.append("- **Stateless Application Servers**: Enables seamless horizontal scaling behind load balancers.\n");
        sb.append("- **Connection Pooling**: Database connections managed via optimized pooling parameters.\n");
        sb.append("- **Resilience**: Domain logic enforces transactions, input constraints, and graceful degradation.\n");

        return sb.toString();
    }
}
