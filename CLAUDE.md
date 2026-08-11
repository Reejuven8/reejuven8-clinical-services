- In all interactions and commit messages, be extremely concise and sacrifice grammar for the sake of concision.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Monorepo Layout

```
NineMo/
├── ninemo-backend/     ← Java 21 + Spring Boot 3.x microservices (Maven multi-module)
├── ninemo-mobile/      ← Kotlin Multiplatform mobile app (Android-first, Compose)
├── docs/               ← Architecture, FRD, DB design, development plan
└── archive/            ← Superseded code, reference only — never build or cite it
```

Each subdirectory has its own CLAUDE.md with project-specific commands and rules.
When working inside `ninemo-backend/` or `ninemo-mobile/`, read that subfolder's
CLAUDE.md first — it overrides or extends this file.

`archive/ninemo-frontend/` is the retired React Native app, replaced by `ninemo-mobile/`
in the native pivot. Nothing builds it. **Never use its `src/types/api.ts` as an API
contract reference** — those types were invented from prose and were wrong; see
`archive/README.md`.

---

## What This Project Is

**Reejuven8** is a cloud-native EHR platform bridging India's siloed healthcare system
to the government's ABDM (Ayushman Bharat Digital Mission) network.

**NineMo** is its first vertical — a smart maternity & childcare health locker that
guides users through 40 weeks of pregnancy and the first five years of childhood,
specifically tailored to the Indian clinical context (IAP vaccination schedule,
Indian diet safety ratings, ABHA identity).

---

## Backend — `ninemo-backend/`

### Tech Stack
- **Language**: Java 21, Spring Boot 3.x
- **AI Service**: Python 3.11 + FastAPI (one service only: `ai-parsing-service`)
- **Build**: Maven multi-module (parent POM at `ninemo-backend/pom.xml`)
- **Databases**: PostgreSQL 16, MongoDB 7, Redis 7
- **Messaging**: Apache Kafka + RabbitMQ
- **Gateway**: Spring Cloud Gateway
- **FHIR**: HAPI FHIR R4
- **ABDM Integration**: `nha-abdm-wrapper` library

### Seven Microservices

| Service | Port | DB | Purpose |
|---|---|---|---|
| `api-gateway` | 8080 | Redis | Routing, JWT validation, rate limiting |
| `identity-abha-service` | 8081 | PostgreSQL + Redis | ABHA enrollment, auth, consent, ABDM callbacks |
| `health-data-service` | 8082 | MongoDB + S3 | FHIR data lake, file vault |
| `ai-parsing-service` | 8083 | — | OCR (AWS Textract), Medical NER, LOINC mapping |
| `ninemo-clinical-service` | 8084 | PostgreSQL + MongoDB | Gestational engine, triage rules, WHO growth charts |
| `notification-service` | 8085 | PostgreSQL | WhatsApp/SMS/FCM/email dispatch |
| `ninemo-community-service` | 8086 | MongoDB | Due Date Clubs, WebSocket STOMP chat |

### Event Bus Topology

```
Kafka topics (immutable audit streams):
  abdm.consent.granted      identity-abha-service  → health-data-service
  abdm.data.received        identity-abha-service  → health-data-service
  document.data.parsed      ai-parsing-service     → ninemo-clinical-service, health-data-service

RabbitMQ queues (worker tasks with DLX retry):
  document.unstructured.uploaded   health-data-service      → ai-parsing-service
  clinical.risk.detected           ninemo-clinical-service  → notification-service
  patient.milestone.due            ninemo-clinical-service  → notification-service
```

Kafka is used for **audit streams** (immutable, replayable, high-throughput).
RabbitMQ is used for **worker queues** (competing consumers, delayed messaging, DLX retry).
Never swap these — the choice is intentional per `docs/System_Design.md`.

### Database Ownership Rules
Each service owns its tables/collections exclusively. No cross-service direct DB access.
Cross-database references use soft UUID links (no DB-level foreign keys across services).

| PostgreSQL Tables | Owner Service |
|---|---|
| `users`, `patient_profiles`, `doctor_profiles`, `addresses`, `user_consents`, `appointments` | `identity-abha-service` |
| `pregnancy_profiles`, `child_profiles`, `vaccination_records`, `medication_schedules`, `hospital_bag_items`, `diet_food_safety` | `ninemo-clinical-service` |
| `notification_logs` | `notification-service` |

