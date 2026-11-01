package com.projectforge.markdown.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.GeneratorUtils;
import org.springframework.stereotype.Component;

/**
 * Generates API_SPECIFICATION.md from the approved project specification.
 */
@Component
public class ApiSpecificationGenerator implements DocumentContentGenerator {

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.API_SPECIFICATION;
    }

    @Override
    public String generateContent(Project project, JsonNode spec) {
        String projectName = GeneratorUtils.getText(spec.path("project"), "name", project.getName());

        StringBuilder sb = new StringBuilder();
        sb.append("# API Specification: ").append(projectName).append("\n\n");
        sb.append("This document establishes the exact REST API contracts defined in the approved project specification.\n\n");

        sb.append("## 1. General API Principles\n\n");
        sb.append("- **Protocol**: HTTP/1.1 and HTTP/2 over TLS.\n");
        sb.append("- **Content-Type**: `application/json` for requests and responses.\n");
        sb.append("- **Authentication**: Bearer token transmitted via `Authorization: Bearer <token>` header.\n\n");

        JsonNode apisNode = spec.path("apis");
        if (apisNode.isArray() && apisNode.size() > 0) {
            sb.append("## 2. Endpoints Catalogue\n\n");
            for (JsonNode api : apisNode) {
                String method = GeneratorUtils.getText(api, "method", "GET").toUpperCase();
                String path = GeneratorUtils.getText(api, "path", "/");
                String purpose = GeneratorUtils.getText(api, "purpose", "API Endpoint");
                boolean authReq = api.path("authenticationRequired").asBoolean(true);
                String reqBody = GeneratorUtils.getText(api, "requestBody", null);
                String resBody = GeneratorUtils.getText(api, "responseBody", "{ status: 'OK' }");

                sb.append("### `").append(method).append(" ").append(path).append("`\n\n");
                sb.append("- **Purpose**: ").append(purpose).append("\n");
                sb.append("- **Authentication**: ").append(authReq ? "`Required` (`Bearer token`)" : "`Public`").append("\n");

                if (reqBody != null && !reqBody.isBlank() && !"null".equalsIgnoreCase(reqBody)) {
                    sb.append("- **Request Body**:\n");
                    sb.append("  ```json\n  ").append(reqBody).append("\n  ```\n");
                } else {
                    sb.append("- **Request Body**: None (Empty)\n");
                }

                sb.append("- **Response Body**:\n");
                sb.append("  ```json\n  ").append(resBody).append("\n  ```\n");

                sb.append("- **Standard Error Responses**:\n");
                sb.append("  - `400 Bad Request`: Payload validation error or invalid request parameters.\n");
                if (authReq) {
                    sb.append("  - `401 Unauthorized`: Missing, expired, or malformed JWT token.\n");
                    sb.append("  - `403 Forbidden`: Authenticated user lacks required permissions.\n");
                }
                sb.append("  - `404 Not Found`: Target resource not found or unauthorized cross-tenant attempt.\n");
                sb.append("  - `500 Internal Server Error`: Unexpected internal service error.\n\n");
            }
        } else {
            sb.append("## 2. Endpoints\n\n");
            sb.append("_No explicit REST API endpoints defined in approved specification._\n\n");
        }

        return sb.toString();
    }
}
