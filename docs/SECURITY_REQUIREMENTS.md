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
