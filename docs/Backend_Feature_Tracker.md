# NineMo Backend — Feature Tracker

> Jira-style tracker for all backend work. **Sequential execution** — items within an epic
> run top-to-bottom; epics run in listed order.
> Statuses: ✅ DONE · 🔄 IN PROGRESS · ⬜ TODO · ⏸ DEFERRED · 🚫 BLOCKED
> Rules: update this sheet **on every dev step**; log problems in `Issue_Tracker.md`.
>
> Last audit: 2026-07-15 (full project analysis)

---

## Progress Summary

| Epic | Tickets | Done | Progress |
|---|---|---|---|
| E1 Foundation & Shared Kernel | 4 | 4 | ✅ 100% |
| E2 Infrastructure | 4 | 4 | ✅ 100% |
| E3 API Gateway | 4 | 4 | ✅ 100% |
| E4 Identity & ABHA | 7 | 7 | ✅ 100% |
| E5 Health Data Lake | 5 | 5 | ✅ 100% |
| E6 AI Parsing Pipeline | 5 | 5 | ✅ 100% |
| E7 Clinical Engine (Antenatal) | 8 | 8 | ✅ 100% |
| E8 Pediatric Mode (Postnatal) | 5 | 5 | ✅ 100% |
| E9 Notification Service | 4 | 4 | ✅ 100% |
| E10 Community Service | 4 | 4 | ✅ 100% |
| E11 Observability | 4 | 4 | ✅ 100% |
| E12 Testing | 4 | 4 | ✅ 100% |
| E13 DevOps | 3 | 3 | ✅ 100% |
| E14 API Polish | 3 | 3 | ✅ 100% |
| E15 Gap Closure (from Communication_Patterns) | 6 | 6 | ✅ 100% |
| E16 Post-Integration Gaps | 6 | 3 | 🟡 50% |
| **Total (committed)** | **70** | **70** | **✅ 100%** |

**🎉 ALL COMMITTED TICKETS COMPLETE (2026-07-16).** Deferred/rejected: NM-B-170/171/172.

**E16 (gaps found after the committed plan closed):**
- ✅ **NM-B-167** pregnancy profile onboarding API · ✅ **NM-B-169** child profile read/update + ownership · ✅ **NM-B-174** service security + gateway hygiene — all shipped 2026-08-11, closing IS-018/027/028/034 and unblocking the first end-to-end run of the clinical + child-mode flows.
- ⬜ Remaining: **NM-B-173** (club alias), **NM-B-168** (doctor lookup), **NM-B-166** (HIP data-push to ABDM).

---

## E1 — Foundation & Shared Kernel

| ID | Title | Description / Acceptance Criteria | Status | Notes |
|---|---|---|---|---|
| NM-B-101 | Maven multi-module parent | Parent POM `com.reejuven8:ninemo-backend`, 7 modules, dependencyManagement (Spring Cloud BOM, HAPI, jjwt, BouncyCastle, Resilience4j, springdoc, MapStruct, Testcontainers) | ✅ DONE | Lombok pinned 1.18.38 (Java 26 — see IS-001) |
| NM-B-102 | common-lib DTOs | `ApiResponse<T>`, `ErrorResponse`, `PagedResponse<T>` with static factories | ✅ DONE | |
| NM-B-103 | common-lib events + exceptions | `BaseEvent` (@SuperBuilder, eventId/timestamp/correlationId/source), 3 concrete events, `BaseException` hierarchy | ✅ DONE | |
| NM-B-104 | common-lib utils | `DateUtils` (EDD strategies, gestational week, trimester), `FhirUtils` (LOINC/UCUM constants), `JwtClaims` record | ✅ DONE | |

## E2 — Infrastructure

| ID | Title | Description / AC | Status | Notes |
|---|---|---|---|---|
| NM-B-105 | docker-compose.infra.yml | Postgres 16, Mongo 7, Redis 7, ZK+Kafka, RabbitMQ, Prometheus, Zipkin, Grafana; health checks; shared network | ✅ DONE | |
| NM-B-106 | postgres-init.sql | Creates `reejuven8_identity` + `reejuven8_ninemo`; uuid-ossp + pg_trgm | ✅ DONE | |
| NM-B-107 | mongo-init.js | 11 collections + 20 indexes per Database_Design §10.2 | ✅ DONE | |
| NM-B-108 | Kafka topic bootstrap | `create-topics.sh` — 3 topics × 3 partitions | ✅ DONE | |

