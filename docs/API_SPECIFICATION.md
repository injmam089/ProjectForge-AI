# API Specification

## 1. Authentication Endpoints

### Register User
- **Method**: `POST`
- **Path**: `/api/auth/register`
- **Access**: Public
- **Request Body**:
  ```json
  {
    "name": "Jane Doe",
    "email": "jane@example.com",
    "password": "Password123!"
  }
  ```
- **Responses**:
  - `201 Created`: User successfully registered.
  - `400 Bad Request`: Validation failure (short password, invalid email format, etc.).
  - `409 Conflict`: Email address is already registered.

### Login User
- **Method**: `POST`
- **Path**: `/api/auth/login`
- **Access**: Public
- **Request Body**:
  ```json
  {
    "email": "jane@example.com",
    "password": "Password123!"
  }
  ```
- **Responses**:
  - `200 OK`: Returns JWT Bearer token and user profile.
    ```json
    {
      "success": true,
      "message": "Login successful",
      "token": "<jwt-token>",
      "tokenType": "Bearer",
      "user": {
        "id": 1,
        "name": "Jane Doe",
        "email": "jane@example.com",
        "role": "USER",
        "createdAt": "2026-11-01T12:00:00Z"
      }
    }
    ```
  - `401 Unauthorized`: Invalid credentials.

### Current User Profile
- **Method**: `GET`
- **Path**: `/api/auth/me`
- **Access**: Authenticated (`Authorization: Bearer <token>`)
- **Responses**:
  - `200 OK`: Returns current authenticated user profile.
  - `401 Unauthorized`: Missing or invalid JWT.

---

## 2. Project Management Endpoints (Phase 3 Implemented)

All project endpoints require `Authorization: Bearer <token>`.
**Ownership Enforcement**: Attempts to read, update, delete, or inspect projects belonging to another user strictly return `404 Not Found` to prevent IDOR resource enumeration.

### Create Project
- **Method**: `POST`
- **Path**: `/api/projects`
- **Status**: `201 Created`
- **Request Body**:
  ```json
  {
    "name": "CloudPulse Monitoring",
    "projectIdea": "Distributed observability platform for multi-cloud Kubernetes clusters",
    "problemStatement": "Engineering teams lack unified latency telemetry across hybrid clouds",
    "targetUsers": "Site Reliability Engineers, Cloud Architects",
    "projectType": "CLOUD",
    "difficulty": "ADVANCED",
    "preferredTechStack": "Java, Spring Boot, React, PostgreSQL",
    "specialRequirements": "Zero-trust network architecture and strict audit logging"
  }
  ```
- **Response**: Full `ProjectResponse` DTO with `status = "DRAFT"`.

