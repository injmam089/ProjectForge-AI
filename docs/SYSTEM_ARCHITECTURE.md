# System Architecture

## 1. Architectural Style
Modular monolith for V1 to ensure strict type safety, transaction consistency, simplified testing, and low operational overhead.

---

## 2. Technology Stack
- **Frontend**: React (Vite), JavaScript, Responsive CSS, React Router, Lucide Icons
- **Backend**: Java 17+ / Spring Boot 3.3.5
- **Security**: Spring Security 6 with stateless JWT (HS512), BCrypt password hashing
- **Persistence**: PostgreSQL 16 with Spring Data JPA and Hibernate ORM
- **Containerization**: Docker Compose
- **AI Integration**: Backend-only OpenAI API integration (Planned Phase 4)

---

## 3. Layered Package Architecture

### 3.1 Backend Packages (`com.projectforge`)
- `config/`: Application, Web MVC, and CORS configurations
- `controller/`: REST controllers (`AuthController`, `ProjectController`, `ProjectSpecificationController`, `HealthController`)
- `service/`: Transactional business logic (`AuthService`, `ProjectService`, `ProjectRequirementsService`, `ProjectSpecificationService`)
- `repository/`: Spring Data JPA interfaces (`UserRepository`, `ProjectRepository`)
- `entity/`: Relational domain entities (`User`, `Project`, `Role`, `ProjectStatus`, `ProjectType`, `ProjectDifficulty`)
- `dto/`: Immutable request and response records (including `RequirementsAnalysisResponse`, `ClarificationRequest`, `OpenQuestionDto`)
- `security/`: Filter chain, JWT service, user details service, and authentication entry point
- `exception/`: Centralized exception models (`AIServiceException`, `AIValidationException`) and `GlobalExceptionHandler`
- `ai/`: AI requirement intelligence subsystem (`AIService`, `OpenAIService`, `RequirementAnalyzer`, `ClarificationEngine`, `RequirementValidator`, `QualityEvaluator`, `PromptLoader`)

### 3.2 Frontend Structure (`frontend/src/`)
- `components/`: UI building blocks (`Header`, `Footer`, `AuthModal`, `ProjectCreationCard`, `ProjectDetailsModal`, `ProjectsOverview`, `EmptyState`, `SystemStatusBadge`)
- `pages/`: Route views (`DashboardPage`, `ProjectDetailsPage`, `DocsPage`, `NotFoundPage`)
- `layouts/`: Master frame layouts (`MainLayout`)
- `services/`: API communication client layer (`api.js`, `authService.js`, `projectService.js`, `healthService.js`)
- `hooks/`: Reusable hooks (`useHealthCheck.js`)
- `utils/`: Constants, enums, and configuration
- `styles/`: Clean modular CSS variables, animations, and typography

---

## 4. Multi-Tenant Project Ownership & IDOR Protection

ProjectForge AI implements defense-in-depth isolation for all project resources:

```
Client HTTP Request
       │
       ▼
JwtAuthenticationFilter (extracts user, establishes SecurityContext)
       │
       ▼
ProjectController (extracts authenticated email from SecurityContext)
       │
       ▼
ProjectService (resolves User entity from database)
       │
       ▼
ProjectRepository.findByIdAndUserId(projectId, userId)
       │
   ┌───┴────────────────────────┐
   ▼                            ▼
Match Found             No Match / Different Owner
   │                            │
Return Project           Throw ResourceNotFoundException
                                │
                                ▼
                         GlobalExceptionHandler
                                │
                                ▼
                         HTTP 404 Not Found (Zero Leakage)
```

**Anti-Enumeration Rule**: Cross-user unauthorized access attempts return HTTP 404 (Not Found) rather than HTTP 403 (Forbidden) so attackers cannot enumerate foreign project IDs or verify resource existence.

---

## 5. Markdown Workspace & Document Versioning Architecture (Phase 7)

### 5.1 Architecture Layers & Immutability Invariant

ProjectForge AI strictly separates the approved specification layer from the document workspace:

```text
APPROVED PROJECT SPECIFICATION (Immutable JSON Source of Truth)
                         ↓
             Canonical Java Generators
                         ↓
                 Markdown Documents
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                 MARKDOWN WORKSPACE                          │
│                                                             │
│  [Manual Editing]  [AI Actions]  [Regeneration]  [Restore]  │
│        ↓                ↓              ↓             ↓      │
│  MANUAL_EDIT        AI_EDIT        REGENERATE     RESTORE   │
│        ↓                ↓              ↓             ↓      │
│        └───────► DocumentVersion Snapshot ◄──────────┘      │
│                  (Strictly Monotonic Increment)             │
└─────────────────────────────────────────────────────────────┘
```

**Crucial Invariant**:
- Editing, AI-modifying, regenerating, or restoring a Markdown document **NEVER** alters or reverse-syncs to the approved `project_specification` JSON.
- The approved project specification remains bit-for-bit immutable after user approval.

### 5.2 Document Versioning Model

- `Document` entity holds the active content and current version number.
- `DocumentVersion` entity holds complete point-in-time snapshots of historical versions.
- Every state mutation records a new snapshot with an explicit `DocumentChangeType` (`GENERATE`, `MANUAL_EDIT`, `AI_EDIT`, `REGENERATE`, `RESTORE`).
- Concurrency protection: `findByIdWithLock(id)` uses database-level `PESSIMISTIC_WRITE` locking to guarantee strictly monotonic version progression without race conditions.
- Version history is append-only: Restoring a historical version appends a new `RESTORE` version snapshot rather than resetting or deleting history.

### 5.3 AI Document Editing Architecture