## E3 — API Gateway (port 8080)

| ID | Title | Description / AC | Status | Notes |
|---|---|---|---|---|
| NM-B-109 | Route table | 6 downstream routes; public paths whitelisted | ✅ DONE | |
| NM-B-110 | JwtAuthFilter | Validate HS256, Redis blacklist check, inject X-User-Id/X-User-Role | ✅ DONE | |
| NM-B-111 | Rate limiting | Redis `RequestRateLimiter` default-filter (100 rps / 150 burst) | ✅ DONE | Was already wired via default-filters (see IS-004) |
| NM-B-112 | Resilience | Circuit breaker + `FallbackController`; CorrelationIdWebFilter; RequestLoggingFilter | ✅ DONE | |

## E4 — Identity & ABHA (port 8081)

| ID | Title | Description / AC | Status | Notes |
|---|---|---|---|---|
| NM-B-113 | Flyway V1–V5 | Enums, users, patient/doctor profiles, addresses, user_consents | ✅ DONE | |
| NM-B-114 | OTP auth flow | `otp/send` → Redis 5-min TTL → `login` consumes; never log OTP | ✅ DONE | |
| NM-B-115 | JWT issue/refresh/logout | 15-min access HS256; 7-day refresh in Redis; logout blacklists jti | ✅ DONE | |
| NM-B-116 | RsaEncryptionService | `RSA/ECB/OAEPWithSHA-1AndMGF1Padding` (BouncyCastle) for Aadhaar/OTP → ABDM | ✅ DONE | |
| NM-B-117 | ABHA enrollment | enroll/otp/generate, verify, address; ABDM abstracted behind interface; sandbox URL | ✅ DONE | `/abha/scan` = Phase-2 stub |
| NM-B-118 | Consent management | grant/revoke/list; Kafka `abdm.consent.granted` on grant | ✅ DONE | |
| NM-B-119 | ABDM callbacks | `/callback/consent`, `/callback/data`; txnId ↔ Redis 5-min TTL; publishes `abdm.data.received` | ✅ DONE | |

## E5 — Health Data Lake (port 8082)

| ID | Title | Description / AC | Status | Notes |
|---|---|---|---|---|
| NM-B-120 | FhirResource Mongo model + repo | `fhir_resources` CRUD, paged by patient, filter by resourceType | ✅ DONE | |
| NM-B-121 | S3 file vault | Upload multipart → `patients/{id}/documents/{uuid}/{filename}`; presigned URL 15-min | ✅ DONE | |
| NM-B-122 | RabbitMQ publisher | `document.unstructured.uploaded` with DLX declared | ✅ DONE | Carries correlationId (NM-B-138) |
| NM-B-123 | Kafka consumers | `abdm.consent.granted`, `abdm.data.received`, `document.data.parsed` → persist FHIR | ✅ DONE | |
| NM-B-124 | Record REST API | `/health/records*` endpoints | ✅ DONE | |

## E6 — AI Parsing Pipeline (port 8083, Python)

| ID | Title | Description / AC | Status | Notes |
|---|---|---|---|---|
| NM-B-125 | FastAPI skeleton + config | Lifespan-managed consumer task; pydantic settings | ✅ DONE | |
| NM-B-126 | OCR service | AWS Textract `extract_text(s3_key)` | ✅ DONE | |
| NM-B-127 | NER service | spaCy `en_core_sci_sm` + regex hybrid; graceful regex-only fallback | ✅ DONE | |
| NM-B-128 | LOINC mapper + FHIR mapper | Keyword→LOINC w/ fuzzy (0.95/0.75 confidence); FHIR R4 Observation builder | ✅ DONE | |
| NM-B-129 | Queue in / Kafka out | aio-pika consumer (tenacity retry, nack→DLX); confluent-kafka producer `document.data.parsed` | ✅ DONE | |

## E7 — Clinical Engine, Antenatal (port 8084)

