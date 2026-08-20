# Reejuven8 / NineMo

Cloud-native EHR platform bridging India's siloed healthcare system to the
government's ABDM (Ayushman Bharat Digital Mission) network.

**NineMo** — first vertical: smart maternity & childcare health locker guiding
users through 40 weeks of pregnancy + first five years of childhood, tailored
to Indian clinical context (IAP vaccination schedule, Indian diet safety
ratings, ABHA identity).

## Monorepo Layout

```
NineMo/
├── ninemo-backend/     Java 21 + Spring Boot 3.x microservices (Maven multi-module)
├── ninemo-mobile/      Kotlin Multiplatform mobile app (Android-first, Compose)
├── docs/               Architecture, FRD, DB design, development plan
└── archive/            Superseded code, reference only — never build or cite it
```

Each subdirectory has own CLAUDE.md with project-specific commands/rules. Read
that first when working inside `ninemo-backend/` or `ninemo-mobile/`.

## Backend

Java 21, Spring Boot 3.x. 7 microservices: `api-gateway`,
`identity-abha-service`, `health-data-service`, `ai-parsing-service` (Python
FastAPI), `ninemo-clinical-service`, `notification-service`,
`ninemo-community-service`. PostgreSQL 16 + MongoDB 7 + Redis 7, Kafka +
RabbitMQ, Spring Cloud Gateway, HAPI FHIR R4.

Local infra via Docker Compose:
```
ninemo-backend/infrastructure/docker-compose.infra.yml   infra only
ninemo-backend/infrastructure/docker-compose.yml          full stack
```

## Mobile

Kotlin Multiplatform, Android-first (`:shared` + `:androidApp`, Compose UI).
Thin client — no medical logic on device, all computation server-side.

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
./gradlew :androidApp:assembleDebug
```

## Docs

Full architecture, DB schema, phased plan in `docs/`. See `CLAUDE.md` for the
complete doc index and dev workflow rules.

## Issue Tracking

GitHub Issues on `Reejuven8/reejuven8-clinical-services`. Local
`docs/Backend_Feature_Tracker.md` / `docs/Issue_Tracker.md` are frozen
archives — history only, never edit.
