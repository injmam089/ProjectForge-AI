package com.projectforge.testutil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projectforge.entity.Project;
import com.projectforge.entity.ProjectDifficulty;
import com.projectforge.entity.ProjectStatus;
import com.projectforge.entity.ProjectType;
import com.projectforge.entity.Role;
import com.projectforge.entity.User;

public class TestSpecificationFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String createValidSpecificationJson() {
        return """
        {
          "schemaVersion": "1.0.0",
          "project": {
            "name": "CloudPulse Telemetry",
            "goal": "High-performance distributed telemetry collector for multi-cloud Kubernetes clusters",
            "problem": "DevOps teams lack centralized low-latency metrics aggregation under 50k RPS",
            "type": "API_BACKEND",
            "difficulty": "ADVANCED"
          },
          "users": [
            {
              "role": "DevOps Engineer",
              "description": "Manages cluster observability and telemetry pipeline",
              "responsibilities": ["Inspect metrics", "Configure alert thresholds"],
              "permissions": ["READ_METRICS", "WRITE_ALERTS"]
            }
          ],
          "roles": ["DevOps Engineer", "Site Reliability Engineer"],
          "features": [
            {
              "id": "FEAT-1",
              "name": "Metric Ingestion",
              "description": "High-throughput OpenTelemetry receiver",
              "category": "CORE",
              "priority": "HIGH"
            },
            {
              "id": "FEAT-2",
              "name": "Alerting Pipeline",
              "description": "Evaluates anomalies and triggers webhooks",
              "category": "CORE",
              "priority": "HIGH"
            },
            {
              "id": "FEAT-3",
              "name": "Dashboard Export",
              "description": "Exports time-series graphs",
              "category": "SUPPORTING",
              "priority": "MEDIUM"
            }
          ],
          "modules": [
            {
              "name": "IngestionEngine",
              "description": "Handles stream parsing",
              "responsibilities": ["Buffer incoming telemetry"],
              "dependencies": []
            },
            {
              "name": "AlertManager",
              "description": "Dispatches notifications",
              "responsibilities": ["Evaluate threshold rules"],
              "dependencies": ["IngestionEngine"]
            }
          ],
          "technology": {
            "frontend": ["React", "Vite", "Recharts"],
            "backend": ["Java 17", "Spring Boot 3", "Netty"],
            "database": ["PostgreSQL", "TimescaleDB"],
            "ai": [],
            "deployment": ["Docker", "Kubernetes Helm Chart"],
            "other": ["Prometheus", "Kafka"]
          },
          "architecture": {
            "style": "Event-Driven Modular Monolith",
            "components": ["Ingestion Worker", "Storage Engine", "REST API"],
            "dataFlow": ["OTLP Collector -> Buffer -> TimeSeries DB -> Query Engine"]
          },
          "database": {
            "entities": [
              {
                "name": "telemetry_metrics",
                "fields": ["id BIGSERIAL", "metric_name VARCHAR(100)", "val DOUBLE PRECISION", "recorded_at TIMESTAMPTZ"],
                "primaryKey": "id",
                "foreignKeys": [],
                "relationships": [],
                "constraints": ["NOT NULL (metric_name, val, recorded_at)"]
              },
              {
                "name": "alert_rules",
                "fields": ["id BIGSERIAL", "threshold DOUBLE PRECISION", "channel VARCHAR(50)"],
                "primaryKey": "id",
                "foreignKeys": [],
                "relationships": [],
                "constraints": []
              }
            ],
            "relationships": ["One alert rule monitors multiple telemetry metrics"]
          },
          "apis": [
            {
              "method": "POST",
              "path": "/api/metrics",
              "purpose": "Ingest metric batch",
              "authenticationRequired": true,
              "requestBody": "Array of MetricData",
              "responseBody": "{ status: 'ACCEPTED' }"
            },
            {
              "method": "GET",
              "path": "/api/metrics/summary",
              "purpose": "Get aggregated telemetry summary",
              "authenticationRequired": true,
              "requestBody": null,
              "responseBody": "SummaryStats"
            }
          ],
          "security": {
            "authentication": "Bearer JWT with HS-512 signing",
            "authorization": "Role-Based Access Control (RBAC)",
            "securityRequirements": ["TLS 1.3 enforced", "Rate limiting at 100k RPS", "Strict input schema validation"]
          },
          "testing": {
            "unitTesting": ["JUnit 5", "AssertJ", "Mockito"],
            "integrationTesting": ["Testcontainers PostgreSQL", "MockMvc"],
            "securityTesting": ["OWASP ZAP scans"]
          },
          "deployment": {
            "environment": "Kubernetes on AWS EKS or GCP GKE",
            "requirements": ["PostgreSQL 16", "2 vCPU / 4GB RAM minimum per pod"]
          },
          "nonFunctionalRequirements": ["Sub-10ms p99 latency", "99.99% availability"],
          "constraints": ["Must support OpenTelemetry standard"],
          "assumptions": ["Metrics are received over JSON or Protobuf"],
          "openQuestions": [
            {
              "id": "Q1",
              "question": "Should metric compression (Zstandard or Gzip) be mandatory on ingestion?",
              "importance": "HIGH",
              "reason": "Affects memory bandwidth and ingestion CPU utilization"
            }
          ],
          "risks": [
            {
              "risk": "High volume telemetry spikes may saturate write buffers",
              "severity": "HIGH",
              "mitigation": "Introduce backpressure queueing and token bucket rate limits"
            }
          ],
          "qualityScore": 88
        }
        """;
    }

    public static String createValidSpecificationJsonWithoutQuestions() {
        try {
            ObjectNode node = (ObjectNode) MAPPER.readTree(createValidSpecificationJson());
            node.putArray("openQuestions");
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate spec without open questions", e);
        }
    }

    public static JsonNode createValidSpecificationNode() {
        try {
            return MAPPER.readTree(createValidSpecificationJson());
        } catch (Exception e) {
            throw new RuntimeException("Invalid static test specification JSON", e);
        }
    }

    public static Project createTestProject(User user) {
        Project p = new Project(
                user,
                "CloudPulse Telemetry",
                "High-performance distributed telemetry collector for multi-cloud Kubernetes clusters",
                "DevOps teams lack centralized low-latency metrics aggregation under 50k RPS",
                "DevOps and SRE Engineers",
                ProjectType.API_BACKEND,
                ProjectDifficulty.ADVANCED,
                "Java 17, Spring Boot, PostgreSQL",
                "Sub-10ms p99 latency"
        );
        p.setId(1L);
        p.setStatus(ProjectStatus.READY_FOR_ANALYSIS);
        return p;
    }

    public static User createTestUser() {
        User u = new User("Developer", "dev@example.com", "hashed_password", Role.USER);
        u.setId(10L);
        return u;
    }

    public static class StubAIService implements com.projectforge.ai.AIService {
        private com.projectforge.ai.AIResponse response;
        private RuntimeException exceptionToThrow;
        private com.projectforge.ai.AIRequest lastRequest;

        public StubAIService(String defaultResponseContent) {
            this.response = new com.projectforge.ai.AIResponse(defaultResponseContent, 100, 200, "gpt-4o-mini");
        }

        public void setResponse(com.projectforge.ai.AIResponse response) {
            this.response = response;
            this.exceptionToThrow = null;
        }

        public void setExceptionToThrow(RuntimeException ex) {
            this.exceptionToThrow = ex;
        }

        public com.projectforge.ai.AIRequest getLastRequest() {
            return lastRequest;
        }

        @Override
        public com.projectforge.ai.AIResponse generate(com.projectforge.ai.AIRequest request) {
            this.lastRequest = request;
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return response;
        }
    }

    public static class FakeHttpClient extends java.net.http.HttpClient {
        private int statusCode = 200;
        private String responseBody = "{}";
        private java.io.IOException exceptionToThrow;

        public FakeHttpClient(int statusCode, String responseBody) {
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
        public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
        public void setExceptionToThrow(java.io.IOException ex) { this.exceptionToThrow = ex; }

        @Override
        @SuppressWarnings("unchecked")
        public <T> java.net.http.HttpResponse<T> send(java.net.http.HttpRequest request, java.net.http.HttpResponse.BodyHandler<T> responseBodyHandler) throws java.io.IOException {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return new java.net.http.HttpResponse<>() {
                @Override public int statusCode() { return statusCode; }
                @Override public java.net.http.HttpRequest request() { return request; }
                @Override public java.util.Optional<java.net.http.HttpResponse<T>> previousResponse() { return java.util.Optional.empty(); }
                @Override public java.net.http.HttpHeaders headers() { return java.net.http.HttpHeaders.of(java.util.Map.of(), (a, b) -> true); }
                @Override public T body() { return (T) responseBody; }
                @Override public java.util.Optional<javax.net.ssl.SSLSession> sslSession() { return java.util.Optional.empty(); }
                @Override public java.net.URI uri() { return request.uri(); }
                @Override public java.net.http.HttpClient.Version version() { return java.net.http.HttpClient.Version.HTTP_1_1; }
            };
        }

        @Override public <T> java.util.concurrent.CompletableFuture<java.net.http.HttpResponse<T>> sendAsync(java.net.http.HttpRequest request, java.net.http.HttpResponse.BodyHandler<T> responseBodyHandler) { throw new UnsupportedOperationException(); }
        @Override public <T> java.util.concurrent.CompletableFuture<java.net.http.HttpResponse<T>> sendAsync(java.net.http.HttpRequest request, java.net.http.HttpResponse.BodyHandler<T> responseBodyHandler, java.net.http.HttpResponse.PushPromiseHandler<T> pushPromiseHandler) { throw new UnsupportedOperationException(); }
        @Override public java.util.Optional<java.net.CookieHandler> cookieHandler() { return java.util.Optional.empty(); }
        @Override public java.util.Optional<java.time.Duration> connectTimeout() { return java.util.Optional.empty(); }
        @Override public java.net.http.HttpClient.Redirect followRedirects() { return java.net.http.HttpClient.Redirect.NEVER; }
        @Override public java.util.Optional<java.net.ProxySelector> proxy() { return java.util.Optional.empty(); }
        @Override public javax.net.ssl.SSLContext sslContext() { return null; }
        @Override public javax.net.ssl.SSLParameters sslParameters() { return new javax.net.ssl.SSLParameters(); }
        @Override public java.util.Optional<java.net.Authenticator> authenticator() { return java.util.Optional.empty(); }
        @Override public java.net.http.HttpClient.Version version() { return java.net.http.HttpClient.Version.HTTP_1_1; }
        @Override public java.util.Optional<java.util.concurrent.Executor> executor() { return java.util.Optional.empty(); }
    }
}
