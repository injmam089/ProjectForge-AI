# System Architecture

## 1. Architectural Style
Modular monolith for V1 to ensure strict type safety, transaction consistency, simplified testing, and low operational overhead.

---

## 2. Technology Stack
- **Frontend**: React (Vite), JavaScript, Responsive CSS, React Router, Lucide Icons
- **Backend**: Java 17+ / Spring Boot 3.3.5
- **Security**: Spring Security 6 with stateless JWT (HS512), BCrypt password hashing
- **Persistence**: PostgreSQL 16 with Spring Data JPA and Hibernate ORM
- **Containerization**: Docker Compose
- **AI Integration**: Backend-only OpenAI API integration (Planned Phase 4)

---

## 3. Layered Package Architecture

### 3.1 Backend Packages (`com.projectforge`)
- `config/`: Application, Web MVC, and CORS configurations
- `controller/`: REST controllers (`AuthController`, `ProjectController`, `ProjectSpecificationController`, `HealthController`)
- `service/`: Transactional business logic (`AuthService`, `ProjectService`, `ProjectRequirementsService`, `ProjectSpecificationService`)
- `repository/`: Spring Data JPA interfaces (`UserRepository`, `ProjectRepository`)
- `entity/`: Relational domain entities (`User`, `Project`, `Role`, `ProjectStatus`, `ProjectType`, `ProjectDifficulty`)
- `dto/`: Immutable request and response records (including `RequirementsAnalysisResponse`, `ClarificationRequest`, `OpenQuestionDto`)
- `security/`: Filter chain, JWT service, user details service, and authentication entry point
- `exception/`: Centralized exception models (`AIServiceException`, `AIValidationException`) and `GlobalExceptionHandler`
- `ai/`: AI requirement intelligence subsystem (`AIService`, `OpenAIService`, `RequirementAnalyzer`, `ClarificationEngine`, `RequirementValidator`, `QualityEvaluator`, `PromptLoader`)

### 3.2 Frontend Structure (`frontend/src/`)
- `components/`: UI building blocks (`Header`, `Footer`, `AuthModal`, `ProjectCreationCard`, `ProjectDetailsModal`, `ProjectsOverview`, `EmptyState`, `SystemStatusBadge`)
- `pages/`: Route views (`DashboardPage`, `ProjectDetailsPage`, `DocsPage`, `NotFoundPage`)
- `layouts/`: Master frame layouts (`MainLayout`)
- `services/`: API communication client layer (`api.js`, `authService.js`, `projectService.js`, `healthService.js`)
- `hooks/`: Reusable hooks (`useHealthCheck.js`)
- `utils/`: Constants, enums, and configuration
- `styles/`: Clean modular CSS variables, animations, and typography

---

## 4. Multi-Tenant Project Ownership & IDOR Protection

ProjectForge AI implements defense-in-depth isolation for all project resources:

```
Client HTTP Request
       │
       ▼
JwtAuthenticationFilter (extracts user, establishes SecurityContext)
       │
       ▼
ProjectController (extracts authenticated email from SecurityContext)
       │
       ▼
ProjectService (resolves User entity from database)
       │
       ▼
ProjectRepository.findByIdAndUserId(projectId, userId)
       │
   ┌───┴────────────────────────┐
   ▼                            ▼
Match Found             No Match / Different Owner
   │                            │
Return Project           Throw ResourceNotFoundException
                                │
                                ▼
                         GlobalExceptionHandler
                                │
                                ▼
                         HTTP 404 Not Found (Zero Leakage)
```

**Anti-Enumeration Rule**: Cross-user unauthorized access attempts return HTTP 404 (Not Found) rather than HTTP 403 (Forbidden) so attackers cannot enumerate foreign project IDs or verify resource existence.
