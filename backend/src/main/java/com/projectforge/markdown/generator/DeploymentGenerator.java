package com.projectforge.markdown.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.GeneratorUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generates DEPLOYMENT.md from the approved project specification.
 */
@Component
public class DeploymentGenerator implements DocumentContentGenerator {

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.DEPLOYMENT;
    }

    @Override
    public String generateContent(Project project, JsonNode spec) {
        String projectName = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
        String environment = GeneratorUtils.getText(spec.path("deployment"), "environment", "Containerized Docker / Cloud Environment");
        List<String> requirements = GeneratorUtils.getStringList(spec.path("deployment"), "requirements");

        StringBuilder sb = new StringBuilder();
        sb.append("# Deployment & Operations Guide: ").append(projectName).append("\n\n");

        sb.append("## 1. Deployment Architecture\n\n");
        sb.append("- **Target Environment**: ").append(environment).append("\n");
        if (!requirements.isEmpty()) {
            sb.append("- **Resource Requirements**:\n");
            for (String r : requirements) {
                sb.append("  - ").append(r).append("\n");
            }
        }
        sb.append("\n");

        sb.append("## 2. Environment Variables Configuration\n\n");
        sb.append("The application runtime requires the following environment variables:\n\n");
        sb.append("| Variable Name | Description | Example / Default |\n");
        sb.append("| :--- | :--- | :--- |\n");
        sb.append("| `SERVER_PORT` | HTTP listening port | `8080` |\n");
        sb.append("| `SPRING_DATASOURCE_URL` | JDBC database connection string | `jdbc:postgresql://localhost:5432/db` |\n");
        sb.append("| `SPRING_DATASOURCE_USERNAME` | Database credentials username | `app_user` |\n");
        sb.append("| `SPRING_DATASOURCE_PASSWORD` | Database credentials password | `(secret)` |\n");
        sb.append("| `JWT_SECRET` | 256-bit cryptographically secure key | `(secret base64)` |\n");
        sb.append("| `CORS_ALLOWED_ORIGINS` | Permitted client host origins | `http://localhost:5173` |\n\n");

        sb.append("## 3. Containerization (Docker)\n\n");
        sb.append("Build and run the project using containerization:\n\n");
        sb.append("```bash\n");
        sb.append("# Build application packages\n");
        sb.append("mvn clean package -DskipTests\n\n");
        sb.append("# Build and launch containers\n");
        sb.append("docker compose up -d\n");
        sb.append("```\n\n");

        sb.append("## 4. Health Checks & Monitoring\n\n");
        sb.append("- **Health Endpoint**: `GET /api/health` returns status UP.\n");
        sb.append("- **Metrics & Logging**: Container logs directed to standard output for centralized aggregation.\n\n");

        sb.append("## 5. Backup & Recovery\n\n");
        sb.append("- Schedule regular automated database snapshots.\n");
        sb.append("- Store encrypted backups in redundant cloud storage with point-in-time recovery capabilities.\n");

        return sb.toString();
    }
}