- Read-Only Context: The approved specification is supplied to the AI prompt as contextual background.
- Quarantine Fences: The current document content is wrapped in `<untrusted-document-content>` delimiters to eliminate prompt injection risks.
- Action-Oriented Prompts: Supports `IMPROVE`, `EXPAND`, `SIMPLIFY`, and `CUSTOM` directives.
- Validation Pipeline: Output is trimmed of superfluous wrapper fences and verified against length and safety rules before persistence.
- Complete Auditability: AI edits record the applied action, user instruction, and timestamp in the version snapshot.

## 6. ZIP Export & Project Package Generation Architecture (Phase 8)

### 6.1 Package Assembly Pipeline

```text
Approved Project Specification (Source of Truth)
            ↓
Current Active Markdown Documents (documents table)
            ↓
ExportValidator (Ownership, Count, Size, Filename, Path Traversal)
            ↓
ExportManifestBuilder (EXPORT_MANIFEST.json generation)
            ↓
ZipPackageBuilder (In-memory ZipOutputStream assembly)
            ↓
Post-Generation Integrity Check (ZipInputStream verification)
            ↓
Binary Stream -> Browser Download (Content-Type: application/zip)
```

### 6.2 Archive Directory Structure

```text
ProjectForge_<safe-project-name>/
├── README.md
├── ARCHITECTURE.md
├── SYSTEM_PATTERNS.md
├── TECH_STACK.md
├── COMPONENT_SPECS.md
├── DATABASE_DESIGN.md
├── API_SPECIFICATION.md
├── SECURITY_GUIDELINES.md
├── DEVELOPMENT_PLAN.md
├── TESTING_STRATEGY.md
├── DEPLOYMENT_GUIDE.md
├── AI_INSTRUCTIONS.md
├── PROMPT_TEMPLATES.md
├── TASK_BREAKDOWN.md
└── projectforge/
    └── EXPORT_MANIFEST.json
```

### 6.3 Export Manifest Format (`EXPORT_MANIFEST.json`)

```json
{
  "product": "ProjectForge AI",
  "projectName": "CloudPulse Telemetry",
  "projectId": 1,
  "exportVersion": "1.0",
  "generatedAt": "2026-11-02T12:00:00Z",
  "documentCount": 14,
  "documents": [
    {
      "filename": "README.md",
      "version": 2
    }
  ]
}
```

### 6.4 Read-Only Guarantees & Zero AI Dependency

- **Zero Database State Mutations**: Export generation is strictly non-mutating. No records in `projects`, `documents`, or `document_versions` are modified or created. No export history tables or disk files are stored.
- **Zero External AI Calls**: Packaging operates entirely on in-memory and database-persisted documents via standard JDK `java.util.zip` streams without external API network latency or failure modes.
- **Current Active State**: Documents exported represent the active content of each document at the time of export (`doc.getContent()`, `doc.getVersion()`), reflecting all manual and AI edits saved by the user.

---

## 7. Dashboard & Project History Architecture (Phase 9)

### 7.1 Authoritative Read-Only Dashboard & Aggregation Layer
The Phase 9 Dashboard functions strictly as a view, search, and navigation layer over authoritative backend domain models (`Project`, `Project Specification`, `Document`, `DocumentVersion`).
- **Zero Schema Duplication**: No shadow entities or cached counters exist. Document counts and quality scores are queried directly from authoritative relational stores (`DocumentRepository.countByProjectIds`, `Project.qualityScore`).
- **User-Scoped Aggregations (`/api/projects/stats`)**: Computes exact lifecycle state counts (`totalProjects`, `draftProjects`, `underReviewProjects`, `approvedProjects`, `generatedProjects`, `exportedProjects`) filtered strictly to the caller's authenticated user ID.

### 7.2 Project History Audit Subsystem (`ProjectHistory`)
Every significant project milestone and workspace mutation records a transactional audit record in the `project_history` table:

```text
Project Created ──► PROJECT_CREATED
Analysis Triggered ──► ANALYSIS_STARTED
Questions Answered ──► REQUIREMENTS_READY / UNDER_REVIEW
Review Edited ──► REVIEW_UPDATED
Project Approved ──► PROJECT_APPROVED
Docs Generated ──► DOCUMENTS_GENERATED
Workspace Edits ──► DOCUMENT_EDITED / DOCUMENT_AI_EDITED / DOCUMENT_RESTORED / DOCUMENT_REGENERATED
ZIP Exported ──► PROJECT_EXPORTED
```

#### Entity Schema (`project_history`):
- `id` (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
- `project_id` (BIGINT, FK -> `projects.id`, ON DELETE CASCADE, INDEXED)
- `event_type` (VARCHAR(50), NOT NULL, Controlled Enum `ProjectHistoryEventType`)
- `description` (VARCHAR(500), NOT NULL)
- `metadata` (VARCHAR(4000), Sanitized JSON or null)
- `created_by` (VARCHAR(100), NOT NULL, User Email or "system")
- `created_at` (TIMESTAMP WITH TIME ZONE, NOT NULL, INDEXED)
- **Composite Index**: `(project_id, created_at)` for high-performance reverse-chronological retrieval.

### 7.3 Multi-Tenant Isolation & Clean Cascade Deletion
- **Query Scoping**: Every search, filter, sort, pagination, stats, and history query includes `WHERE p.user.id = :userId`. Cross-tenant lookups strictly return `404 Not Found`.
- **Safe Cascade Deletion**: Deleting a project systematically deletes `DocumentVersion`, `Document`, `ProjectHistory`, and the `Project` entity within a single atomic `@Transactional` boundary, guaranteed by both JPA repositories and database constraints.
