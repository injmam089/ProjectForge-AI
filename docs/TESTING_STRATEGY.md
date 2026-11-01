# Testing Strategy

## Backend
Unit tests for services, API/controller tests, persistence tests where useful, security/authorization tests, AI schema validation, Markdown generation tests, and ZIP path-safety tests.

## Frontend
Component tests, form validation, API integration, critical workflow tests, and responsive checks.

## Critical end-to-end flow
Register/login → create project → analyze idea → clarify → review/edit → validate → approve → generate documents → preview/edit → export ZIP.

## Security scenarios
Unauthorized project access, ownership/IDOR violations, malformed AI output, oversized requests, path traversal, secret leakage, and rate-limit behavior.

A phase is not complete if its critical workflow cannot be verified.
