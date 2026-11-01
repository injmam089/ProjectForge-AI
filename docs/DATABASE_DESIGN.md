# Database Design

## 1. Schema Overview

ProjectForge AI utilizes PostgreSQL 16 as its relational database. Schema management is handled through Spring Data JPA / Hibernate ORM with strict indexing, referential integrity, and cascading rules.

---

## 2. Implemented Tables

### 2.1 `users` Table
Stores authenticated user accounts.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL / BIGINT | PRIMARY KEY | Generated identifier |
| `name` | VARCHAR(100) | NOT NULL | User's full name |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | Normalized email address |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt hashed password |
| `role` | VARCHAR(20) | NOT NULL, DEFAULT 'USER' | System role (`USER`, `ADMIN`) |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | Record creation timestamp |
| `updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | Record last update timestamp |

**Indexes**:
- `users_pkey`: Primary key on `id`
- `uk_users_email`: Unique index on `email`

---

### 2.2 `projects` Table
Stores user projects and core specification intake data.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL / BIGINT | PRIMARY KEY | Generated project identifier |
| `user_id` | BIGINT | NOT NULL, FK(`users.id`) | Owning user identifier |
| `name` | VARCHAR(100) | NOT NULL | Project title |
| `project_idea` | VARCHAR(3000) | NOT NULL | Raw project concept & description |
| `problem_statement` | VARCHAR(2000) | NULLABLE | Problem being solved |
| `target_users` | VARCHAR(1000) | NULLABLE | Target audience / personas |
| `project_type` | VARCHAR(50) | NOT NULL | Controlled enum classification |
| `difficulty` | VARCHAR(30) | NOT NULL | Controlled architectural complexity |
| `preferred_tech_stack` | VARCHAR(1000) | NULLABLE | Requested technologies |
| `special_requirements` | VARCHAR(2000) | NULLABLE | Custom constraints/requirements |
| `project_specification` | TEXT (JSON) | NULLABLE | Structured JSON specification |
| `status` | VARCHAR(40) | NOT NULL, DEFAULT 'DRAFT' | Controlled lifecycle status |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | Creation timestamp |
| `updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | Update timestamp |

**Indexes**:
- `projects_pkey`: Primary key on `id`
- `idx_projects_user_id`: Index on `user_id` for fast user-specific project lookups and isolation queries
- `idx_projects_user_id_status`: Composite index on `(user_id, status)` for fast dashboard status filtering and metric aggregation
- `fk_projects_user`: Foreign key constraint to `users(id)` with `ON DELETE CASCADE`

---

## 3. Enumerations

### 3.1 `ProjectStatus`
- `DRAFT`: Initial specification intake stage
- `READY_FOR_ANALYSIS`: Project intake complete and validated, prepared for Phase 4 AI analysis
- `ANALYZING`: AI requirement clarification in progress
- `REQUIREMENTS_READY`: Clarified specification ready for user review
- `UNDER_REVIEW`: Human-in-the-loop review active
- `APPROVED`: User approved; single source of truth locked
- `GENERATING`: AI document generation executing
- `GENERATED`: Markdown documentation complete
- `EXPORTED`: Downloaded as ZIP archive

### 3.2 `ProjectType`
- `WEB_APPLICATION`
- `MOBILE_APPLICATION`
- `DESKTOP_APPLICATION`
- `API_BACKEND`
- `AI_ML_PROJECT`
- `CYBERSECURITY_PROJECT`
- `IOT_PROJECT`
- `DATA_ANALYTICS`
- `FULL_STACK_APPLICATION`
- `OTHER`

### 3.3 `ProjectDifficulty`
- `BEGINNER`
- `INTERMEDIATE`
- `ADVANCED`
- `EXPERT`

---

## 4. Planned Future Tables (Phase 5+)

### `documents` Table
- `id`: BIGSERIAL PRIMARY KEY
- `project_id`: BIGINT NOT NULL FK(`projects.id`)
- `document_type`: VARCHAR(50) NOT NULL
- `filename`: VARCHAR(255) NOT NULL
- `content`: TEXT NOT NULL
- `version`: INT NOT NULL DEFAULT 1
- `created_at`: TIMESTAMP WITH TIME ZONE
- `updated_at`: TIMESTAMP WITH TIME ZONE
