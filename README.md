<div align="center">

# ProjectForge AI

### Turn an idea into a complete AI-ready project blueprint.

[![Java 17](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61dafb?style=flat-square&logo=react)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-5-646cff?style=flat-square&logo=vite)](https://vitejs.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ed?style=flat-square&logo=docker)](https://docs.docker.com/compose/)
[![OpenAI](https://img.shields.io/badge/OpenAI-API-412991?style=flat-square&logo=openai)](https://platform.openai.com/)
[![JJWT](https://img.shields.io/badge/JJWT-0.12.6-lightgrey?style=flat-square)](https://github.com/jwtk/jjwt)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=flat-square&logo=apachemaven)](https://maven.apache.org/)

</div>

---

## What is ProjectForge AI?

ProjectForge AI is a structured **project planning and documentation platform** that transforms a raw software idea into a comprehensive, AI-ready project blueprint.

It is **not** a code generator, a coding assistant, or a GitHub integration. Its single responsibility is to help you think through a software idea rigorously and produce a complete set of consistent, editable Markdown documents that describe the project clearly enough for humans and AI coding agents to build from.

The output is a professionally structured ZIP package containing the full project documentation suite — ready to hand to a developer, an AI coding agent, or a team.

---

## Core Workflow

```
User submits project idea (name, type, difficulty, description)
            ↓
AI Requirement Analysis (OpenAI) — structured JSON specification
            ↓
AI-generated clarification questions → User answers
            ↓
6-Dimension Quality Score (0–100)
            ↓
Human Review & Approval (immutable lock on specification)
            ↓
Markdown Document Generation (14 core docs + specialized docs)
            ↓
Markdown Workspace — manual edits, AI edits, version history
            ↓
ZIP Export — secure downloadable package
```

---

## Single Source of Truth

```
APPROVED PROJECT SPECIFICATION (Immutable JSON — locked after approval)
                    ↓
        Canonical Java Document Generators
                    ↓
        Markdown Documents (editable workspace)
                    ↓
    [Manual Edit] [AI Edit] [Regenerate] [Restore]
                    ↓
        Version History (append-only snapshots)
                    ↓
             ZIP Export Package
```

> **Critical Invariant:** Editing, AI-modifying, regenerating, or restoring a Markdown document **never** modifies the approved project specification. The specification remains bit-for-bit immutable after approval.

---

## Features

| Feature | Description |
|---|---|
| **Project Creation** | Name, type (10 categories), difficulty (4 levels), free-text description |
| **AI Requirement Analysis** | Structured JSON specification generated via OpenAI API |
| **Clarification Engine** | AI generates targeted questions; user answers drive spec refinement |
| **6-Dimension Quality Score** | Deterministic 0–100 score across Completeness, Clarity, Consistency, Feasibility, Security, Testability |
| **Human Approval Gate** | Explicit confirmation modal with irreversible spec lock |
| **Markdown Generation** | 14 core documents generated from the approved specification |
| **Specialized Documents** | Project-type-specific docs for AI/ML, IoT, Cybersecurity, and Mobile projects |
| **Markdown Workspace** | Split-pane editor and live preview with full document switching |
| **Manual Editing** | Direct in-browser Markdown editing with save confirmation |
| **AI Document Editing** | IMPROVE / EXPAND / SIMPLIFY / CUSTOM actions with prompt injection defense |
| **Version History** | Append-only snapshots for every GENERATE, MANUAL\_EDIT, AI\_EDIT, REGENERATE, RESTORE |
| **Restore Previous Versions** | One-click restore appends a RESTORE snapshot (history preserved) |
| **ZIP Export** | In-memory ZIP of all current documents + EXPORT\_MANIFEST.json |
| **ZIP Security** | Zip-slip path traversal defense; safe filename sanitization |
| **Dashboard** | Search, filter, sort, paginate all projects; lifecycle status at a glance |
| **Project History** | Chronological audit trail of all project milestones and workspace events |
| **Project Deletion** | Safe cascade deletion with confirmation modal |
| **JWT Authentication** | Stateless HS512-signed JWTs; BCrypt password hashing |
| **Anti-IDOR Protection** | Cross-user resource access returns 404 (not 403) — zero enumeration leakage |

---

## AI Requirement Analysis

When a user submits a project, the AI pipeline runs:

1. **RequirementAnalyzer** — calls OpenAI to produce a structured JSON specification (features, modules, entities, APIs, technology stack, architecture, security, testing, deployment)
2. **RequirementValidator** — applies deterministic rule checks on the JSON before acceptance
3. **ClarificationEngine** — generates targeted open questions from specification gaps
4. **QualityEvaluator** — scores the final specification across six dimensions

### Quality Evaluation Dimensions

| Dimension | Max Points | What Is Measured |
|---|---|---|
| Completeness | 25 | Features, modules, entities, APIs coverage |
| Clarity | 15 | Goal/problem articulation, user persona definitions |
| Consistency | 15 | Tech/API alignment, entity primary keys, architecture style |
| Technical Feasibility | 15 | Tech stack completeness, component definitions, deployment targets |
| Security Coverage | 15 | Authentication, authorization, and security requirements |
| Testability | 15 | Unit, integration, and security testing coverage |
| **Total** | **100** | Open unresolved questions trigger score deductions |

### Prompt Injection Defense

- AI-generated content is wrapped in `<untrusted-document-content>` delimiters
- AI instructions are fully separated from user-supplied content in every prompt
- Output is length-checked and trimmed of superfluous wrapper fences before persistence
- Prompts are loaded from external `.txt` files (`resources/prompts/`) — not hardcoded

---

## Human Approval Workflow

```
DRAFT → UNDER_REVIEW → APPROVED → GENERATED → (workspace edits) → EXPORTED
```

- The user must explicitly confirm approval via a modal with a unique typed confirmation phrase
- Approval is **irreversible** — the specification JSON is immutably locked at the database level
- Post-approval, requirements editing endpoints return 409 Conflict
- All documents are generated from the locked specification after approval

---

## Generated Documentation

### Core Documents (14 — all project types)

| Document | Contents |
|---|---|
| `README.md` | Project overview and setup |
| `PROJECT_GOALS.md` | Vision, objectives, and success criteria |
| `PROJECT_REQUIREMENTS.md` | Functional and non-functional requirements |
| `FEATURES.md` | Feature breakdown with priority and user stories |
| `ARCHITECTURE.md` | System architecture and component design |
| `TECH_STACK.md` | Technology choices with rationale |
| `DATABASE.md` | Entity design and data relationships |
| `API_SPECIFICATION.md` | REST API contracts and endpoint definitions |
| `SECURITY.md` | Security requirements and implementation plan |
| `TESTING.md` | Testing strategy and coverage requirements |
| `DEVELOPMENT_PLAN.md` | Development phases and timeline |
| `DEPLOYMENT.md` | Deployment environments and infrastructure |
| `UI_UX_REQUIREMENTS.md` | UI/UX principles and user flow |
| `AI_BUILD_INSTRUCTIONS.md` | AI-optimized build instructions for coding agents |

### Specialized Documents (project-type-specific)

| Project Type | Additional Documents |
|---|---|
| AI / ML | MODEL\_REQUIREMENTS, DATASET, ML\_PIPELINE |
| IoT | HARDWARE\_REQUIREMENTS, SENSOR\_SPECIFICATION, IOT\_PROTOCOLS |
| Cybersecurity | THREAT\_MODEL, SECURITY\_TESTING |
| Mobile | MOBILE\_REQUIREMENTS |

---

## Markdown Workspace

The workspace provides a full in-browser editing environment for all generated documents:

- **Split-pane layout** — editor on the left, live Markdown preview on the right
- **Document sidebar** — quick switching between all project documents
- **Safe Markdown rendering** — pure React element AST (no `dangerouslySetInnerHTML`); XSS-safe
- **AI edit panel** — four AI actions (IMPROVE, EXPAND, SIMPLIFY, CUSTOM) with user instruction field
- **Version history panel** — timeline of all snapshots with diff context
- **Restore** — one-click restore of any historical version (appends a RESTORE snapshot)
- **Regenerate** — re-generate a document from the approved specification (appends a REGENERATE snapshot)

### Document Change Types

| Type | Trigger |
|---|---|
| `GENERATE` | Initial document creation after project approval |
| `MANUAL_EDIT` | User saves changes via the in-browser editor |
| `AI_EDIT` | AI editing action applied and saved |
| `REGENERATE` | Document regenerated from the locked specification |
| `RESTORE` | Historical version restored |

---

## ZIP Export

- Triggered from the dashboard or the Markdown workspace via an **Export** button
- Assembled entirely **in-memory** using standard JDK `java.util.zip` — no disk writes, no temp files
- Contents: all current active document versions + `projectforge/EXPORT_MANIFEST.json`
- **Zip-slip defense**: All entry paths are normalized and validated against the archive root before insertion
- **Safe filenames**: Entry names are sanitized via `SafeFilenameService` — alphanumeric, dots, hyphens, underscores only
- **Zero mutations**: Export never modifies any database record
- **Zero AI calls**: Packaging uses only database-persisted content — no external API dependency

### EXPORT\_MANIFEST.json

```json
{
  "product": "ProjectForge AI",
  "projectName": "YourProject",
  "projectId": 1,
  "exportVersion": "1.0",
  "generatedAt": "2026-11-02T12:00:00Z",
  "documentCount": 14,
  "documents": [{ "filename": "README.md", "version": 3 }]
}
```

---

## Dashboard & Project History

- **Search** by project name (debounced)
- **Filter** by status (DRAFT / UNDER\_REVIEW / APPROVED / GENERATED / EXPORTED) and project type
- **Sort** by last updated, created date, name, or quality score
- **Pagination** with configurable page sizes
- **Project cards** — show status badge, type, difficulty, document count, quality score, last updated
- **Quick actions** per card — open project, open workspace, export, delete
- **Project stats bar** — live counts of projects in each lifecycle state
- **Project history panel** — chronological audit trail of all significant events:

```
PROJECT_CREATED → ANALYSIS_STARTED → REQUIREMENTS_READY → UNDER_REVIEW
→ PROJECT_APPROVED → DOCUMENTS_GENERATED
→ DOCUMENT_EDITED / DOCUMENT_AI_EDITED / DOCUMENT_REGENERATED / DOCUMENT_RESTORED
→ PROJECT_EXPORTED
```

---

## Security

### Authentication & Authorization

- **JWT**: HS512-signed tokens, configurable expiration (default 24 hours), stored in `Authorization: Bearer` header
- **Passwords**: BCrypt with strength factor 10
- **Token validation**: Signature verification + expiry check on every protected request via `JwtAuthenticationFilter`
- **Anti-IDOR**: All project and document lookups use `findByIdAndUserId` — cross-user access returns HTTP 404 (not 403), preventing foreign ID enumeration

### Input Validation

- Bean Validation annotations (`@NotBlank`, `@Size`, `@Email`) on all request DTOs
- `GlobalExceptionHandler` normalizes validation errors into consistent JSON responses
- Field-level length constraints enforced at both API and database levels

### AI Security

- User requirements are never passed raw to the AI — they are embedded in structured prompt templates loaded from external files
- Document content passed to the AI edit endpoint is quarantined with `<untrusted-document-content>` delimiters
- AI output is post-processed: trimmed, length-bounded, and validated before persistence

### Markdown Security

- `SafeMarkdownRenderer.jsx` renders Markdown as a pure React element AST
- No `dangerouslySetInnerHTML` — XSS from malicious Markdown content is structurally prevented

### ZIP Security

- Zip-slip path traversal: all archive entry paths are validated against the canonical archive root
- Filenames are sanitized to `[A-Za-z0-9._-]` only via `SafeFilenameService`
- Post-generation integrity check re-reads the assembled ZIP via `ZipInputStream`

### Infrastructure

- CORS: Configurable per-environment via `APP_CORS_ALLOWED_ORIGINS` (whitespace-trimmed list)
- Non-root Docker users: `appgroup` / `appuser` (Alpine `addgroup`/`adduser`)
- Production profile (`application-prod.yml`): HikariCP connection pool (max 20, min idle 5), graceful shutdown (30 s), PostgreSQL URL externalized

---

## Architecture

```
Browser (React 18 / Vite 5)
        │  REST API (JSON)
        ▼
Spring Boot 3.3.5 (Modular Monolith)
   ├── AuthController / ProjectController
   ├── DocumentController / ExportController
   ├── ai/          — AI requirement pipeline
   ├── markdown/    — Document generation engine
   ├── export/      — ZIP package builder
   ├── security/    — JWT filter chain
   └── service/     — Transactional business logic
        │
        ├── PostgreSQL 16 (Spring Data JPA / Hibernate)
        │       projects / documents / document_versions
        │       project_history / users
        │
        └── OpenAI API (backend-only, requirement analysis phase)
```

---

## Tech Stack

### Frontend

| Technology | Version | Role |
|---|---|---|
| React | 18 | UI framework |
| Vite | 5 | Build tool and dev server |
| React Router | 6 | Client-side routing |
| Lucide React | — | Icon system |
| CSS Modules / Variables | — | Styling |

### Backend

| Technology | Version | Role |
|---|---|---|
| Java | 17 | Runtime |
| Spring Boot | 3.3.5 | Application framework |
| Spring Security | 6 | Authentication and authorization |
| Spring Data JPA | — | ORM layer |
| Hibernate | — | JPA implementation |
| JJWT | 0.12.6 | JWT signing and validation |
| Jackson | — | JSON serialization |
| Maven | 3.x | Build and dependency management |

### Database & Infrastructure

| Technology | Version | Role |
|---|---|---|
| PostgreSQL | 16 | Primary relational database |
| HikariCP | — | Connection pooling |
| Docker Compose | — | Multi-container orchestration |
| Nginx | Alpine | Frontend static file server and API proxy |

### AI

| Provider | Role |
|---|---|
| OpenAI API (configurable) | Requirement analysis, clarification, quality scoring, document editing |

---

## Project Structure

```
ProjectForge-AI/
├── backend/
│   ├── src/main/java/com/projectforge/
│   │   ├── ai/                    # AI requirement pipeline
│   │   ├── config/                # App, CORS, OpenAI, Jackson configs
│   │   ├── controller/            # REST controllers
│   │   ├── dto/                   # Request/response records
│   │   ├── entity/                # JPA entities and enums
│   │   ├── exception/             # Exception models and global handler
│   │   ├── export/                # ZIP package builder
│   │   ├── markdown/              # Document generation engine
│   │   │   └── generator/         # Core and specialized generators
│   │   ├── repository/            # Spring Data JPA interfaces
│   │   ├── requirement/           # Requirements processing pipeline
│   │   ├── security/              # JWT filter chain and services
│   │   └── service/               # Transactional business logic
│   ├── src/main/resources/
│   │   ├── application.yml        # Base config
│   │   ├── application-prod.yml   # Production overrides
│   │   └── prompts/               # AI prompt templates (4 files)
│   ├── src/test/                  # 209 tests across 33 test classes
│   ├── Dockerfile                 # Multi-stage Maven → eclipse-temurin:17-jre-alpine
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/            # UI building blocks
│   │   ├── pages/                 # Route views
│   │   ├── layouts/               # App frame layouts
│   │   ├── services/              # API client layer
│   │   ├── hooks/                 # Reusable React hooks
│   │   ├── utils/                 # Constants, enums, helpers
│   │   └── styles/                # CSS variables, animations, typography
│   ├── Dockerfile                 # Multi-stage Node → nginx:alpine
│   └── nginx.conf                 # SPA fallback, /api/ proxy, security headers
├── docs/                          # Architecture and design documentation
├── scratch/                       # Phase verification scripts (*.ps1) — not deployed
├── docker-compose.yml             # 3-service stack: postgres / backend / frontend
├── .env.example                   # Environment variable template
└── README.md
```

---

## Getting Started (Local Development)

### Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+
- Docker Desktop (for the database)
- An OpenAI API key

### Step 1 — Configure environment

```bash
cp .env.example .env
```

Edit `.env` and set:
- `POSTGRES_PASSWORD` — a secure local password
- `SPRING_DATASOURCE_PASSWORD` — same value as above
- `JWT_SECRET` — at least 64 random bytes (512 bits)
- `OPENAI_API_KEY` — your OpenAI API key

### Step 2 — Start the database

```bash
docker compose up postgres -d
```

### Step 3 — Start the backend

```bash
cd backend
mvn spring-boot:run
```

The API is available at `http://localhost:8080/api`.

### Step 4 — Start the frontend

```bash
cd frontend
npm install
npm run dev
```

The UI is available at `http://localhost:5173`.

---

## Full Docker Deployment

To run the complete 3-tier stack (PostgreSQL + Spring Boot backend + Nginx frontend):

```bash
docker compose up --build
```

| Service | Port | Description |
|---|---|---|
| Frontend (Nginx) | 80 | React SPA + API proxy |
| Backend (Spring Boot) | 8080 | REST API |
| PostgreSQL | 5432 | Database (not exposed externally in prod) |

The Nginx reverse proxy forwards `/api/` requests to the backend container — no CORS configuration is needed in full-stack Docker mode.

---

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `POSTGRES_DB` | Yes | Database name |
| `POSTGRES_USER` | Yes | Database username |
| `POSTGRES_PASSWORD` | Yes | Database password |
| `POSTGRES_PORT` | Yes | Database port (default: 5432) |
| `SPRING_DATASOURCE_URL` | Yes | JDBC connection URL |
| `SPRING_DATASOURCE_USERNAME` | Yes | DB username for Spring |
| `SPRING_DATASOURCE_PASSWORD` | Yes | DB password for Spring |
| `SPRING_PROFILES_ACTIVE` | Yes | Spring profile (`dev` / `prod`) |
| `CORS_ALLOWED_ORIGINS` | Yes | Comma-separated allowed frontend origins |
| `APP_CORS_ALLOWED_ORIGINS` | Yes | Docker Compose CORS origins override |
| `JWT_SECRET` | Yes | HS512 signing key — minimum 64 bytes |
| `JWT_EXPIRATION` | Yes | Token expiry in milliseconds (default: 86400000 = 24 h) |
| `OPENAI_API_KEY` | Yes | OpenAI API key (backend-only) |
| `OPENAI_MODEL` | No | Model name (default: `gpt-4o-mini`) |
| `OPENAI_BASE_URL` | No | OpenAI API base URL |
| `OPENAI_TIMEOUT_SECONDS` | No | Request timeout (default: 60) |
| `VITE_API_BASE_URL` | Yes (frontend) | API base URL for the Vite dev server |
| `BACKEND_PORT` | No | Backend container port mapping |
| `FRONTEND_PORT` | No | Frontend container port mapping |

See [`.env.example`](.env.example) for the full template.

---

## Testing

### Backend Tests

```bash
cd backend
mvn clean test
```

**Result: 209 passed, 0 failed, 0 errors, 0 skipped** (33 test classes)

Test coverage spans: authentication, JWT validation, project lifecycle, AI service mocking, requirement validation, document generation, ZIP export, dashboard, project history, security boundary (anti-IDOR), sorting, pagination, and exception handling.

### Frontend Build Verification

```bash
cd frontend
npm run build
```

**Result: 1612 modules bundled, 0 errors**

### Phase Verification Suite

| Phase | Suite | Result |
|---|---|---|
| Phase 6 — Document Generation | 12 checks | 12/12 PASS |
| Phase 7 — Markdown Workspace | 13 checks | 13/13 PASS |
| Phase 8 — ZIP Export | 14 checks | 14/14 PASS |
| Phase 9 — Dashboard & History | 16 checks | 16/16 PASS |
| Phase 10 — Security Hardening | 8 checks | 8/8 PASS |
| Phase 11 — Production Readiness | 20 checks | 20/20 PASS |
| Phase 12 — Final Release | 28 checks | 28/28 PASS |

---

## Production Readiness

The following have been verified:

- ✅ All 209 backend tests pass on a clean `mvn clean test`
- ✅ Frontend builds with 0 errors
- ✅ Docker Compose 3-service stack starts cleanly with health checks
- ✅ Non-root Docker users (`appgroup` / `appuser`)
- ✅ Production Spring profile with HikariCP pool and 30 s graceful shutdown
- ✅ No secrets in source control (`.env.example` uses placeholder values only)
- ✅ `JWT_SECRET` is externalized — never hardcoded
- ✅ CORS origins configurable per environment
- ✅ Zip-slip and filename sanitization verified
- ✅ Anti-IDOR ownership checks verified across all resource endpoints
- ✅ Safe Markdown rendering — no `dangerouslySetInnerHTML`

---

## Known Limitations

- **OpenAI API key required**: AI features (requirement analysis, clarification, AI document editing) require a valid OpenAI API key. The key is never exposed to the frontend.
- **Single-node PostgreSQL**: No built-in replication, read replicas, or automatic failover. Use managed database services (e.g., AWS RDS, Cloud SQL) for production HA.
- **No GitHub integration**: ProjectForge AI does not read from or write to GitHub repositories.
- **No code generation**: The platform produces Markdown documentation only — it does not generate application source code.
- **No real-time collaboration**: There is no multi-user simultaneous editing, presence indicators, or conflict resolution.
- **No billing layer**: No subscription management, usage metering, or payment processing.
- **Modular monolith**: The backend is a single deployable unit. Microservices decomposition is a future consideration.

---

## Roadmap

The following are possible future directions — not currently implemented:

- [ ] GitHub repository integration (push documentation to a repo)
- [ ] AI code scaffolding from the approved specification
- [ ] Real-time collaborative editing (WebSocket / CRDT)
- [ ] Multi-user team workspaces
- [ ] Additional AI providers (Anthropic, Gemini, local models via Ollama)
- [ ] Custom prompt template editor
- [ ] PDF export option alongside ZIP
- [ ] Usage analytics dashboard

---

## Documentation

Full architecture and design documentation is in the [`docs/`](docs/) directory.

See [`docs/README.md`](docs/README.md) for the documentation index.

---

## Screenshots

See [`docs/screenshots/`](docs/screenshots/) — contributions welcome.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup, branch naming conventions, testing requirements, and the pull request process.

---

## License

No license is currently specified for this repository.

If you intend to open-source this project, consider adding a [`LICENSE`](https://choosealicense.com/) file. Common choices:
- **MIT License** — permissive, minimal restrictions
- **Apache License 2.0** — permissive with explicit patent grant

---

## Author

Built by the ProjectForge AI team.