| ID | Title | Description / AC | Status | Notes |
|---|---|---|---|---|
| NM-B-130 | Flyway V1–V8 + JPA entities | 6 tables + enums + diet seed (15 ingredients) | ✅ DONE | |
| NM-B-131 | EDD strategies + TimelineService | LMP/IVF/Ultrasound strategies; week clamp [1,42]; trimester; Mongo feed w/ generated fallback | ✅ DONE | |
| NM-B-132 | Symptom triage engine | Chain-of-responsibility: Preeclampsia, Anemia, GDM, PrematureLabor, ReducedFM rules; `ninemo_symptom_triage_total` metric | ✅ DONE | |
| NM-B-133 | Vitals logging + thresholds | Normal-range map; out-of-range → risk alert | ✅ DONE | |
| NM-B-134 | Kick counter | WHO: <10 kicks in ≥120 min → CRITICAL | ✅ DONE | |
| NM-B-135 | Contraction timer | Labor pattern (≤300s interval ∧ ≥60s duration); <37w → CRITICAL | ✅ DONE | |
| NM-B-136 | Summary card | Cross-DB aggregation (profile + latest logs) | ✅ DONE | |
| NM-B-137 | Diet lookup | pg_trgm fuzzy `/diet/search?q=` | ✅ DONE | |

## E8 — Pediatric Mode, Postnatal

| ID | Title | Description / AC | Status | Notes |
|---|---|---|---|---|
| NM-B-140a | WhoLmsTable | WHO LMS 0–60m both sexes; interpolation | ✅ DONE | |
| NM-B-140b | GrowthChartService | Z-scores (WAZ/HAZ/HCZ), percentiles, channel-crossing alerts | ✅ DONE | |
| NM-B-140c | VaccinationScheduleService | Idempotent IAP 2023 — 37 doses; markCompleted | ✅ DONE | |
| NM-B-140d | ModeTransitionService | Deactivate pregnancy → create child → IAP schedule → week-1 reminder | ✅ DONE | |
| NM-B-140e | DevelopmentalMilestoneService | WHO checklists (m 2–60); <50% → DELAY_RISK + alert; controller | ✅ DONE | Model uses `List<Map<String,Object>>` (see IS-005) |

## E9 — Notification Service (port 8085)

| ID | Title | Description / AC | Status | Notes |
|---|---|---|---|---|
| NM-B-141 | RabbitMQ topology + listeners | `clinical.risk.detected`, `patient.milestone.due` w/ DLX | ✅ DONE | |
| NM-B-142 | Channel services | Twilio WhatsApp/SMS, FCM push | ✅ DONE | |
| NM-B-143 | Orchestrator + NotificationLog | Route by channel; persist status | ✅ DONE | `entry` var — Lombok @Slf4j collision (IS-003) |
| NM-B-144 | Flyway V1 | notification_logs | ✅ DONE | |

## E10 — Community Service (port 8086)

| ID | Title | Description / AC | Status | Notes |
|---|---|---|---|---|
| NM-B-145a | Due Date Clubs | Find-or-create by month; 3 default channels; idempotent join | ✅ DONE | |
| NM-B-145b | STOMP chat | `/ws/connect`; send → persist → broadcast `/topic/club.{c}.{ch}` | ✅ DONE | CONNECT not JWT-validated → NM-B-161 |
| NM-B-145c | Chat REST | Paged history; soft-delete w/ ownership | ✅ DONE | |
| NM-B-145d | Content articles | By category / gestational week / published | ✅ DONE | |

## E11 — Observability

| ID | Title | Description / AC | Status | Notes |
|---|---|---|---|---|
| NM-B-146 | Structured logging | logback-spring per service; JSON (Logstash) in prod | ✅ DONE | |
| NM-B-147 | Correlation ID filters | MVC `CorrelationIdFilter` ×5 + reactive WebFilter (gateway) | ✅ DONE | |
| NM-B-148 | Prometheus + custom metrics | `/actuator/prometheus` all services; triage counter | ✅ DONE | |
| NM-B-149 | Tracing | OTel bridge → Zipkin; 100% sampling dev | ✅ DONE | |

## E12 — Testing

| ID | Title | Description / AC | Status | Notes |
|---|---|---|---|---|
| NM-B-150 | Clinical unit tests | 32 tests: Timeline, WhoLms, KickCounter, Contraction | ✅ DONE | |
| NM-B-151 | Identity unit tests | JwtTokenProvider (6), RsaEncryptionService (4) | ✅ DONE | ReflectionTestUtils for @Value fields |
| NM-B-152 | Python pytest | NER (6), LOINC (6), FHIR mapper (5) + health | ✅ DONE | |
| NM-B-153 | Integration tests (Testcontainers) | health-data (Mongo+Rabbit), clinical (Flyway+Mongo), identity (PG+Redis OTP) | ✅ DONE | 2026-07-16. 3 test classes, 7 tests, `disabledWithoutDocker=true` (skip cleanly sans Docker; run in CI). All pass vs real containers. **Caught 2 real bugs:** IS-016 (PG enum inserts broken in 11 entities — registration/onboarding would fail in prod; fixed w/ `@JdbcTypeCode(NAMED_ENUM)` ×12 fields) + IS-015 (both identity unit test files never compiled). Testcontainers 1.20.1→1.21.3; OrbStack min-api-version note (IS-017). |

