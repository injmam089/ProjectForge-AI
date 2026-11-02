# Contributing to ProjectForge AI

Thank you for your interest in contributing. This guide covers everything you need to set up a development environment, follow the project conventions, and submit a pull request.

---

## Table of Contents

- [Development Setup](#development-setup)
- [Running Tests](#running-tests)
- [Branch Naming](#branch-naming)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Documentation Requirements](#documentation-requirements)
- [Security Considerations](#security-considerations)

---

## Development Setup

Follow the [Getting Started](README.md#getting-started-local-development) section in the main README to set up a local development environment.

Quick summary:

```bash
# 1. Copy environment config
cp .env.example .env
# Edit .env: set POSTGRES_PASSWORD, JWT_SECRET, OPENAI_API_KEY

# 2. Start the database
docker compose up postgres -d

# 3. Start the backend
cd backend
mvn spring-boot:run

# 4. Start the frontend
cd frontend
npm install
npm run dev
```

---

## Running Tests

Always run the full test suite before opening a pull request.

### Backend tests

```bash
cd backend
mvn clean test
```

Expected: **209 passed, 0 failed, 0 errors** (if you have not added new tests).

### Frontend build

```bash
cd frontend
npm run build
```

Expected: **0 build errors**.

---

## Branch Naming

Use a consistent branch prefix:

| Prefix | Use for |
|---|---|
| `feature/` | New functionality |
| `fix/` | Bug fixes |
| `docs/` | Documentation-only changes |
| `refactor/` | Code cleanup with no behavior change |
| `test/` | Test additions or improvements |
| `chore/` | Dependency updates, CI changes, tooling |

Examples:
- `feature/pdf-export`
- `fix/zip-manifest-encoding`
- `docs/update-api-specification`

---

## Commit Guidelines

- Use present-tense imperative style: `Add PDF export endpoint`, not `Added PDF export`
- Keep the subject line under 72 characters
- Reference GitHub issues where applicable: `Fix zip-slip traversal (#42)`
- Do not commit secrets, `.env` files, build artifacts (`target/`, `dist/`), or log files

---

## Pull Request Process

1. Fork the repository and create a branch from `main`
2. Make your changes
3. Ensure `mvn clean test` passes and `npm run build` succeeds
4. Update relevant documentation in `docs/` if your change affects architecture, APIs, or security
5. Open a pull request with a clear description of what changed and why
6. Reference related issues

Pull requests that fail tests, break the build, or introduce hard-coded secrets will not be merged.

---

## Documentation Requirements

If your contribution changes the public API, database schema, security model, or document generation behavior:

- Update the relevant file in `docs/` (see [`docs/README.md`](docs/README.md) for the index)
- Keep the README.md accurate (test counts, feature tables, architecture diagram)
- Do not add placeholder, aspirational, or inaccurate content to documentation — describe only what is actually implemented

---

## Security Considerations

- Never commit real API keys, passwords, or JWT secrets — use `.env.example` placeholder values only
- If you are adding a new input pathway, ensure it has Bean Validation annotations (`@NotBlank`, `@Size`, etc.)
- If you are adding a new document type, ensure filenames pass through `SafeFilenameService`
- If you are adding a new resource endpoint, ensure it scopes queries to the authenticated user's ID (anti-IDOR)
- If you discover a security vulnerability, please report it responsibly — see [SECURITY.md](SECURITY.md)

---

## Questions

Open a GitHub Issue for questions, bug reports, or feature discussions.
