# ProjectForge AI

> AI-powered project requirements and documentation generator. Turn a rough software idea into complete, structured, reviewable project specifications and consistent Markdown documentation packages.

---

## 1. Start Here

- Read [`AI.md`](AI.md) first. It is the root-level master instruction and project contract.
- See the [`docs/`](docs/) directory for detailed specifications:
  - [`docs/PROJECT_VISION.md`](docs/PROJECT_VISION.md) — Product vision & target users
  - [`docs/PROJECT_REQUIREMENTS.md`](docs/PROJECT_REQUIREMENTS.md) — Functional and non-functional requirements
  - [`docs/SYSTEM_ARCHITECTURE.md`](docs/SYSTEM_ARCHITECTURE.md) — Modular monolith architecture & tech stack
  - [`docs/AI_ARCHITECTURE.md`](docs/AI_ARCHITECTURE.md) — AI pipeline & engine roles
  - [`docs/AI_INSTRUCTIONS.md`](docs/AI_INSTRUCTIONS.md) — Practical rules for AI behavior
  - [`docs/PROJECT_RULES.md`](docs/PROJECT_RULES.md) — Engineering standards & conventions
  - [`docs/DEVELOPMENT_PLAN.md`](docs/DEVELOPMENT_PLAN.md) — 12-phase roadmap
  - [`docs/DATABASE_DESIGN.md`](docs/DATABASE_DESIGN.md) — Schema design & relational model
  - [`docs/API_SPECIFICATION.md`](docs/API_SPECIFICATION.md) — REST API endpoints
  - [`docs/SECURITY_REQUIREMENTS.md`](docs/SECURITY_REQUIREMENTS.md) — Security checklist
  - [`docs/DOCUMENT_GENERATION.md`](docs/DOCUMENT_GENERATION.md) — Markdown generation & templates
  - [`docs/TESTING_STRATEGY.md`](docs/TESTING_STRATEGY.md) — Verification & testing strategy

---

## 2. Tech Stack

- **Frontend:** React 18+, Vite, JavaScript, Responsive CSS, React Router DOM, Lucide Icons
- **Backend:** Java 17+, Spring Boot 3.3.x, Spring Web, Spring Boot Actuator, Spring Data JPA, Maven
- **Database:** PostgreSQL 16 (via Docker Compose)
- **Architecture:** Modular Monolith

---

## 3. Project Structure

```text
ProjectForge-AI/
├── AI.md                     # Root-level master project contract
├── README.md                 # Project overview and setup instructions
├── .gitignore                # Root gitignore for all stacks
├── .env.example              # Environment variables template
├── docker-compose.yml        # PostgreSQL container configuration
│
├── frontend/                 # React + Vite client
│   ├── public/               # Static assets & favicons
│   ├── src/
│   │   ├── components/       # Reusable UI components
│   │   ├── pages/            # View pages (Dashboard, Docs, NotFound)
│   │   ├── layouts/          # Shell layouts (MainLayout)
│   │   ├── services/         # API clients (healthService, api)
│   │   ├── hooks/            # Custom hooks (useHealthCheck)
│   │   ├── utils/            # Constants & utilities
│   │   └── styles/           # Modern enterprise CSS system
│   ├── package.json
│   └── vite.config.js
│
├── backend/                  # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/projectforge/
│   │   │   │   ├── config/       # Security & CORS configuration
│   │   │   │   ├── controller/   # REST controllers (HealthController)
│   │   │   │   ├── service/      # Business logic services
│   │   │   │   ├── repository/   # Spring Data JPA repositories
│   │   │   │   ├── entity/       # JPA entities
│   │   │   │   ├── dto/          # Data Transfer Objects
│   │   │   │   ├── security/     # Security components
│   │   │   │   ├── ai/           # AI provider services
│   │   │   │   ├── requirement/  # Requirement analysis engine
│   │   │   │   ├── markdown/     # Document generation
│   │   │   │   ├── export/       # Safe ZIP export
│   │   │   │   └── exception/    # Global exception handling
│   │   │   └── resources/        # application.yml and prompts
│   │   └── test/                 # Unit & integration tests
│   └── pom.xml
│
└── docs/                     # Source-of-truth documentation
```

---

## 4. Windows Setup & Run Instructions

### Prerequisites

Ensure the following tools are installed on your Windows machine:

1. **Java JDK 17+** (JDK 17, 21, or 25 LTS): `java -version`
2. **Apache Maven 3.8+**: `mvn -version`
3. **Node.js 18+ & npm**: `node -v` and `npm -v`
4. **Docker Desktop**: `docker -v` and `docker compose version`

---

### Step 1: Environment Configuration

Copy the example environment configuration into `.env`:

```powershell
Copy-Item .env.example .env
```

Review `.env` and adjust database credentials or ports if desired.

---

### Step 2: Start PostgreSQL Database

Launch the PostgreSQL container using Docker Compose:

```powershell
docker compose up -d
```

Verify that PostgreSQL is healthy:

```powershell
docker compose ps
```

To stop the database:

```powershell
docker compose down
```

---

### Step 3: Run the Spring Boot Backend

Open a terminal in the `backend/` directory:

```powershell
cd backend

# Build and run tests
mvn clean test

# Run the Spring Boot application
mvn spring-boot:run
```

The backend server will start at `http://localhost:8080`.

Verify the health check endpoint:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/health"
```

Expected output:

```json
{
  "status": "UP",
  "service": "ProjectForge AI Backend"
}
```

---

### Step 4: Run the React Frontend

Open another terminal in the `frontend/` directory:

```powershell
cd frontend

# Install dependencies
npm install

