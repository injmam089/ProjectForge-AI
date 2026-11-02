# Security Requirements

- Hash passwords with BCrypt or an appropriate password hashing algorithm.
- Choose and document secure token/session handling.
- Enforce server-side authentication, authorization, and project ownership.
- Keep AI credentials server-side; never put secrets in frontend bundles or Git.
- Validate and limit requests, file names, JSON, and Markdown input.
- Apply rate limiting to authentication and costly AI endpoints.
- Restrict CORS and use secure headers.
- Avoid logging secrets and unnecessary personal content.
- Protect ZIP export against absolute paths and `../` traversal.
- Use safe errors without stack traces.
- Use HTTPS in production.
- Never automatically execute AI-generated code or commands.

## Markdown Workspace & AI Document Editor Security (Phase 7)

- **Anti-IDOR Enforcement**: All document and version endpoints (`PUT /api/documents/{id}`, `GET /api/documents/{id}/versions`, `GET /api/documents/{id}/versions/{version}`, `POST /api/documents/{id}/restore/{version}`, `POST /api/documents/{id}/regenerate`, `POST /api/documents/{id}/ai-edit`, `GET /api/projects/{id}/workspace`) verify that the document's parent project is owned by the authenticated user (`document.getProject().getUser().equals(currentUser)`). Unauthorized or cross-tenant access returns HTTP `404 Not Found` with zero metadata leakage to eliminate resource enumeration vectors.
- **Project Specification Immutability**: The approved Project Specification JSON is the immutable single source of truth for the project. Manual edits, AI editing actions, and version restores operate strictly on individual Markdown document records and MUST NEVER modify or reverse-sync back to the Project Specification. Transactions verify that `project.getProjectSpecification()` remains bit-for-bit identical before commit.
- **Document Content Validation & XSS Defense**:
  - Minimum content length (20 characters) and maximum content length (500,000 characters) enforced server-side.
  - Prohibited HTML tags and dangerous event handlers (`<script`, `<iframe`, `<object`, `<embed`, `javascript:`, `onerror=`, `onload=`) rejected with HTTP 400.
  - Safe client-side rendering uses `SafeMarkdownRenderer`, parsing CommonMark into pure React elements without `dangerouslySetInnerHTML`.
- **Prompt Injection Isolation**: Untrusted document content sent to OpenAI is strictly quarantined inside delimited `<untrusted-document-content>` fences. The LLM is explicitly instructed that content inside fences must be treated as passive data and never as execution instructions.
- **Concurrency & Monotonic Versioning**: Version increments use pessimistic database row locking (`PESSIMISTIC_WRITE`) to prevent race conditions during rapid concurrent edits. Version numbers are strictly monotonic and never reset. Restores append new `RESTORE` snapshots, preserving complete tamper-evident audit history.
- **Fail-Safe AI Isolation**: If the AI provider returns an error, times out, or outputs malformed text, the transaction rolls back cleanly without altering the document or its version history.

## ZIP Export & Project Package Security (Phase 8)

- **Anti-IDOR Enforcement**: All export endpoints (`POST /api/projects/{id}/export`, `GET /api/projects/{id}/export/download`, `GET /api/projects/{id}/export`) enforce strict tenant isolation. The project must be owned by the authenticated caller (`project.getUser().equals(currentUser)`). Unauthorized or cross-tenant requests return HTTP `404 Not Found` with zero metadata leakage to eliminate resource enumeration vectors.
- **Path Traversal Defense (Zip Slip Prevention)**: 
  - Every ZIP entry path is constructed strictly beneath a sanitized base folder: `ProjectForge_<safe-name>/`.
  - Document filenames are strictly validated against `^[a-zA-Z0-9_-]+\.md$` with a maximum length of 100 characters.
  - Path traversal sequences (`..`, `../`, `..\`), slashes, backslashes, colons, null bytes, and ASCII control characters are rejected immediately.
  - Base directory and entry paths are normalized and checked to verify they remain strictly within the archive tree root.
- **Archive Name Sanitization**:
  - Raw project names are converted to safe ASCII tokens using `SafeArchiveName`: control characters, filesystem reserved characters (`<>:"/\\|?*`), and leading/trailing whitespace are stripped.
  - Names are truncated to 50 characters, and sanitized identifiers default to `Project` if all characters are stripped.
- **Resource Exhaustion & Zip Bomb Defenses**:
  - `MAX_DOCUMENT_COUNT`: 100 documents maximum per project export.
  - `MAX_SINGLE_DOCUMENT_SIZE`: 500,000 characters maximum per individual markdown file.
  - `MAX_TOTAL_EXPORT_SIZE`: 10,000,000 bytes (10MB) maximum total uncompressed archive size.
  - Empty projects (0 documents) are rejected with HTTP 400 Bad Request.
  - Export generation is performed entirely in memory using standard JDK streaming buffers (`ByteArrayOutputStream`, `ZipOutputStream`) without temporary disk writes.
- **Read-Only Immutability Guarantee**:
  - Export generation is strictly read-only and side-effect free.
  - It NEVER mutates `project.status`, `project.projectSpecification`, `document.content`, or `document_versions`.
  - The service actively asserts that `project.getProjectSpecification()` remains bit-for-bit identical before and after export assembly.
- **Secret & Credential Exclusion**:
  - Export packages contain only current active project Markdown documentation and `projectforge/EXPORT_MANIFEST.json`.
  - No environment files (`.env`, `application.properties`), API keys, user passwords, JWT tokens, database connection strings, or system logs are ever included.
- **Post-Generation Archive Integrity Verification**:
  - After in-memory ZIP package creation, the byte stream is validated with `ZipInputStream` to verify CRC32 integrity, assert entry count matches manifest metadata, and confirm manifest presence prior to sending the HTTP response.

## Dashboard & Project History Security (Phase 9)

- **Anti-IDOR & Information Leakage Prevention**:
  - All dashboard endpoints (`GET /api/projects`, `GET /api/projects/stats`, `GET /api/projects/{id}/history`, `DELETE /api/projects/{id}`) enforce strict multi-tenant isolation.
  - Queries are explicitly scoped to `p.user.id = :userId`. Foreign project access returns HTTP `404 Not Found` with zero metadata leakage (preventing enumeration of existence or metadata).
- **Sort Allowlist & SQL Injection Prevention**:
  - Sorting parameters are strictly validated against a hardcoded allowlist: `updatedAt`, `createdAt`, `name`, `status`. Any unauthorized sort field is rejected with HTTP `400 Bad Request` or defaulted to `updatedAt`, preventing dynamic SQL injection or unintended database property exposure.
- **Bounded Inputs & DoS Protection**:
  - Search queries are clamped to a maximum of 100 characters.
  - Page sizes are clamped between 1 and 100 items (default 12 or 20).
  - Negative page numbers are sanitized to 0.
- **Audit Metadata Sanitization & Storage Limits**:
  - Project history event metadata is sanitized, trimmed, and strictly bounded to <=4000 characters. Longer strings are safely truncated with an ellipsis (`...`).
  - History event types are constrained to a controlled enum (`ProjectHistoryEventType`). Arbitrary string injection into event types is impossible.
- **Safe Cascade Deletion & Audit Integrity**:
  - Cascade deletion requires explicit confirmation on the frontend (`DeleteConfirmModal`).
  - Child entities (`DocumentVersion`, `Document`, `ProjectHistory`) are deleted in proper relational dependency order within a single `@Transactional` boundary, preventing orphan records or foreign key constraint violations.
