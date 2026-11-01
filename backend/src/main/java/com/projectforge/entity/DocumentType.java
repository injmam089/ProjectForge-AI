package com.projectforge.entity;

/**
 * Controlled document types for generated Markdown documentation in ProjectForge AI.
 * Each document type maps to an approved standard or specialized documentation artifact.
 */
public enum DocumentType {
    // Standard Document Set
    README("README.md", "Project Overview & Quickstart Guide"),
    PROJECT_REQUIREMENTS("PROJECT_REQUIREMENTS.md", "Comprehensive Functional & Non-Functional Requirements"),
    PROJECT_GOALS("PROJECT_GOALS.md", "Strategic Goals, Objectives & Success Criteria"),
    FEATURES("FEATURES.md", "Categorized Feature Breakdown & Prioritization"),
    TECH_STACK("TECH_STACK.md", "Approved Technology Stack & Component Rationale"),
    ARCHITECTURE("ARCHITECTURE.md", "System Architecture, Topology & Data Flows"),
    DATABASE("DATABASE.md", "Database Schema, Entities, Relationships & Constraints"),
    API_SPECIFICATION("API_SPECIFICATION.md", "REST API Endpoint Contracts & Payloads"),
    SECURITY("SECURITY.md", "Security Policy, Authentication, RBAC & Protection Controls"),
    UI_UX_REQUIREMENTS("UI_UX_REQUIREMENTS.md", "User Experience, Layouts, Design Goals & Views"),
    DEVELOPMENT_PLAN("DEVELOPMENT_PLAN.md", "Phase-by-Phase Implementation Roadmap"),
    TESTING("TESTING.md", "Testing Strategy, Unit, Integration & Security Tests"),
    DEPLOYMENT("DEPLOYMENT.md", "Deployment Architecture, Environments & CI/CD Guidance"),
    AI_BUILD_INSTRUCTIONS("AI_BUILD_INSTRUCTIONS.md", "AI Developer Guidance & Guardrails for Agentic Code Generation"),

    // Specialized / Dynamic Document Types
    MODEL_REQUIREMENTS("MODEL_REQUIREMENTS.md", "Machine Learning Model Architecture & Performance Requirements"),
    DATASET("DATASET.md", "Data Ingestion, Schema, Preprocessing & Feature Engineering"),
    ML_PIPELINE("ML_PIPELINE.md", "Machine Learning Training, Evaluation & Serving Pipeline"),
    HARDWARE_REQUIREMENTS("HARDWARE_REQUIREMENTS.md", "Embedded Hardware & System Resource Specifications"),
    SENSOR_SPECIFICATION("SENSOR_SPECIFICATION.md", "Sensor Interfaces, Polling Rates & Calibration"),
    IOT_PROTOCOLS("IOT_PROTOCOLS.md", "IoT Network Protocols, Telemetry & Edge Communication"),
    THREAT_MODEL("THREAT_MODEL.md", "Threat Modeling, Attack Surfaces & Mitigations"),
    SECURITY_TESTING("SECURITY_TESTING.md", "Penetration Testing, Fuzzing & Static Analysis Procedures"),
    MOBILE_REQUIREMENTS("MOBILE_REQUIREMENTS.md", "Mobile Platform Specifications, Offline Sync & Device APIs");

    private final String defaultFilename;
    private final String description;

    DocumentType(String defaultFilename, String description) {
        this.defaultFilename = defaultFilename;
        this.description = description;
    }

    public String getDefaultFilename() {
        return defaultFilename;
    }

    public String getDescription() {
        return description;
    }
}
