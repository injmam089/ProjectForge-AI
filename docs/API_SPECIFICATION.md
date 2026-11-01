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

### List User's Projects
- **Method**: `GET`
- **Path**: `/api/projects`
- **Status**: `200 OK`
- **Response**: List of `ProjectSummaryResponse` objects owned by the authenticated user, ordered by `updatedAt` descending.

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

## 3. System & Health Endpoints

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

## 4. Planned Future Endpoints (Phase 4+)

### Requirements Engine (Phase 4)
- `POST /api/projects/{id}/analyze`
- `GET /api/projects/{id}/requirements`
- `PUT /api/projects/{id}/requirements`
- `POST /api/projects/{id}/requirements/validate`
- `POST /api/projects/{id}/approve`

### Documents Engine (Phase 5)
- `POST /api/projects/{id}/generate`
- `GET /api/projects/{id}/documents`
- `GET /api/documents/{id}`
- `PUT /api/documents/{id}`
- `POST /api/documents/{id}/ai-edit`

### Export Engine (Phase 6)
- `POST /api/projects/{id}/export`
