package com.projectforge.markdown.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.GeneratorUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generates TECH_STACK.md from the approved project specification.
 */
@Component
public class TechStackGenerator implements DocumentContentGenerator {

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.TECH_STACK;
    }

    @Override
    public String generateContent(Project project, JsonNode spec) {
        String projectName = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
        JsonNode tech = spec.path("technology");

        List<String> frontend = GeneratorUtils.getStringList(tech, "frontend");
        List<String> backend = GeneratorUtils.getStringList(tech, "backend");
        List<String> database = GeneratorUtils.getStringList(tech, "database");
        List<String> ai = GeneratorUtils.getStringList(tech, "ai");
        List<String> deployment = GeneratorUtils.getStringList(tech, "deployment");
        List<String> other = GeneratorUtils.getStringList(tech, "other");

        String auth = GeneratorUtils.getText(spec.path("security"), "authentication", "Token Authentication");
        List<String> unitTesting = GeneratorUtils.getStringList(spec.path("testing"), "unitTesting");
        List<String> intTesting = GeneratorUtils.getStringList(spec.path("testing"), "integrationTesting");

        StringBuilder sb = new StringBuilder();
        sb.append("# Approved Technology Stack: ").append(projectName).append("\n\n");
        sb.append("> **Mandatory Rule**: The technologies listed below represent the approved architectural baseline. No substitutions or unauthorized libraries may be introduced.\n\n");

        sb.append("## 1. Overview Matrix\n\n");
        sb.append("| Layer | Approved Technologies |\n");
        sb.append("| :--- | :--- |\n");
        sb.append("| **Frontend** | ").append(GeneratorUtils.joinOrNone(frontend)).append(" |\n");
        sb.append("| **Backend** | ").append(GeneratorUtils.joinOrNone(backend)).append(" |\n");
        sb.append("| **Database / Storage** | ").append(GeneratorUtils.joinOrNone(database)).append(" |\n");
        if (!ai.isEmpty()) {
            sb.append("| **AI / Machine Learning** | ").append(String.join(", ", ai)).append(" |\n");
        }
        sb.append("| **Authentication & Security** | ").append(auth).append(" |\n");
        sb.append("| **Testing** | ").append(joinTesting(unitTesting, intTesting)).append(" |\n");
        sb.append("| **Infrastructure & Deployment** | ").append(GeneratorUtils.joinOrNone(deployment)).append(" |\n\n");

        // Section details
        sb.append("## 2. Frontend Layer\n\n");
        if (!frontend.isEmpty()) {
            for (String f : frontend) {
                sb.append("- **").append(f).append("**: Client application library/framework as approved.\n");
            }
        } else {
            sb.append("_No dedicated frontend layer specified in approved specification._\n");
        }
        sb.append("\n");

        sb.append("## 3. Backend & Core Services\n\n");
        if (!backend.isEmpty()) {
            for (String b : backend) {
                sb.append("- **").append(b).append("**: Core server runtime and framework.\n");
            }
        } else {
            sb.append("- Standard backend services as defined in architecture.\n");
        }
        sb.append("\n");

        sb.append("## 4. Database & Persistence\n\n");
        if (!database.isEmpty()) {
            for (String d : database) {
                sb.append("- **").append(d).append("**: Relational or storage engine.\n");
            }
        } else {
            sb.append("_No persistent database specified._\n");
        }
        sb.append("\n");

        if (!ai.isEmpty()) {
            sb.append("## 5. AI & Machine Learning Components\n\n");
            for (String a : ai) {
                sb.append("- **").append(a).append("**: Intelligent processing component.\n");
            }
            sb.append("\n");
        }

        sb.append("## 6. Authentication & Security\n\n");
        sb.append("- **Authentication**: ").append(auth).append("\n");
        String authz = GeneratorUtils.getText(spec.path("security"), "authorization", "Role-Based Access Control");
        sb.append("- **Authorization**: ").append(authz).append("\n\n");

        sb.append("## 7. Testing Frameworks\n\n");
        if (!unitTesting.isEmpty()) sb.append("- **Unit Testing**: ").append(String.join(", ", unitTesting)).append("\n");
        if (!intTesting.isEmpty()) sb.append("- **Integration Testing**: ").append(String.join(", ", intTesting)).append("\n");
        sb.append("\n");

        sb.append("## 8. Deployment & Other Tools\n\n");
        if (!deployment.isEmpty()) sb.append("- **Deployment**: ").append(String.join(", ", deployment)).append("\n");
        if (!other.isEmpty()) sb.append("- **Supporting Utilities**: ").append(String.join(", ", other)).append("\n");

        return sb.toString();
    }

    private String joinTesting(List<String> u, List<String> i) {
        if (u.isEmpty() && i.isEmpty()) return "Standard testing frameworks";
        if (u.isEmpty()) return String.join(", ", i);
        if (i.isEmpty()) return String.join(", ", u);
        return String.join(", ", u) + ", " + String.join(", ", i);
    }
}
