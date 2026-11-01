package com.projectforge.markdown.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.GeneratorUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Specialized document generators for AI/ML, IoT, Cybersecurity, and Mobile projects.
 */
public final class SpecializedDocumentGenerators {

    private SpecializedDocumentGenerators() {
    }

    @Component
    public static class ModelRequirementsGenerator implements DocumentContentGenerator {
        @Override public DocumentType getDocumentType() { return DocumentType.MODEL_REQUIREMENTS; }
        @Override public String generateContent(Project project, JsonNode spec) {
            String name = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
            List<String> aiTech = GeneratorUtils.getStringList(spec.path("technology"), "ai");
            StringBuilder sb = new StringBuilder();
            sb.append("# AI & Model Requirements: ").append(name).append("\n\n");
            sb.append("## 1. Machine Learning Architecture\n\n");
            sb.append("- **Frameworks**: `").append(GeneratorUtils.joinOrNone(aiTech)).append("`\n");
            sb.append("- **Model Strategy**: Supervised or pre-trained inference serving with low-latency constraints.\n\n");
            sb.append("## 2. Inference & Evaluation Metrics\n\n");
            sb.append("- **Latency Target**: p95 inference time <= 100ms.\n");
            sb.append("- **Accuracy / F1 Target**: Established against benchmark validation splits.\n");
            return sb.toString();
        }
    }

    @Component
    public static class DatasetGenerator implements DocumentContentGenerator {
        @Override public DocumentType getDocumentType() { return DocumentType.DATASET; }
        @Override public String generateContent(Project project, JsonNode spec) {
            String name = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
            StringBuilder sb = new StringBuilder();
            sb.append("# Dataset Specification & Data Ingestion: ").append(name).append("\n\n");
            sb.append("## 1. Data Ingestion & Preprocessing\n\n");
            sb.append("- **Data Pipeline**: Cleaned, normalized, and validated feature streams.\n");
            sb.append("- **Feature Store**: Standardized numerical and categorical transformations.\n\n");
            sb.append("## 2. Privacy & Data Governance\n\n");
            sb.append("- **PII Protection**: Data anonymization and pseudonymization prior to feature ingestion.\n");
            return sb.toString();
        }
    }

    @Component
    public static class MlPipelineGenerator implements DocumentContentGenerator {
        @Override public DocumentType getDocumentType() { return DocumentType.ML_PIPELINE; }
        @Override public String generateContent(Project project, JsonNode spec) {
            String name = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
            StringBuilder sb = new StringBuilder();
            sb.append("# ML Pipeline & MLOps Architecture: ").append(name).append("\n\n");
            sb.append("## 1. Pipeline Stages\n\n");
            sb.append("1. **Data Ingestion**: Ingest raw datasets from approved sources.\n");
            sb.append("2. **Preprocessing**: Feature scaling, tokenization, and outlier rejection.\n");
            sb.append("3. **Model Inference**: Optimized runtime serving inference requests.\n");
            sb.append("4. **Telemetry & Drift Monitoring**: Model performance tracking and automated retraining triggers.\n");
            return sb.toString();
        }
    }

    @Component
    public static class HardwareRequirementsGenerator implements DocumentContentGenerator {
        @Override public DocumentType getDocumentType() { return DocumentType.HARDWARE_REQUIREMENTS; }
        @Override public String generateContent(Project project, JsonNode spec) {
            String name = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
            StringBuilder sb = new StringBuilder();
            sb.append("# Hardware & Embedded Requirements: ").append(name).append("\n\n");
            sb.append("## 1. Target System Specifications\n\n");
            sb.append("- **Architecture**: ARM / Embedded Linux / RTOS targets.\n");
            sb.append("- **Memory & Compute**: Constrained memory footprint (RAM/Flash budget).\n");
            return sb.toString();
        }
    }

    @Component
    public static class SensorSpecificationGenerator implements DocumentContentGenerator {
        @Override public DocumentType getDocumentType() { return DocumentType.SENSOR_SPECIFICATION; }
        @Override public String generateContent(Project project, JsonNode spec) {
            String name = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
            StringBuilder sb = new StringBuilder();
            sb.append("# Sensor Interfaces & Telemetry: ").append(name).append("\n\n");
            sb.append("## 1. Sensor Polling & Interfaces\n\n");
            sb.append("- **Buses**: I2C, SPI, UART, or GPIO interrupt pins.\n");
            sb.append("- **Telemetry Cadence**: Periodic sampling with edge buffering.\n");
            return sb.toString();
        }
    }

    @Component
    public static class IotProtocolsGenerator implements DocumentContentGenerator {
        @Override public DocumentType getDocumentType() { return DocumentType.IOT_PROTOCOLS; }
        @Override public String generateContent(Project project, JsonNode spec) {
            String name = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
            StringBuilder sb = new StringBuilder();
            sb.append("# IoT Network Protocols & Communication: ").append(name).append("\n\n");
            sb.append("## 1. Network & Transport Protocols\n\n");
            sb.append("- **Protocols**: MQTT / CoAP over TLS 1.3 with mutual certificate authentication.\n");
            sb.append("- **Payload Encoding**: Compact binary (Protobuf/CBOR) or lightweight JSON.\n");
            return sb.toString();
        }
    }

    @Component
    public static class ThreatModelGenerator implements DocumentContentGenerator {
        @Override public DocumentType getDocumentType() { return DocumentType.THREAT_MODEL; }
        @Override public String generateContent(Project project, JsonNode spec) {
            String name = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
            StringBuilder sb = new StringBuilder();
            sb.append("# Threat Model & Attack Surface Analysis: ").append(name).append("\n\n");
            sb.append("## 1. STRIDE Threat Analysis\n\n");
            sb.append("- **Spoofing**: Mitigated via signed JWTs and strong credential hashing.\n");
            sb.append("- **Tampering**: Mitigated via TLS encryption and HMAC integrity validation.\n");
            sb.append("- **Information Disclosure**: Mitigated via user tenant isolation and sanitized logging.\n");
            sb.append("- **Denial of Service**: Mitigated via rate-limiting filters and bounded thread pools.\n");
            return sb.toString();
        }
    }

    @Component
    public static class SecurityTestingGenerator implements DocumentContentGenerator {
        @Override public DocumentType getDocumentType() { return DocumentType.SECURITY_TESTING; }
        @Override public String generateContent(Project project, JsonNode spec) {
            String name = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
            StringBuilder sb = new StringBuilder();
            sb.append("# Security Testing & Penetration Verification: ").append(name).append("\n\n");
            sb.append("## 1. Security Test Plan\n\n");
            sb.append("- **Static Analysis (SAST)**: Automated security vulnerability scanning on dependencies.\n");
            sb.append("- **Dynamic Analysis (DAST)**: Automated OWASP vulnerability probes on API endpoints.\n");
            return sb.toString();
        }
    }

    @Component
    public static class MobileRequirementsGenerator implements DocumentContentGenerator {
        @Override public DocumentType getDocumentType() { return DocumentType.MOBILE_REQUIREMENTS; }
        @Override public String generateContent(Project project, JsonNode spec) {
            String name = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
            StringBuilder sb = new StringBuilder();
            sb.append("# Mobile Client Specifications: ").append(name).append("\n\n");
            sb.append("## 1. Platform Requirements\n\n");
            sb.append("- **Platforms**: iOS and Android native or cross-platform framework.\n");
            sb.append("- **Offline Capability**: Local cache and synchronization upon network re-establishment.\n");
            return sb.toString();
        }
    }
}
