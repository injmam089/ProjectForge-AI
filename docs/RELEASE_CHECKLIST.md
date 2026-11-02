# ProjectForge AI — Release Checklist

This document serves as the authoritative verification checklist for production releases of **ProjectForge AI**.

---

## 1. Security Verification

- [x] **No Secrets Committed**: Source code, test fixtures, and git commit history scanned for API keys, passwords, and tokens.
- [x] **Cryptographic JWT Secret**: Production configuration requires minimum 64-byte (512-bit) HS-512 signing secret.
- [x] **CORS Allowlist**: Configurable allowed origins via `APP_CORS_ALLOWED_ORIGINS` / `CORS_ALLOWED_ORIGINS` without wildcard credentials.
- [x] **Security HTTP Headers**: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, and `X-XSS-Protection: 1; mode=block` configured in Spring Security and Nginx.
- [x] **XSS Defense**: Zero `dangerouslySetInnerHTML` in application codebase. Markdown rendering uses pure React element AST transformation and safe URL scheme allowlisting.
- [x] **Zip Slip & Path Traversal Protection**: Archive packaging enforces canonical path validation, rejects directory escapes (`..`), and sets archive limits.
- [x] **AI Prompt Isolation**: Backend-only API key handling, strict delimiter fencing, structured JSON parsing, and immutability of approved specifications.
- [x] **Multi-Tenant Isolation (Anti-IDOR)**: All project queries and mutations verify owner identity. Unowned resource access strictly yields `404 Not Found`.

---

## 2. Backend Verification

- [x] **Automated Tests**: Complete test suite executed via `mvn test` (209 unit and integration tests passing with 0 failures, 0 errors, 0 skipped).
- [x] **Production Profile**: `application-prod.yml` configured with PostgreSQL datasource, HikariCP connection pool, generic error responses, and disabled SQL query logging.
- [x] **Database Constraints & Schema**: Normalized entities with foreign keys, uniqueness constraints, cascading cleanups, and indexes on critical query paths (`user_id`, `project_id`, `document_id`).
- [x] **Health Check Endpoint**: Public `/api/health` and `/actuator/health` reporting application operational status.
- [x] **Error Handling**: Global exception handler transforms exceptions into standard JSON error envelopes without exposing stack traces or internal implementation details.
- [x] **Graceful Shutdown**: `spring.lifecycle.timeout-per-shutdown-phase: 30s` configured for inflight request completion.

---

## 3. Frontend Verification

- [x] **Production Build**: Vite production compilation completes with zero errors (`npm run build`).
- [x] **Responsive UI**: Verified across standard breakpoints (320px, 375px, 414px, 768px, 1024px, 1280px, 1440px, 1920px).
- [x] **Accessibility (a11y)**: Semantic HTML tags, visible focus indicators, form input `<label>` associations, modal `aria-modal` / `role="dialog"` attributes.
- [x] **Keyboard Navigation**: Escape key closes all modals, `Ctrl+S` / `Cmd+S` triggers document save, form submission on Enter.
- [x] **Loading & Empty States**: Feedback spinners on all asynchronous actions; helpful action-oriented empty states for projects, documents, history, and search queries.
- [x] **Console Cleanliness**: Zero uncaught console runtime exceptions during normal application workflows.

---

## 4. Deployment Verification

- [x] **Multi-Stage Dockerfiles**: Non-root runtime images, cached dependency layers, minimal Temurin JRE 17 and Alpine Nginx bases.
- [x] **Docker Compose Orchestration**: Services `postgres`, `backend`, and `frontend` orchestrated with health dependency conditions (`condition: service_healthy`).
- [x] **Database Persistence**: Persistent named volume `postgres_data` preserves schema and data across container restarts.
- [x] **Health Probes**: Automated curl/wget healthcheck directives configured on all services.
- [x] **Environment Separation**: Clean `.env.example` template with zero committed credentials.

---

## 5. Product Lifecycle Verification

- [x] **Authentication Flow**: User registration, login, JWT validation, expiration handling, session persistence.
- [x] **Project Management**: Project creation in `DRAFT` status, requirements intake, status transitions, safe cascade deletion.
- [x] **AI Requirements Analysis**: Quality evaluation, structured specification generation, clarification question refining.
- [x] **Human Review & Approval**: Interactive specification inspection, user confirmation, irreversible approval lock.
- [x] **Markdown Documentation Generation**: Canonical generator suite (Architecture, Requirements, Database, API, etc.) derived strictly from approved JSON specification.
- [x] **Markdown Workspace**: Dual-pane editor and preview, manual editing, AI document improvements (`IMPROVE`, `EXPAND`, `SIMPLIFY`, `CUSTOM`), single-document regeneration.
- [x] **Version History & Rollback**: Monotonic version tracking, snapshot previews, one-click version restoration.
- [x] **ZIP Export**: Client-ready documentation archive streaming with export manifest.
- [x] **Dashboard & History**: Real-time project statistics, search, multi-filter combinations, sort allowlist, server-side pagination, audit activity history.
