# AI Architecture

## 1. Requirement Intelligence Pipeline (Phase 4 Implemented)

```text
Project Intake (Name, Problem, Target Users, Type, Difficulty, Tech, Constraints)
      │
      ▼
PromptLoader (Loads externalized prompt templates from classpath)
      │
      ▼
RequirementAnalyzer (Encapsulates intake into untrusted_user_input JSON)
      │
      ▼
AIService / OpenAIService (Backend-only HTTPS call to OpenAI with timeout & rate limit handling)
      │
      ▼
Raw LLM Response JSON (cleans markdown fences if present)
      │
      ▼
RequirementValidator (Structural completeness, non-empty critical sections & fields)
      │
      ▼
QualityEvaluator (Deterministic scoring 0-100 across completeness, clarity, security, risk)
      │
      ▼
Project Entity Persistence (Stores specification JSON in PostgreSQL project_specification)
      │
      ▼
ClarificationEngine (Accepts user answers, refines specification, resolves open questions)
      │
      ▼
REQUIREMENTS_READY (Single Source of Truth locked for Phase 5+)
```

## 2. Engine Roles & Components
- **Requirement Analyzer** (`com.projectforge.ai.RequirementAnalyzer`): Converts project intake into a domain-specific structured specification containing features, modules, entities, APIs, security, testing, and open questions.
- **Clarification Engine** (`com.projectforge.ai.ClarificationEngine`): Refines an existing specification with user-provided answers to open questions, removing resolved questions and increasing quality score.
- **Requirement Validator** (`com.projectforge.ai.RequirementValidator`): Enforces that specifications meet all schema requirements, non-empty arrays, and structural invariants before persistence.
- **Quality Evaluator** (`com.projectforge.ai.QualityEvaluator`): Deterministically evaluates four dimensions (Completeness: 35, Clarity: 25, Security: 20, Risk Resolution: 20) on a 0–100 scale.
- **Prompt Loader** (`com.projectforge.ai.PromptLoader`): Loads cached prompt templates from `resources/prompts/`.
- **OpenAI Service** (`com.projectforge.ai.OpenAIService`): Server-side HTTPS client connecting to OpenAI Chat Completions API with timeouts, rate limit defense, and safe error propagation.

## 3. Externalized Prompts
Stored under `backend/src/main/resources/prompts/`:
- `requirement-analyzer.txt`: System instructions for decomposing project intake into structured specifications.
- `requirements-validator.txt`: System instructions for validating structural integrity and detecting omissions.
- `clarification-engine.txt`: System instructions for incorporating user answers to open questions.
- `quality-evaluator.txt`: System instructions for qualitative evaluation.

## 4. Prompt Injection Defense
User project descriptions, problem statements, and clarification answers are treated strictly as untrusted external data. User inputs are wrapped in dedicated `untrusted_user_input` JSON containers within the user message, strictly separated from system instructions. System prompts explicitly instruct models to never follow overrides or commands embedded within project data.

## 5. Reliability & Error Recovery
- Missing or invalid API keys fail gracefully with descriptive 503/422 responses without leaking internal keys or stack traces.
- If analysis fails midway, project status automatically reverts to `READY_FOR_ANALYSIS` so the user can safely retry.
- Corrupted or invalid JSON output is rejected by `RequirementValidator` before saving to the database.