# Start Vite development server
npm run dev
```

The frontend dashboard will be available at:

```text
http://localhost:5173
```

---

## 5. Development Status

- **Phase 1: Project Foundation** — COMPLETED & VERIFIED
  - Clean repository structure and configuration
  - Docker Compose PostgreSQL 16 setup
  - Spring Boot backend with health check (`/api/health`), CORS, and centralized exception handling
  - React + Vite dashboard with enterprise SaaS UI, real-time health indicator, and modular layout
  - Fully runnable and independently verified

- **Phase 2: Authentication & Security Foundation** — COMPLETED, AUDITED & VERIFIED
  - Stateless JWT authentication (HS512) with custom filter and exception handling
  - BCrypt password hashing and user role enforcement
  - Authenticated user context propagation via `@AuthenticationPrincipal`
  - React authentication modals, JWT token persistence, and automatic authorization headers

- **Phase 3: Project Management & Requirements Intake** — COMPLETED & VERIFIED
  - Full project lifecycle management (`DRAFT`, `READY_FOR_ANALYSIS`, `ANALYZING`, `REQUIREMENTS_READY`, `UNDER_REVIEW`, `APPROVED`, `GENERATING`, `GENERATED`, `EXPORTED`)
  - Strict multi-tenant ownership enforcement with defense-in-depth anti-IDOR protection (returns HTTP 404 Not Found to prevent resource enumeration)
  - Full CRUD endpoints (`/api/projects`), specification management (`/api/projects/{id}/specification`), and intake verification (`/api/projects/{id}/prepare-analysis`)
  - Comprehensive 8-field requirements intake and rich project details dashboard with status-based action guards
  - 56 unit, integration, and security tests passing with zero failures

- **Phase 4: AI Requirement Analysis & Clarification Engine** — COMPLETED & VERIFIED
  - Server-side OpenAI API integration (`OpenAIService`) with externalized prompt templates under `prompts/`
  - Automated `RequirementAnalyzer` producing comprehensive structured Project Specification JSON
  - Structural and business rule validation engine (`RequirementValidator`) preventing corrupted specifications
  - Deterministic 4-dimensional quality evaluation (0–100 score) evaluating completeness, clarity, security, and risk resolution (`QualityEvaluator`)
  - Interactive `ClarificationEngine` resolving domain unknowns from user answers and refining the specification
  - Strict multi-tenant anti-IDOR protection on all AI and requirements endpoints (`/analyze`, `/requirements`, `/clarifications`)
  - Modern React workspace featuring interactive analysis execution, quality score indicator, open question cards, and full specification tabbed views
  - 88 total automated backend unit, integration, and security tests passing with zero failures

- **Phase 5: Requirements Workspace & Human-in-the-Loop Approval** — COMPLETED & VERIFIED
  - Human-in-the-Loop review workflow with interactive question resolution
  - Structured visual specification editor and raw JSON mode
  - Explicit approval modal enforcing architectural readiness invariants
  - Strict multi-tenant anti-IDOR protection on approval endpoints

- **Phase 6: Markdown Documentation Generation** — COMPLETED & VERIFIED
  - Canonical Java document generators deriving 14+ documentation files strictly from the approved JSON specification
  - Deterministic document planning and cross-document consistency verification
  - Document viewing modal and project-level generation pipeline

- **Phase 7: Markdown Workspace & AI Document Editor** — COMPLETED & VERIFIED
  - Markdown workspace with real-time editing and side-by-side CommonMark preview
  - Strictly monotonic document versioning with append-only snapshots (`MANUAL_EDIT`, `AI_EDIT`, `REGENERATE`, `RESTORE`)
  - AI-assisted document editing (`IMPROVE`, `EXPAND`, `SIMPLIFY`, `CUSTOM`) with prompt injection isolation fences
  - Deterministic single-document regeneration and historical version restoration
  - Specification immutability: Document editing never modifies the approved specification JSON

- **Phase 8: ZIP Export & Project Package Generation** — COMPLETED & VERIFIED
  - Export package preparation and validation endpoint (`POST /api/projects/{id}/export`)
  - Streaming ZIP archive generation and download (`GET /api/projects/{id}/export/download`)
  - In-memory package assembly with sanitized directory structure (`ProjectForge_<safe-name>/`)
  - Standardized export manifest (`projectforge/EXPORT_MANIFEST.json`) excluding secrets and credentials
  - Multi-tenant anti-IDOR protection, Zip Slip path traversal defenses, and resource limits
  - Read-only immutability invariant: Export operations never mutate specifications or documents
  - Frontend ZIP export modal and download integration in Workspace and Project Details views

- **Phase 9: Dashboard & Project History** — COMPLETED & VERIFIED
  - Enhanced dashboard with search, multi-criteria filtering (status, type, difficulty), sort allowlist, and server-side pagination
  - User-scoped project analytics and statistics aggregations endpoint (`GET /api/projects/stats`)
  - Full audit logging subsystem (`ProjectHistory`) tracking 11 lifecycle milestones and workspace mutations
  - Project activity timeline modal (`ProjectHistoryModal`) with chronologically ordered events, user attribution, and expandable metadata
  - Safe enterprise cascade deletion (`DeleteConfirmModal`) removing child documents, document versions, history records, and projects within an atomic transaction
  - Strict multi-tenant anti-IDOR protection on all dashboard, stats, history, and deletion endpoints (HTTP 404 with zero leakage)
  - 209 total automated backend unit, integration, and security tests passing with zero failures
  - Production frontend build verified with zero errors (1612 modules compiled)

- **Phase 10: GitHub & VCS Repository Integration** — Planned Next