| MongoDB Collections | Owner Service |
|---|---|
| `fhir_resources` | `health-data-service` |
| `ninemo_timeline_feed`, `symptom_logs`, `vitals_logs`, `kick_counter_sessions`, `contraction_sessions`, `growth_measurements`, `developmental_milestones` | `ninemo-clinical-service` |
| `due_date_clubs`, `chat_messages`, `content_articles` | `ninemo-community-service` |

### Local Infrastructure
All infrastructure runs via Docker Compose:
```
ninemo-backend/infrastructure/docker-compose.infra.yml   ← databases + brokers only
ninemo-backend/infrastructure/docker-compose.yml          ← full stack including services
```

### Shared Library
`ninemo-backend/common-lib/` is a Maven module depended on by all Java services.
It contains: `ApiResponse<T>`, `BaseEvent`, `JwtClaims`, `DateUtils`, shared exceptions.
Do not add service-specific logic here — it is a shared kernel only.

---

## Mobile — `ninemo-mobile/`

Kotlin Multiplatform, **Android-first**. Replaced the React Native app (now in
`archive/ninemo-frontend/`) in the native pivot — see `docs/Cross_Platform_Strategy.md`.

### Tech Stack
- Kotlin Multiplatform: `:shared` (common logic) + `:androidApp` (Compose UI)
- **Ktor** client with JWT auto-refresh · **kotlinx.serialization** for DTOs
- **Koin** for DI · **Krossbow** STOMP for live chat · **Coil** images · **Firebase** push
- Jetpack Compose + Material3 · Compose Navigation · `StateFlow` for state
- iOS target exists and `:shared` is written for it, but iOS is **F7 scope** and not yet built

### Module Layout

```
ninemo-mobile/
  shared/src/commonMain/kotlin/com/reejuven8/ninemo/shared/
    model/        ← @Serializable DTOs mirroring backend DTOs exactly
    network/      ← Ktor client, ApiRoutes, auth plugin — HTTP only
    repository/   ← one class per backend surface; suspend fns returning Result<T>
    viewmodel/    ← ViewModels exposing StateFlow<UiState<T>>; no UI types
    session/      ← SessionStore (encrypted token/ids), JwtDecoder
    di/           ← SharedModule.kt (Koin)
  shared/src/androidMain, shared/src/iosMain   ← expect/actual platform bits
  androidApp/src/main/java/com/reejuven8/ninemo/android/
    ui/screens/, ui/components/, ui/theme/
    navigation/   ← Routes.kt, BottomTabs.kt, NineMoNavHost.kt
```

### Core Architecture Rule: Thin Client
**The mobile app never implements medical logic.** All computation lives in the backend.
Never implement on the client: EDD calculation, gestational week, symptom triage
thresholds, WHO Z-scores or percentiles, IAP schedule generation, BMI, child age in
months, or any clinical threshold evaluation. The client renders what the server computed.

If you find yourself writing medical logic in Kotlin, stop — it belongs in
`ninemo-clinical-service`.

### Layer Rules
- `repository/` — HTTP call → typed result. No state, no UI, no transformation.
- `viewmodel/` — orchestrates repositories, exposes `StateFlow`. No Compose imports.
- `ui/screens/` — collects state, renders. No direct Ktor calls, no business logic.
- `model/` — DTOs only, no runtime behaviour.

Violations: a composable calling a repository directly; a repository holding state; a
ViewModel importing `androidx.compose.*`.

### API Contract Rule — the most expensive lesson on this project
**Read the actual Spring controller / DTO / entity source before writing any Kotlin DTO.**
When a backend field is an untyped `Map`, read the service or rule code that populates it.

Prose specs (`docs/UI_Design.md`, the archived `api.ts`) are **not** contract sources. The
KMP scaffold was seeded from them and every single mobile phase F1–F6 had to rewrite the
DTOs it inherited — IS-020, IS-022, IS-024, IS-026, IS-029, IS-030. Field names must mirror
the backend exactly; no client-side renaming.

Jackson gotcha: Lombok boolean getters serialize is-prefix-stripped (`isMember` → `member`
on the wire), so those fields need `@SerialName`.

