# Security Policy

## Supported Versions

Security fixes are applied to the current release only.

| Version | Supported |
|---|---|
| Latest release | ✅ Yes |
| Older versions | ❌ No |

---

## Reporting a Vulnerability

If you believe you have found a security vulnerability in ProjectForge AI, please report it responsibly.

**Do not** open a public GitHub Issue for security vulnerabilities, as this may expose the issue to others before a fix is available.

### How to Report

1. Open a GitHub Issue labeled `security` with a **high-level description only** (no proof-of-concept exploit code in public)
2. Or contact the maintainer directly via the contact information in the GitHub profile

### What to Include

- A description of the vulnerability type (e.g., IDOR, injection, authentication bypass)
- Steps to reproduce (if safe to share publicly — otherwise, share via direct contact)
- Affected component (backend endpoint, frontend rendering, ZIP export, etc.)
- Your assessment of the potential impact

### Response Commitment

- We will acknowledge receipt of your report as soon as possible
- We will investigate and, if confirmed, develop a fix
- We will credit responsible disclosures in the release notes (unless you prefer anonymity)

---

## Security Design Summary

ProjectForge AI applies defense-in-depth:

- **JWT authentication**: HS512-signed tokens; validated on every protected request
- **BCrypt password hashing**: Strength factor 10; passwords never stored in plaintext
- **Anti-IDOR**: Cross-user resource access returns HTTP 404 (not 403) — prevents foreign ID enumeration
- **Input validation**: Bean Validation on all request DTOs; field-length constraints at API and database levels
- **AI prompt security**: User content is quarantined with `<untrusted-document-content>` delimiters; AI instructions are separated from user-supplied data in every prompt
- **Safe Markdown rendering**: Pure React element AST — no `dangerouslySetInnerHTML`
- **ZIP security**: Zip-slip path traversal defense; filename sanitization to `[A-Za-z0-9._-]`
- **CORS**: Configurable per environment — not open wildcard
- **Non-root containers**: Docker images run as `appuser` (not root)
- **No secrets in source control**: All sensitive values are externalized to environment variables

For the full security design, see [`docs/SECURITY_REQUIREMENTS.md`](docs/SECURITY_REQUIREMENTS.md).

---

## Out of Scope

The following are **not** in scope for security reports:

- Issues in third-party dependencies not directly exploitable via ProjectForge AI's surface area
- Denial-of-service attacks requiring control of the network or infrastructure
- Social engineering
- Physical access