## E13 — DevOps

| ID | Title | Description / AC | Status | Notes |
|---|---|---|---|---|
| NM-B-154 | GitHub Actions CI | backend-test, python-test, 6+1 image builds (main only) | ✅ DONE | |
| NM-B-155 | Kubernetes manifests | ns, configmap, secrets template, ingress, 8× Deploy+Svc+HPA | ✅ DONE | secrets.yaml has REPLACE_ME placeholders |
| NM-B-156 | Docker Compose full stack | docker-compose.yml incl. services | ✅ DONE | |

## E14 — API Polish

| ID | Title | Description / AC | Status | Notes |
|---|---|---|---|---|
| NM-B-157 | Correlation ID in events | All 5 publishers stamp MDC correlationId | ✅ DONE | Consumer-side restore → NM-B-163 |
| NM-B-158 | Swagger @Operation | All 20 REST controllers annotated | ✅ DONE | |
| NM-B-159 | Architecture docs current | Backend_Architecture §19/§20 + endpoint corrections | ✅ DONE | 2026-07-15 |

## E15 — Gap Closure (sequential order of execution)

| ID | Title | Description / Acceptance Criteria | Status | Notes |
|---|---|---|---|---|
| NM-B-160 | SSE parse-progress stream | `GET /api/v1/health/files/events?s3Key=` via `SseEmitter`; emits PROCESSING on subscribe, PARSED when `document.data.parsed` arrives; late subscribers of completed docs get PARSED immediately (30-min retention) | ✅ DONE | 2026-07-15. `ParseProgressService` + hook in `ParsedDocumentListener`. Query param not path var — s3Key contains slashes (see IS-011). Compiled clean. |
| NM-B-161 | STOMP JWT on CONNECT | `ChannelInterceptor` in community-service; reject CONNECT without valid Bearer; sender identity from JWT not payload | ✅ DONE | 2026-07-15. `StompAuthChannelInterceptor` (CONNECT auth + SEND/SUBSCRIBE require principal), `JwtValidator` (jjwt, shared `jwt.secret`), `SendMessageRequest.senderId` removed — `ChatController` uses `Principal`. Compiled clean. Mobile clients must send `Authorization` native header on CONNECT. |
| NM-B-162 | FCM device-token registration | `POST /api/v1/notifications/devices` — userId, token, platform ANDROID/IOS; upsert; used by PushNotificationService | ✅ DONE | 2026-07-15. Flyway `V2__create_device_tokens.sql`, `DeviceToken` entity, upsert-by-token service (re-login re-binds owner), `DELETE /devices?fcmToken=` for logout, orchestrator PUSH fans out to all user devices w/ legacy fcmToken fallback. Compiled clean. |
| NM-B-163 | Consumer-side MDC restore | All Kafka/Rabbit listeners read correlationId from payload → MDC before processing | ✅ DONE | 2026-07-16. `CorrelationMdc` (common-lib, try-with-resources, UUID fallback) applied to all 6 Java listeners. Python was silently dropping the id (IS-012): pipeline now binds it to structlog + `DocumentParsedEvent.correlationId` populated. Full chain upload→OCR→parse→persist→notify now traceable. |
| NM-B-164 | Twilio delivery webhook | `POST /api/v1/notifications/callbacks/twilio` → NotificationLog SENT→DELIVERED; idempotent | ✅ DONE | 2026-07-16. `TwilioCallbackService` (monotonic: delivered/read→DELIVERED+deliveredAt, failed/undelivered→FAILED+reason, intermediate ignored; terminal states never regress). X-Twilio-Signature verified via `RequestValidator` when `twilio.status-callback-url` set. `setStatusCallback()` wired into WhatsApp+SMS sends. Gateway public path `/api/v1/notifications/callbacks/` added. Compiled clean. |
| NM-B-165 | Envelope normalization | Timeline, SummaryCard, Diet return `ApiResponse<T>`; coordinate mobile DTO change | ✅ DONE | 2026-07-16. Scope grew: wrapped **8** clinical controllers (Timeline, SummaryCard, Diet, Vitals, Growth, Vaccination, Milestone, ModeTransition) w/ concrete generics — `<?>` returns eliminated. Also fixed IS-011 latent bug: `GET /health/files/{s3Key}/download` → `/health/files/download?s3Key=`. **No mobile change needed** — RN services already unwrapped `ApiResponse<T>` (`res.data.data`); backend was the mismatched side (IS-014). 32/32 tests green. |

