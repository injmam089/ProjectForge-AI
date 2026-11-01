# ProjectForge AI — Master Project Instructions

> **This is the root-level master file for ProjectForge AI.**
> Antigravity must read this file first whenever it starts work in this repository.
> The detailed documents in `docs/` expand the rules and specifications defined here.

---

## 1. Project Identity

**Project name:** ProjectForge AI  
**Product goal:** Turn a rough software project idea into complete, structured, reviewable project requirements, then generate a consistent package of Markdown documentation and export it as a ZIP file.

**Core product flow:**

```text
Project Idea
    ↓
AI Requirement Analysis
    ↓
Clarification Questions (when needed)
    ↓
Complete Project Requirements
    ↓
User Review and Editing
    ↓
User Approval
    ↓
Approved Project Specification (JSON)
    ↓
Relevant Markdown Documents
    ↓
Markdown Validation and Editing
    ↓
ZIP Export
```

The product must not skip the requirements review and approval stage.

## 2. The Most Important Product Rule

The approved **Project Specification JSON** is the single source of truth.

Every generated document must be derived from the same approved specification. The README, architecture, database, API, security, testing, and deployment documents must not contradict one another.

AI-generated text is not automatically trusted. Validate structured output before saving or approving it.

## 3. Required Reading Order

Before making a code change, read this file and then the relevant documents:

1. `docs/PROJECT_VISION.md`
2. `docs/PROJECT_REQUIREMENTS.md`
3. `docs/SYSTEM_ARCHITECTURE.md`
4. `docs/AI_ARCHITECTURE.md`
5. `docs/AI_INSTRUCTIONS.md`
6. `docs/PROJECT_RULES.md`
7. `docs/DEVELOPMENT_PLAN.md`
8. `docs/DATABASE_DESIGN.md` when working on persistence
9. `docs/API_SPECIFICATION.md` when working on APIs
10. `docs/SECURITY_REQUIREMENTS.md` when working on security or user data
11. `docs/DOCUMENT_GENERATION.md` when working on Markdown generation or ZIP export
12. `docs/TESTING_STRATEGY.md` when adding or changing tests

If a referenced document is missing, do not silently invent its contents. Report the missing file and propose a minimal, documented resolution.

## 4. V1 Scope

Build these capabilities in order:

- User registration and login
- Create, view, update, and delete projects
- Submit project idea and project preferences
- AI requirement analysis
- Clarification questions for critical missing information
- Structured requirements generation
- Requirements review and editing
- Requirement validation and quality feedback
- Explicit user approval
- Markdown generation based on approved requirements
- Markdown preview and editing
- AI-assisted document editing
- Safe ZIP export
- Project dashboard and history

### Not part of V1

Do not implement these unless explicitly requested in a later approved phase:

- Automatic source-code generation
- Automatic GitHub repository creation
- Autonomous deployment
- Multi-agent coding orchestration inside the product
- Vector databases or RAG infrastructure
- Custom model fine-tuning
- Team collaboration

## 5. Approved Technical Direction

Use a **modular monolith** for V1. Do not split it into microservices without a demonstrated need.

| Layer | Preferred technology |
|---|---|
| Frontend | React + Vite |
| Frontend language | JavaScript initially; use TypeScript only if explicitly selected before implementation |
| Backend | Java 17+ and Spring Boot |
| API | REST with JSON |
| Persistence | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security; BCrypt password hashing; a documented token/session strategy |
| AI provider | OpenAI API, called only from the backend |
| Markdown | Markdown generation and preview in the frontend |
| ZIP | Safe server-side ZIP generation |
| Local environment | Docker Compose |
| Version control | Git |

Do not change this stack without explaining the reason and obtaining approval.

## 6. Repository Structure

Create and maintain this structure. Add files only when they are needed by the current implementation phase.

```text
ProjectForge-AI/
├── AI.md
├── README.md
├── .gitignore
├── .env.example
├── docker-compose.yml
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── layouts/
│   │   ├── services/
│   │   ├── hooks/
│   │   ├── utils/
│   │   └── styles/
│   ├── package.json
│   └── vite.config.js
├── backend/
│   ├── src/main/java/com/projectforge/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   ├── mapper/
│   │   ├── security/
│   │   ├── ai/
│   │   ├── requirement/
│   │   ├── markdown/
│   │   ├── export/
│   │   ├── validation/
│   │   └── exception/
│   └── src/main/resources/
│       └── prompts/
└── docs/
    ├── PROJECT_VISION.md
    ├── PROJECT_REQUIREMENTS.md
    ├── SYSTEM_ARCHITECTURE.md
    ├── AI_ARCHITECTURE.md
    ├── AI_INSTRUCTIONS.md
    ├── PROJECT_RULES.md
    ├── DEVELOPMENT_PLAN.md
    ├── DATABASE_DESIGN.md
    ├── API_SPECIFICATION.md
    ├── SECURITY_REQUIREMENTS.md
    ├── DOCUMENT_GENERATION.md
    └── TESTING_STRATEGY.md
```

The structure is a target layout, not permission to generate every possible file immediately. For each phase, create only the files needed to complete and verify that phase.

## 7. AI Behavior Rules

The ProjectForge AI assistant must:

1. Understand the user's actual project goal.
2. Distinguish user-provided facts from assumptions and recommendations.
3. Ask concise clarification questions when missing information materially affects scope, architecture, data, APIs, or security.
4. Avoid asking unnecessary questions when a safe, clearly labeled assumption is sufficient.
5. Separate V1 must-have features from optional and future features.
6. Produce clear, testable, feasible requirements.
7. Detect contradictions, duplicates, and major omissions.
8. Preserve approved decisions unless the user changes them.
9. Avoid buzzwords and unjustified complexity.
10. Never claim an unimplemented feature exists.

## 8. Antigravity Engineering Rules

When acting as the implementation agent:

### Inspect first
- Inspect the repository and current Git state before editing.
- Read relevant documentation and related source files.
- Identify existing code that can be reused.
- Do not overwrite working code blindly.

### Plan and implement
- Work only on the requested phase or task.
- Break the task into verifiable implementation steps.
- Keep changes focused and maintainable.
- Do not jump ahead to later phases.
- Do not add dependencies without a clear reason.

### Verify
After implementation:
- Run the relevant build.
- Run relevant tests.
- Check application startup where applicable.
- Fix failures caused by the change.
- Inspect the diff for accidental or unrelated changes.
- State honestly what was and was not verified.

### Maintain documentation
If a change affects a requirement, architecture, API, database, security control, or development sequence, update the relevant `docs/*.md` file and this root `AI.md` only when the master-level direction itself changes.

### Do not
- Claim tests passed if they were not run.
- Hide errors or warnings that matter.
- fabricate files, commands, endpoints, or results.
- hardcode secrets or credentials.
- expose API keys in frontend code.
- execute arbitrary AI-generated commands.
- introduce microservices, Kubernetes, vector databases, or agent swarms without approval.

## 9. Security Baseline

- Hash passwords; never store plaintext passwords.
- Enforce authorization on the backend for every private project/document operation.
- Validate inputs and structured AI output.
- Keep AI provider credentials server-side and outside source control.
- Use environment variables for secrets.
- Restrict CORS to configured origins.
- Apply rate limits to sensitive and expensive endpoints.
- Avoid logging credentials, tokens, or unnecessary personal content.
- Prevent path traversal and unsafe paths during ZIP creation.
- Apply file size and request size limits.
- Return safe error messages without leaking stack traces.
- Use HTTPS in production.

See `docs/SECURITY_REQUIREMENTS.md` for the detailed security checklist.

## 10. Canonical Project Specification

The data model will evolve, but the approved specification should contain the equivalent of:

```json
{
  "project": {
    "name": "",
    "type": "",
    "goal": "",
    "problem": "",
    "difficulty": ""
  },
  "users": [],
  "roles": [],
  "features": [],
  "modules": [],
  "technology": {},
  "architecture": {},
  "database": {},
  "apis": [],
  "security": {},
  "testing": {},
  "deployment": {},
  "assumptions": [],
  "risks": []
}
```

Use schema validation and version the schema when breaking changes are introduced. Do not assume this illustrative example is the final production schema.

## 11. Project Lifecycle

Use an explicit lifecycle and enforce valid transitions:

```text
DRAFT
  → ANALYZING
  → REQUIREMENTS_READY
  → UNDER_REVIEW
  → APPROVED
  → GENERATING
  → GENERATED
  → EXPORTED
```

Failures must be recoverable. A failed analysis or document-generation attempt must not silently mark the project as approved or generated.

## 12. Development Phases

1. **Foundation:** repository, React/Vite app, Spring Boot app, PostgreSQL/Docker Compose, health check, environment example.
2. **Authentication:** registration, login, password hashing, authorization, tests.
3. **Project management:** project CRUD and ownership checks.
4. **AI requirement engine:** provider integration, structured output, clarification and validation.
5. **Requirements workspace:** review, edit, validate, approve.
6. **Markdown generation:** relevant document planner and generators.
7. **Markdown workspace:** file list, preview, edit, save, AI edit.
8. **ZIP export:** selection, safe archive creation, download.
9. **Dashboard:** project status, search, recent activity.
10. **Hardening:** integration tests, security review, error handling.
11. **Deployment:** production configuration and deployment documentation.
12. **Polish:** accessibility, responsiveness, performance, final review.

Finish and verify a phase before proceeding.

## 13. First Task: Foundation Only

The first Antigravity implementation task must establish a clean, runnable foundation.

Expected outcomes:
- Root repository layout
- React + Vite frontend scaffold
- Spring Boot backend scaffold
- PostgreSQL service in Docker Compose
- `.env.example` with placeholder variable names only
- `.gitignore`
- Basic backend health-check endpoint
- Basic frontend landing/health-status view if practical
- Clear local setup instructions in `README.md`

Do **not** implement authentication, AI integration, requirement analysis, Markdown generation, or ZIP export in Phase 1.

Before finishing, run the available frontend/backend build checks and report exact results.

## 14. Completion Report Format

At the end of each task, report:

1. **Implemented:** files and functionality added/changed.
2. **Commands run:** actual commands executed.
3. **Verification:** builds/tests and their results.
4. **Issues:** unresolved errors, warnings, or limitations.
5. **Documentation:** Markdown files updated.
6. **Next phase:** the next planned phase, without starting it automatically.

---

**Final instruction:** Treat `AI.md` as the root-level project contract. When detailed documentation provides more specific guidance, follow it unless it conflicts with this file. If a genuine conflict exists, stop, describe the conflict, and ask for a decision rather than silently choosing.