### Kotlin Rules
- Explicit types on all public repository/ViewModel signatures
- DTO fields that the backend may omit are nullable with defaults — a missing field must
  never crash deserialization
- No swallowing errors into empty states; surface them through `UiState.Error`

### Toolchain (do not regress — see `docs/Issue_Tracker.md`)
- Gradle 9.6.1 + AGP 9.0.0; `:shared` uses `com.android.kotlin.multiplatform.library`
  with `android {}` nested inside `kotlin {}`
- `androidApp` applies `com.android.application` **only** — AGP 9 bundles Kotlin, so
  also applying `kotlin.android` fails
- Build with `JAVA_HOME=/opt/homebrew/opt/openjdk@21` (backend uses `openjdk@26`)
- Config cache disabled (AGP-9 KMP instability)
- Verify any new dependency actually exists on Maven Central before adding it (IS-019)

### Build
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
./gradlew :androidApp:assembleDebug
```

### When Unsure
- Business logic → backend, always
- Missing DTO → read the real controller, then add it to `shared/model/`
- Screen growing complex → extract a ViewModel
- ViewModel making several calls for one screen → consider a backend aggregate endpoint

---

## ABDM / Government Integration Notes

- All Aadhaar/OTP payloads sent to ABDM must be RSA-encrypted (`RSA/ECB/OAEPWithSHA-1AndMGF1Padding`)
- Inbound health data from ABDM is Curve25519-encrypted — use `nha-abdm-wrapper` to decrypt
- ABDM callbacks are asynchronous — `identity-abha-service` caches transaction IDs in Redis (5-min TTL)
- Local and staging environments use ABDM Sandbox (`https://dev.abdm.gov.in`)
- Production uses ABDM Live (`https://live.abdm.gov.in`)
- Abstract all ABDM calls behind interfaces — the spec changes; implementations must be swappable

---

## Security Rules (Applies to Both Subprojects)

- JWT access tokens: 15-minute TTL. Refresh tokens: 7-day TTL.
- RBAC roles: `PATIENT`, `DOCTOR`, `ADMIN` — enforced at the API Gateway and repeated at service level
- Doctors can only access patient data with an active, non-expired `user_consents` record
- S3 presigned URLs for medical files: 15-minute expiry
- Never log Aadhaar numbers, OTPs, or raw JWT payloads

---

## Full Design Reference

All architectural decisions, DB schemas, and the phased execution plan are in `docs/`:

| File | Contents |
|---|---|
| `docs/Development_Plan.md` | Master plan — microservice internals, patterns, phased timeline |
| `docs/Database_Design.md` | Full PostgreSQL + MongoDB schemas, indexing strategy, retention |
| `docs/System_Design.md` | Architecture overview, EDA topology, security/encryption matrix |
| `docs/NineMo_Functional_Requirement.txt` | Feature requirements across all 5 pillars |
| `docs/Technical_Requirement.txt` | Epic-level technical stories and acceptance criteria |
| `docs/UI_Design.md` | Screen-by-screen UI spec (P0–P23) — the UI reference, not a contract source |
| `docs/Cross_Platform_Strategy.md` | Why KMP, and the RN → native pivot |
| `docs/Backend_Feature_Tracker.md` | Every backend ticket + status — **update on every dev step** |
| `docs/Issue_Tracker.md` | Every issue found + resolution — **update on every dev step** |

<!-- code-review-graph MCP tools -->
## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes_tool` or `query_graph_tool` instead of Grep
- **Understanding impact**: `get_impact_radius_tool` instead of manually tracing imports
- **Code review**: `detect_changes_tool` + `get_review_context_tool` instead of reading entire files
- **Finding relationships**: `query_graph_tool` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview_tool` + `list_communities_tool`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool | Use when |
| ------ | ---------- |
| `detect_changes_tool` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context_tool` | Need source snippets for review — token-efficient |
| `get_impact_radius_tool` | Understanding blast radius of a change |
| `get_affected_flows_tool` | Finding which execution paths are impacted |
| `query_graph_tool` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes_tool` | Finding functions/classes by name or keyword |
| `get_architecture_overview_tool` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes_tool` for code review.
3. Use `get_affected_flows_tool` to understand impact.
4. Use `query_graph_tool` pattern="tests_for" to check coverage.