### List User's Projects (Search, Filter, Sort, Pagination - Phase 9 Enhanced)
- **Method**: `GET`
- **Path**: `/api/projects`
- **Access**: Authenticated (`Authorization: Bearer <token>`)
- **Query Parameters (Optional)**:
  - `page`: Zero-based page index (e.g. `0`). When omitted, returns backward-compatible unpaginated `List<ProjectSummaryResponse>`.
  - `size`: Page size limit (default `12`, clamped to `1..100`).
  - `search`: Case-insensitive substring query on `name` or `description` (clamped to <=100 characters).
  - `status`: Filter by lifecycle status (`DRAFT`, `READY_FOR_ANALYSIS`, `ANALYZING`, `UNDER_REVIEW`, `REQUIREMENTS_READY`, `APPROVED`, `GENERATED`, `EXPORTED`).
  - `type`: Filter by project type (`WEB_APPLICATION`, `MOBILE_APP`, `CLI_TOOL`, `API_SERVICE`, `DATA_PIPELINE`, `DESKTOP_APP`, `OTHER`).
  - `difficulty`: Filter by difficulty level (`BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `EXPERT`).
  - `sort`: Field name to sort by (strictly validated allowlist: `updatedAt`, `createdAt`, `name`, `status`; default `updatedAt`).
  - `direction`: Sort direction (`ASC` or `DESC`; default `DESC`).
- **Responses**:
  - `200 OK` (when `page` parameter is supplied):
    ```json
    {
      "content": [
        {
          "id": 1,
          "name": "CloudPulse Monitoring",
          "description": "Distributed observability platform",
          "projectType": "WEB_APPLICATION",
          "status": "GENERATED",
          "difficulty": "ADVANCED",
          "createdAt": "2026-11-01T12:00:00Z",
          "updatedAt": "2026-11-01T12:30:00Z",
          "documentCount": 8,
          "qualityScore": 95
        }
      ],
      "page": 0,
      "size": 12,
      "totalElements": 1,
      "totalPages": 1,
      "first": true,
      "last": true
    }
    ```
  - `200 OK` (when `page` is omitted, backward-compatible): Returns array `[ { ...ProjectSummaryResponse } ]`.

### Get User Project Statistics (Phase 9)
- **Method**: `GET`
- **Path**: `/api/projects/stats`
- **Access**: Authenticated (`Authorization: Bearer <token>`)
- **Description**: Returns aggregated metrics strictly scoped to the authenticated user's workspace.
- **Response**:
  ```json
  {
    "totalProjects": 12,
    "draftProjects": 3,
    "underReviewProjects": 2,
    "approvedProjects": 1,
    "generatedProjects": 4,
    "exportedProjects": 2
  }
  ```

### Get Project Activity & History (Phase 9)
- **Method**: `GET`
- **Path**: `/api/projects/{id}/history`
- **Access**: Authenticated project owner.
- **Query Parameters**:
  - `page`: Zero-based page index (default `0`).
  - `size`: Page size limit (default `20`, clamped to `1..100`).
- **Responses**:
  - `200 OK`: Paginated list of lifecycle audit events ordered newest first (`createdAt DESC, id DESC`).
    ```json
    {
      "content": [
        {
          "id": 42,
          "projectId": 1,
          "eventType": "PROJECT_EXPORTED",
          "description": "Project documentation exported as ZIP package",
          "metadata": "{\"fileCount\": 8, \"packageSizeBytes\": 48210}",
          "createdBy": "alpha@test.com",
          "createdAt": "2026-11-02T10:15:30Z"
        }
      ],
      "page": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1,
      "first": true,
      "last": true
    }
    ```
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

### Get Project Details
- **Method**: `GET`
- **Path**: `/api/projects/{id}`
- **Responses**:
  - `200 OK`: Detailed `ProjectResponse` including owner metadata, idea, and status.
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

### Update Project
- **Method**: `PUT`
- **Path**: `/api/projects/{id}`
- **Request Body**: `UpdateProjectRequest` containing updated editable fields.
- **Responses**:
  - `200 OK`: Updated `ProjectResponse`.
  - `400 Bad Request`: Field validation failure.
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

### Delete Project
- **Method**: `DELETE`
- **Path**: `/api/projects/{id}`
- **Responses**:
  - `204 No Content`: Project successfully deleted.
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

### Prepare Project for Analysis
- **Method**: `POST`
- **Path**: `/api/projects/{id}/prepare-analysis`
- **Description**: Validates that the project meets the minimum intake requirements for analysis, and transitions the project status from `DRAFT` to `READY_FOR_ANALYSIS`. Does not invoke external AI APIs (reserved for Phase 4).
- **Responses**:
  - `200 OK`: `ProjectResponse` with `status = "READY_FOR_ANALYSIS"`.
  - `400 Bad Request`: Project is not in a valid state (`DRAFT` or `READY_FOR_ANALYSIS`) or fails minimum intake validation.
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

### Get Project Specification
- **Method**: `GET`
- **Path**: `/api/projects/{id}/specification`
- **Responses**:
  - `200 OK`: `ProjectSpecificationResponse` containing structured JSON specification.
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

### Update Project Specification
- **Method**: `PUT`
- **Path**: `/api/projects/{id}/specification`
- **Request Body**:
  ```json
  {
    "specification": {
      "schemaVersion": "1.0.0",
      "architecture": "Modular Microservices",
      "modules": ["Auth", "Telemetry", "Billing"]
    }
  }
  ```
- **Responses**:
  - `200 OK`: Updated `ProjectSpecificationResponse`.
  - `400 Bad Request`: Malformed or missing specification.
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

### Run AI Requirement Analysis (Phase 4)
- **Method**: `POST`
- **Path**: `/api/projects/{id}/analyze`
- **Description**: Triggers automated AI requirements analysis on an owned project. Transitions state to `ANALYZING` and then `REQUIREMENTS_READY` upon successful completion, validating and persisting the structured specification JSON.
- **Responses**:
  - `200 OK`: `RequirementsAnalysisResponse` containing quality score, structured specification, open questions, and status.
  - `400 Bad Request`: Invalid lifecycle state for analysis.
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).
  - `503 Service Unavailable`: OpenAI service unavailable or API key not configured.

### Get Current Requirements (Phase 4)
- **Method**: `GET`
- **Path**: `/api/projects/{id}/requirements`
- **Description**: Retrieves current structured requirements, quality score, and open clarification questions for an owned project.
- **Responses**:
  - `200 OK`: `RequirementsAnalysisResponse`.
  - `400 Bad Request`: No requirements analysis exists yet for this project.
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

### Submit Clarifications (Phase 4)
- **Method**: `POST`
- **Path**: `/api/projects/{id}/clarifications`
- **Description**: Submits domain decisions and answers to open clarification questions. The Clarification Engine integrates answers into the specification, resolves questions, and updates the quality score.
- **Request Body**:
  ```json
  {
    "answers": [
      {
        "questionId": "Q1",
        "answer": "Zstandard compression should be mandatory for all network streams."
      }
    ]
  }
  ```
- **Responses**:
  - `200 OK`: Updated `RequirementsAnalysisResponse`.
  - `400 Bad Request`: Empty or invalid answer payload, or no specification exists.
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

---

## 3. Human-in-the-Loop Review & Approval Endpoints (Phase 5 Implemented)

All review and approval endpoints require `Authorization: Bearer <token>`.
**Strict Anti-IDOR Enforcement**: Cross-user review or approval attempts strictly return `404 Not Found` with zero metadata leakage.

### Get Project Review
- **Method**: `GET`
- **Path**: `/api/projects/{id}/review`
- **Description**: Returns review details including the structured specification, server-calculated quality score, detailed dimension breakdown, list of open clarification questions, blocking questions (CRITICAL or HIGH), and approval eligibility flags (`approved`, `canApprove`).
- **Responses**:
  - `200 OK`: `ProjectReviewResponse`
  - `400 Bad Request`: No specification exists yet for this project.
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

### Human-in-the-Loop Review Update
- **Method**: `PUT`
- **Path**: `/api/projects/{id}/review`
- **Description**: Allows the project owner to edit the structured specification. The backend strictly validates the JSON schema against requirements, deterministically recalculates all 6 quality dimensions (25/15/15/15/15/15 = 100 max), evaluates remaining blocking questions, and updates the project status (`REQUIREMENTS_READY` if all critical/high questions resolved; `UNDER_REVIEW` if blocking questions remain).
- **Post-Approval Protection**: Cannot be called on projects in `APPROVED` status; throws `400 Bad Request` (`InvalidProjectStateException`).
- **Request Body**:
  ```json
  {
    "specification": { ... }
  }
  ```
- **Responses**:
  - `200 OK`: Updated `ProjectReviewResponse` with re-evaluated quality score and status.
  - `400 Bad Request`: Invalid JSON schema, missing mandatory sections, or project is in locked `APPROVED` state.
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

### Explicit Project Approval
- **Method**: `POST`
- **Path**: `/api/projects/{id}/approve`
- **Description**: Explicitly approves the project requirements specification and transitions status to `APPROVED`.
- **Mandatory Approval Rules**:
  - Approval is NEVER automatic; requires explicit confirmation (`"confirmation": true`).
  - Cannot approve if unresolved `CRITICAL` or `HIGH` open questions exist.
  - Quality score alone does NOT substitute for explicit approval.
  - Idempotent: Calling approve on an already `APPROVED` project safely returns `200 OK` without side effects.
- **Request Body**:
  ```json
  {
    "confirmation": true
  }
  ```
- **Responses**:
  - `200 OK`: `ApprovalResponse` (`status: "APPROVED"`, `approved: true`).
  - `400 Bad Request`: Missing/false confirmation, unresolved blocking questions, or project is in an ineligible state (e.g. `DRAFT`).
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

---

## 3.6 Markdown Documentation Generation (Phase 6)

### Generate Project Documentation
- **Method**: `POST`
- **Path**: `/api/projects/{id}/generate`
- **Description**: Triggers deterministic Markdown document generation strictly from the `APPROVED` Project Specification JSON (the single source of truth). Plans document set based on project type and specification, executes Java generators for all standard and specialized documents, runs cross-document consistency checks and Markdown validation, transactionally deletes old documents if re-generating, and persists documents with `version = 1`.
- **Lifecycle Preconditions**: Project must be in `APPROVED` or `GENERATED` status. Any other status (`DRAFT`, `READY_FOR_ANALYSIS`, `ANALYZING`, `UNDER_REVIEW`, `REQUIREMENTS_READY`) returns `400 Bad Request` (`InvalidProjectStateException`).
- **Post-Approval Protections**: Document generation never modifies the approved specification.
- **Responses**:
  - `200 OK`: `DocumentGenerationResponse`
    ```json
    {
      "projectId": 1,
      "status": "GENERATED",
      "totalDocuments": 14,
      "message": "Successfully generated 14 Markdown documents from approved specification.",
      "documents": [
        {
          "id": 10,
          "projectId": 1,
          "filename": "README.md",
          "type": "README",
          "description": "Project overview, quick start, architecture summary, and key links",
          "version": 1,
          "characterCount": 3520,
          "createdAt": "2026-11-01T20:00:00Z",
          "updatedAt": "2026-11-01T20:00:00Z"
        }
      ]
    }
    ```
  - `400 Bad Request`: Project is not in `APPROVED` or `GENERATED` state, or specification is missing/invalid.
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

### List Project Documents
- **Method**: `GET`
- **Path**: `/api/projects/{id}/documents`
- **Description**: Retrieves the summary list of all generated Markdown documents for the given project.
- **Responses**:
  - `200 OK`: Array of `DocumentSummaryResponse`
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

### Get Single Document Content
- **Method**: `GET`
- **Path**: `/api/documents/{id}`
- **Description**: Retrieves full document content including markdown body, character count, type, and version.
- **Responses**:
  - `200 OK`: `DocumentResponse`
    ```json
    {
      "id": 10,
      "projectId": 1,
      "filename": "README.md",
      "type": "README",
      "content": "# Project Name\n\n...",
      "version": 1,
      "characterCount": 3520,
      "createdAt": "2026-11-01T20:00:00Z",
      "updatedAt": "2026-11-01T20:00:00Z"
    }
    ```
  - `404 Not Found`: Document does not exist OR parent project belongs to another user (anti-IDOR).

---

## 3.7 Markdown Workspace & AI Document Editor (Phase 7)

### Get Project Workspace
- **Method**: `GET`
- **Path**: `/api/projects/{id}/workspace`
- **Description**: Retrieves full workspace overview for a project including project metadata, status, and all documents with current version and content.
- **Responses**:
  - `200 OK`: `WorkspaceResponse`
    ```json
    {
      "projectId": 1,
      "projectName": "CloudPulse Telemetry",
      "status": "GENERATED",
      "documents": [
        {
          "id": 10,
          "filename": "README.md",
          "type": "README",
          "version": 2,
          "characterCount": 4200,
          "content": "# CloudPulse Telemetry\n\n..."
        }
      ]
    }
    ```
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

### Save Document (Manual Edit)
- **Method**: `PUT`
- **Path**: `/api/documents/{id}`
- **Description**: Updates document content with manual edits, increments version number monotonically, records a `MANUAL_EDIT` snapshot in `DocumentVersion`, and validates markdown safety.
- **Request Body**:
  ```json
  {
    "content": "# Updated Content\n\nNew markdown text..."
  }
  ```
- **Responses**:
  - `200 OK`: `DocumentSaveResponse`
    ```json
    {
      "id": 10,
      "filename": "README.md",
      "version": 2,
      "characterCount": 3850,
      "message": "Document saved successfully as version 2"
    }
    ```
  - `400 Bad Request`: Validation failure (empty content, length < 20 or > 500,000, dangerous HTML scripts, missing Markdown heading).
  - `404 Not Found`: Document does not exist OR parent project belongs to another user (anti-IDOR).

### List Document Version History
- **Method**: `GET`
- **Path**: `/api/documents/{id}/versions`
- **Description**: Returns all historical versions of a document ordered descending by version number (metadata only, excluding full body text).
- **Responses**:
  - `200 OK`: Array of `DocumentVersionSummaryResponse`
    ```json
    [
      {
        "id": 25,
        "version": 2,
        "changeType": "MANUAL_EDIT",
        "instruction": null,
        "characterCount": 3850,
        "createdBy": "user@example.com",
        "createdAt": "2026-11-02T10:15:00Z"
      },
      {
        "id": 12,
        "version": 1,
        "changeType": "GENERATE",
        "instruction": null,
        "characterCount": 3520,
        "createdBy": "system",
        "createdAt": "2026-11-01T20:00:00Z"
      }
    ]
    ```
  - `404 Not Found`: Document does not exist OR belongs to another user (anti-IDOR).

### Get Historical Version Snapshot
- **Method**: `GET`
- **Path**: `/api/documents/{id}/versions/{version}`
- **Description**: Retrieves full content and metadata for a specific historical version snapshot.
- **Responses**:
  - `200 OK`: `DocumentVersionResponse`
    ```json
    {
      "id": 12,
      "documentId": 10,
      "version": 1,
      "content": "# CloudPulse Telemetry\n\nOriginal v1 text...",
      "changeType": "GENERATE",
      "instruction": null,
      "characterCount": 3520,
      "createdBy": "system",
      "createdAt": "2026-11-01T20:00:00Z"
    }
    ```
  - `404 Not Found`: Document or specific version does not exist, or belongs to another user (anti-IDOR).

### Restore Historical Version
- **Method**: `POST`
- **Path**: `/api/documents/{id}/restore/{version}`
- **Description**: Restores document content to match a historical version. Appends a brand-new `RESTORE` version snapshot to preserve complete audit history without rewriting past versions.
- **Responses**:
  - `200 OK`: `DocumentResponse` with updated version (e.g. `version: 4`).
  - `404 Not Found`: Document or version does not exist, or belongs to another user (anti-IDOR).

### Regenerate Document from Specification
- **Method**: `POST`
- **Path**: `/api/documents/{id}/regenerate`
- **Description**: Deterministically regenerates a single document strictly from the approved project specification JSON using the canonical generator. Increments version monotonically and records `REGENERATE` snapshot.
- **Responses**:
  - `200 OK`: `DocumentResponse`
  - `400 Bad Request`: Project is not in `APPROVED` or `GENERATED` status.
  - `404 Not Found`: Document does not exist OR parent project belongs to another user (anti-IDOR).

### AI Document Edit
- **Method**: `POST`
- **Path**: `/api/documents/{id}/ai-edit`
- **Description**: Applies an AI editing action (`IMPROVE`, `EXPAND`, `SIMPLIFY`, `CUSTOM`) to the document. The approved project specification is provided as read-only context. Strict prompt injection isolation fences protect the LLM. The approved specification is verified 100% untouched.
- **Request Body**:
  ```json
  {
    "action": "IMPROVE",
    "instruction": "Optional specific focus note..."
  }
  ```
- **Responses**:
  - `200 OK`: `DocumentAiEditResponse`
    ```json
    {
      "id": 10,
      "filename": "README.md",
      "version": 3,
      "content": "# Improved README\n\n...",
      "action": "IMPROVE",
      "message": "AI edit applied successfully as version 3"
    }
    ```
  - `400 Bad Request`: Missing action or validation failure.
  - `404 Not Found`: Document does not exist OR parent project belongs to another user (anti-IDOR).
  - `503 Service Unavailable`: External AI service failure or timeout.

---

## 3.8 ZIP Export & Package Generation (Phase 8)

### Prepare / Validate Export Package
- **Method**: `POST`
- **Path**: `/api/projects/{id}/export`
- **Description**: Validates export prerequisites (anti-IDOR project ownership, active documents existence, size and count limits, path traversal safety) and generates the package metadata including sanitized filename and document count.
- **Access**: Authenticated project owner (`ROLE_USER`, `ROLE_ADMIN`).
- **Responses**:
  - `200 OK`: `ExportResponse`
    ```json
    {
      "projectId": 1,
      "projectName": "CloudPulse Telemetry",
      "documentCount": 14,
      "filename": "ProjectForge_CloudPulse_Telemetry.zip",
      "exportedAt": "2026-11-02T12:00:00Z"
    }
    ```
  - `400 Bad Request`: Validation failure (project has 0 documents generated, or exceeds size/count limits).
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

### Download Export Archive
- **Method**: `GET`
- **Path**: `/api/projects/{id}/export/download`
- **Description**: Streams the generated ZIP archive containing all current active Markdown documents and `projectforge/EXPORT_MANIFEST.json` under `ProjectForge_<safe-name>/`. Operates strictly read-only: project status, specifications, and documents remain 100% untouched.
- **Access**: Authenticated project owner.
- **Headers**:
  - `Content-Type`: `application/zip`
  - `Content-Disposition`: `attachment; filename="ProjectForge_<safe-name>.zip"`
  - `Content-Length`: `<binary-byte-length>`
- **Responses**:
  - `200 OK`: Binary ZIP byte stream.
  - `400 Bad Request`: Validation failure (empty project).
  - `404 Not Found`: Project does not exist OR belongs to another user (anti-IDOR).

### Export Download (Direct Alias)
- **Method**: `GET`
- **Path**: `/api/projects/{id}/export`
- **Description**: Convenience alias for `GET /api/projects/{id}/export/download`. Streams the binary ZIP package.
- **Access**: Authenticated project owner.
- **Responses**:
  - `200 OK`: Binary ZIP stream.
  - `400 Bad Request`: Empty project.
  - `404 Not Found`: Anti-IDOR not found.

---

## 4. System & Health Endpoints

### System Health
- **Method**: `GET`
- **Path**: `/api/health`
- **Access**: Public
- **Response**:
  ```json
  {
    "status": "UP",
    "service": "ProjectForge AI Backend"
  }
  ```

### Spring Actuator Health
- **Method**: `GET`
- **Path**: `/actuator/health`
- **Access**: Public

---

## 5. Security & Rate Limiting

- **Authentication**: Stateless HMAC-SHA512 JWT transmitted in `Authorization: Bearer <token>` header.
- **Authorization**: Anti-IDOR tenant isolation enforced on every project, document, version, history, and export endpoint. Accessing unowned resources yields `404 Not Found`.
- **Input Sanitization**: Strict allowlist for sort parameters (`updatedAt`, `createdAt`, `name`, `status`), bounded page size (1..100), bounded search query length (<=100 chars).
- **Prompt Injection Defense**: Multi-tier boundary isolation with prompt delimiters, markdown formatting constraints, and strict JSON specification schema validation.
- **Zip Slip Defense**: Normalized archive entry paths preventing directory traversal, path escape, symlink injection, or unbounded archive expansion.
