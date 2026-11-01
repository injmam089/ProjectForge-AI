# Engineering Rules

- Use layered backend architecture; keep controllers thin and business logic in services.
- Use DTOs and validate API input.
- Centralize exception handling.
- Keep frontend components focused and API calls in service modules.
- Include loading, empty, success, and error UI states.
- Use database migrations for schema changes.
- Enforce authorization server-side for every project/document resource.
- Hash passwords; never store plaintext credentials.
- Keep secrets in environment variables.
- Validate AI output before saving it.
- Prevent path traversal in ZIP export.
- Do not add dependencies or abstractions without a clear need.
- Inspect existing code before modifying it.