## E16 — Post-Integration Gaps & Future Scope

| ID | Title | Description / Acceptance Criteria | Status | Notes |
|---|---|---|---|---|
| NM-B-167 | Pregnancy profile onboarding API | `POST /api/v1/ninemo/profiles/pregnancy` (X-User-Id; LMP/ultrasound/IVF date → EDD via existing strategies; height/weight/blood group → baseline BMI; risk flags JSONB) + `GET` current profile. **Blocker for first-run UX** — all clinical features need an active profile; today no endpoint exists (IS-018) | ✅ DONE | 2026-08-11. `PregnancyProfileController` + `PregnancyProfileService`. `POST /api/v1/ninemo/profiles/pregnancy` (201): exactly one dating basis (lmpDate / ultrasoundDate+GA / ivfTransferDate), method inferred when omitted and rejected when it contradicts the date; EDD via the existing strategies; **`UltrasoundCalculationStrategy` was a throwing stub — implemented as `scanDate + (280 - gestationalAgeAtScan)`**; baseline BMI = kg/m² at DECIMAL(4,1); risk flags to JSONB; 409 on a second active profile; implausible EDDs rejected 400. `GET` returns the active profile with server-computed gestationalWeek/trimester, 404 when not onboarded — which is what lets the app check onboarding state across reinstalls. Also swapped the six `IllegalStateException("No active pregnancy profile")` throws for `ResourceNotFoundException` so those are 404 not 500. 12 unit tests + 1 Testcontainers test (covers the JSONB column and the native PG enum). Verified live end-to-end: onboarding → `GET /timeline/current` 200, the first time that chain has ever worked. Mobile `CreatePregnancyProfileRequest`/`PregnancyProfileResponse` rewritten to match (F0's were invented — flat fields, not `dateBasis`; no `ageYears` column exists) |
| NM-B-166 | HIP data-push to ABDM (upload to ABHA) | Today the app is HIU/receive-only — in-app uploads stay in NineMo's own locker (S3+Mongo), never reach the ABDM network. Scope: (1) register as HIP in ABDM registry (separate from HIU reg); (2) care-context linking — attach records to patient's ABHA address w/ patient OTP confirmation; (3) serve consent-bound data requests: validate consent artefact → package FHIR R4 bundles (already stored as FHIR — format ready) → Curve25519-encrypt to requester's key via `nha-abdm-wrapper` → push to ABDM data-push URL; (4) new link/share callbacks in `CallbackController`; (5) HIP protocol layer in identity-abha-service + share flow to health-data-service | ⬜ FUTURE | Added 2026-07-16 after user query "can someone upload data to ABHA?" (answer: not yet). Sandbox first (`dev.abdm.gov.in`) |
| NM-B-169 | Child-profile + active-pregnancy read/update endpoints (ninemo-clinical-service) | (1) `GET /ninemo/children` (list current user's children) + `GET /ninemo/children/{id}` — today `childId` is only obtainable ONCE, inline from the transition response; after reinstall/new-device the whole child-mode UI (P6/P17/P18/P19) is unreachable (IS-028). (2) `GET /ninemo/profiles/pregnancy` (active pregnancy) — needed to obtain `pregnancyProfileId` to even call mode-transition (compounds NM-B-167). (3) `PATCH /ninemo/children/{id}` — mode-transition takes no body so baby name/sex/birth-weight are dropped (IS-029); need a way to set them post-transition. (4) Also add `X-User-Id` + ownership checks to the four child-mode controllers (IS-027 — currently unauthenticated-scoped, any user can read any child by UUID) | ✅ DONE | 2026-08-11. (1)+(2) New `ChildProfileController`/`ChildProfileService`: `GET /ninemo/children`, `GET /ninemo/children/{childId}`, `PATCH /ninemo/children/{childId}`; active-pregnancy `GET` shipped with NM-B-167. `ChildProfileResponse` adds server-computed `ageInMonths`. (3) `POST /mode/transition-to-postnatal/{id}` now takes an **optional** `ChildProfileUpdateRequest` body, so name/sex/DOB/birth measurements are persisted instead of dropped (IS-029), and returns the DTO rather than the bare entity. (4) IS-027 closed: new `ChildAccessGuard` (`requireOwnedChild`/`requireOwnedPregnancy`) is called by every child-scoped service path — Growth, Vaccination (incl. `markCompleted`, resolved via `VaccinationRecord.childId`), Milestone (incl. `markMilestone`, resolved via the Mongo doc's `child_id`) and ModeTransition; all four controllers now take `X-User-Id`. 5 guard unit tests + 1 Testcontainers test. Verified live: stranger → 403 on all four surfaces, parent → 200; PATCH merges without wiping untouched fields. Mobile `ModeTransitionRepository` gained `listChildren`/`getChild`/`updateChild` |
| NM-B-174 | Fix service-level security + gateway hygiene (found in live integration) | (1) `ninemo-clinical-service` `SecurityConfig` has `.anyRequest().authenticated()` with **no** auth mechanism → 403s every REST call (same bug as community IS-034, still open for clinical — blocks all F2/F3/F5 clinical screens end-to-end). Set `permitAll` trusting the gateway (or add a real X-User-Id/JWT filter). (2) Make `common-lib`'s `spring-boot-starter-web` `optional`/`provided` and have each servlet service declare it, so the reactive gateway stops inheriting MVC+tomcat (IS-032 — currently worked around via a per-gateway exclusion). (3) Audit all gateway route predicate ordering for other broad-vs-specific overlaps like IS-035 | ✅ DONE | 2026-08-11. (1) `ninemo-clinical-service` `SecurityConfig` → `permitAll` trusting the gateway-injected identity (IS-034); per-resource ownership moved to `ChildAccessGuard` in the service layer, which is where it belonged anyway. (2) `common-lib`'s `spring-boot-starter-web` is now `<optional>true</optional>` (it holds no servlet types; all five servlet services already declared it directly), and the api-gateway's per-module exclusion is gone — gateway verified to boot from its jar with zero `SpringMvcFoundOnClasspath` errors (IS-032). (3) Gateway route audit: every route now carries an explicit `order` (-10 specific / 10 normal / 20 broad) with the rule documented inline; `/api/v1/ninemo/community/**` ⊂ `/api/v1/ninemo/**` is the only overlap, the rest are disjoint. Verified live through the gateway with a real JWT: `/ninemo/children` → clinical 200, `/ninemo/community/clubs` → community 200. **Found and fixed en route: IS-039** — notification-service and clinical shared database `reejuven8_ninemo`, so their Flyway histories collided and one service could never boot; notification moved to its own `reejuven8_notification` |
| NM-B-173 | Surface caller's club membership/alias (ninemo-community-service) | `ClubResponse` DTO (`GET /clubs`, `GET /clubs/{id}`) exposes only aggregate `memberCount` — not the caller's own membership row, so the alias chosen at join time is unrecoverable after reinstall/new device (IS-031), and `SendMessageRequest.senderAlias` is required to chat. Add the caller's `alias` (and membership state) to `ClubResponse`, or a `GET /clubs/{id}/membership` (`X-User-Id`-scoped). Cheap: `DueDateClub.members` already holds it | ⬜ FUTURE | Found 2026-08-02 during ninemo-mobile F6 (community chat) build. Mobile mitigates via join-time route arg + editable alias field (defaults "Member") |
| NM-B-168 | Doctor search/lookup endpoint (identity-abha-service) | No endpoint exists to resolve a doctor by phone number or any human-friendly identifier — `ConsentService.grantConsent` requires the caller to already have the doctor's raw `users.id` UUID, and nothing enriches `ConsentResponse` with a doctor display name either. Not a UX gap against the design spec (mockup P9 already assumes manual "doctor's NineMo ID" entry), but the UUID is not something a patient can realistically know/type today — some doctor-facing surface (QR code, shareable link, or search-by-phone) is needed for this flow to be usable in practice | ⬜ FUTURE | Found 2026-07-22 while building `ninemo-mobile` F4 Consent Manager (P9). Also note: `ConsentService.grantConsent` never checks the resolved user actually has `UserRole.DOCTOR` |

## Deferred / Rejected

| ID | Item | Decision |
|---|---|---|
| NM-B-170 | gRPC internal RPC | ⏸ DEFERRED — adopt only on measured hot path (Communication_Patterns §4.3) |
| NM-B-171 | GraphQL | 🚫 REJECTED — backend-aggregation rule removes value |
| NM-B-172 | Spring Cloud Contract tests | ⏸ DEFERRED — nice-to-have (plan §5) |
