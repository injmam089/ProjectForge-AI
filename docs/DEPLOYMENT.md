# Production Deployment & Infrastructure Guide

This guide details the production deployment, infrastructure architecture, environment configurations, and maintenance procedures for **ProjectForge AI**.

---

## 1. Architecture Overview

ProjectForge AI operates as a containerized 3-tier architecture:

```
[ Browser / Client ]
        │
        ▼ (Port 80)
┌──────────────────────────────────────────────┐
│ projectforge-frontend (Nginx 1.25 Alpine)    │
│  - Static SPA delivery (React 18 / Vite 5)   │
│  - Reverse proxy /api/ traffic to backend   │
│  - Security headers (HSTS, CSP, X-Frame)     │
│  - Health probe at /health                   │
└──────────────────────┬───────────────────────┘
                       │ (Docker network: 8080)
                       ▼
┌──────────────────────────────────────────────┐
│ projectforge-backend (Spring Boot 3.3.4 JRE) │
│  - Multi-stage minimal Temurin JRE 17        │
│  - Non-root user execution (`appuser`)       │
│  - Stateless JWT HS-512 authentication       │
│  - Production Spring profile (`prod`)        │
│  - Health check at /api/health               │
└──────────────────────┬───────────────────────┘
                       │ (Docker network: 5432)
                       ▼
┌──────────────────────────────────────────────┐
│ projectforge-postgres (PostgreSQL 16 Alpine) │
│  - Persistent named volume (`postgres_data`) │
│  - Healthcheck via `pg_isready`              │
└──────────────────────────────────────────────┘
```

---

## 2. Prerequisites

- **Docker Engine**: version 24.0 or newer
- **Docker Compose**: version 2.20 or newer
- **Network**: Port 80 (HTTP) and optionally 8080/5432 open for local debugging

---

## 3. Environment Variables Reference

Configure these in `.env` (copied from `.env.example`):

| Variable | Description | Default | Required in Prod |
|----------|-------------|---------|------------------|
| `POSTGRES_DB` | PostgreSQL database name | `projectforge` | Recommended |
| `POSTGRES_USER` | PostgreSQL user | `projectforge_user` | Yes |
| `POSTGRES_PASSWORD` | PostgreSQL user password | - | **Yes (strong)** |
| `POSTGRES_PORT` | Host port mapped to PostgreSQL | `5432` | No |
| `BACKEND_PORT` | Host port mapped to Spring Boot backend | `8080` | No |
| `FRONTEND_PORT` | Host port mapped to Nginx web frontend | `80` | Yes |
| `JWT_SECRET` | 64+ byte HMAC-SHA512 secret string | - | **Yes (cryptographic)** |
| `JWT_EXPIRATION` | Token expiration in milliseconds | `86400000` (24h) | No |
| `OPENAI_API_KEY` | OpenAI API key for requirement analysis | - | Optional (AI features) |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed origins | `http://localhost,http://localhost:80` | Yes |

---

## 4. Quick Start: Production Deployment

### Step 1: Clone and Configure Environment
```bash
git clone <repository-url> ProjectForge-AI
cd ProjectForge-AI
cp .env.example .env
# Edit .env with your production secrets (JWT_SECRET, POSTGRES_PASSWORD, OPENAI_API_KEY)
```

### Step 2: Build and Launch Containers
```bash
docker compose build --no-cache
docker compose up -d
```

### Step 3: Verify Container Health
```bash
docker compose ps
```
All 3 containers (`projectforge-postgres`, `projectforge-backend`, `projectforge-frontend`) should display `(healthy)`.

### Step 4: Healthcheck Verification
```bash
# Frontend health
curl -f http://localhost/health

# Backend health
curl -f http://localhost:8080/api/health
```

---

## 5. Backup & Recovery

### Automated Database Backup
```bash
docker exec -t projectforge-postgres pg_dump -U projectforge_user -d projectforge -F c > backup_$(date +%Y%m%d_%H%M%S).dump
```

### Database Restoration
```bash
# Stop backend to prevent concurrent writes
docker compose stop backend

# Restore dump into PostgreSQL container
docker exec -i projectforge-postgres pg_restore -U projectforge_user -d projectforge --clean --if-exists < backup_file.dump

# Restart backend
docker compose start backend
```

---

## 6. Security Hardening Checklist

- **Non-root Containers**: The backend executes under the unprivileged `appuser:appgroup` system user.
- **Secret Isolation**: Secrets (`JWT_SECRET`, `POSTGRES_PASSWORD`, `OPENAI_API_KEY`) are passed exclusively via environment variables; never baked into Docker images.
- **Frontend Reverse Proxy**: Frontend Nginx strips server tokens and enforces `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, and `Referrer-Policy: strict-origin-when-cross-origin`.
- **Stateless Tokens**: Authentication state resides in client-held HMAC-SHA512 JWTs, preventing session state synchronization issues.

---

## 7. Troubleshooting & Logs

```bash
# View aggregated service logs
docker compose logs -f

# View backend logs specifically
docker compose logs -f backend

# View database connection errors
docker compose logs postgres

# Graceful restart
docker compose down
docker compose up -d
```
