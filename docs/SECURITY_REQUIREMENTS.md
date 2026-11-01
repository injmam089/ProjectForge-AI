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
