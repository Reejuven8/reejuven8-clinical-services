# NineMo Backend — Architecture Document

> **Phases 1–7 Complete — updated June 2026**
> All services fully implemented. Business logic, security, AI pipeline, pediatric mode, community features, observability, unit tests, CI, and Kubernetes manifests are in place.
> Communication method choices are governed by `Communication_Patterns.md` — summarized in §19; open gaps in §20.

---

## 1. Repository Layout

```
NineMo/
├── .github/workflows/ci.yml             ← CI: backend-test, python-test, docker builds
└── ninemo-backend/
    ├── pom.xml                          ← Maven multi-module parent (7 Java modules)
    ├── common-lib/                      ← Shared kernel JAR
    ├── services/
    │   ├── api-gateway/                 ← Port 8080
    │   ├── identity-abha-service/       ← Port 8081
    │   ├── health-data-service/         ← Port 8082
    │   ├── ai-parsing-service/          ← Port 8083  (Python, not a Maven module)
    │   ├── ninemo-clinical-service/     ← Port 8084
    │   ├── notification-service/        ← Port 8085
    │   └── ninemo-community-service/    ← Port 8086
    ├── k8s/                             ← Deployment+Service+HPA per service,
    │                                      namespace, configmap, secrets (template), ingress
    └── infrastructure/
        ├── docker-compose.infra.yml     ← DBs + brokers + observability stack
        ├── docker-compose.yml           ← Full stack
        ├── init-scripts/
        │   ├── postgres-init.sql        ← Create reejuven8_identity + reejuven8_ninemo
        │   └── mongo-init.js            ← Create 11 collections + 20 indexes
        ├── kafka/
        │   └── create-topics.sh         ← Pre-create 3 Kafka topics
        └── prometheus/
            └── prometheus.yml           ← Scrape config for all 6 services
```

---

## 2. Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language (Java services) | Java | 26 (Homebrew openjdk) |
| Framework | Spring Boot | 3.3.5 |
| Build | Maven multi-module | 3.9.x |
| Service mesh | Spring Cloud | 2023.0.3 |
| Language (AI service) | Python | 3.11 |
| AI framework | FastAPI + uvicorn | latest |
| Relational DB | PostgreSQL | 16 |
| Document DB | MongoDB | 7 |
| Cache | Redis | 7 |
| File storage | AWS S3 | — |
| Audit streaming | Apache Kafka | 3.x (Confluent 7.6) |
| Worker queues | RabbitMQ | 3.13 |
| FHIR | HAPI FHIR R4 | 7.2.0 |
| ABDM | nha-abdm-wrapper | — |
| JWT | jjwt | 0.12.6 |
| Crypto | Bouncy Castle | 1.78.1 |
| Circuit breaker | Resilience4j | 2.2.0 |
| API docs | springdoc-openapi | 2.6.0 |
| DB migrations | Flyway | 10.x (Boot-managed) |
| Metrics | Micrometer → Prometheus | Boot-managed |
| Distributed tracing | Micrometer OTel bridge → Zipkin | Boot-managed |
| JSON logging | logstash-logback-encoder | 8.0 |
| Dashboards | Grafana | 11.1 |
| Container | Docker + Compose | — |

> **Java 26 quirks:** Lombok requires `<lombok.version>1.18.38</lombok.version>` override (Spring Boot bundles 1.18.34 which fails on Java 26). Mockito/Byte Buddy requires `-Dnet.bytebuddy.experimental=true` in the Surefire plugin.

---

## 3. Parent POM (`pom.xml`)

**`groupId:`** `com.reejuven8`
**`artifactId:`** `ninemo-backend`
**`version:`** `1.0.0-SNAPSHOT`

**Declared modules (build order):**
1. `common-lib`
2. `services/api-gateway`
3. `services/identity-abha-service`
4. `services/health-data-service`
5. `services/notification-service`
6. `services/ninemo-clinical-service`
7. `services/ninemo-community-service`

> `ai-parsing-service` is Python — not a Maven module.

**`<dependencyManagement>` locks:**
- All Spring Cloud artifacts via BOM
- HAPI FHIR (`hapi-fhir-base`, `hapi-fhir-structures-r4`)
- JWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
- Bouncy Castle (`bcprov-jdk18on`, `bcpkix-jdk18on`)
- Resilience4j (`resilience4j-spring-boot3`)
- springdoc (`springdoc-openapi-starter-webmvc-ui`)
- MapStruct (`mapstruct` + `mapstruct-processor`)
- Testcontainers BOM
- `logstash-logback-encoder:8.0` (not in Spring Boot BOM; version pinned here)

> Prometheus, OTel, and Zipkin are managed by Spring Boot 3.3.5 parent BOM — no explicit version needed.

**Global dependencies (all modules):**
- `lombok` (optional, v1.18.38)
- `spring-boot-starter-test` (test scope, includes JUnit 5 + Mockito)

**Surefire plugin (global):**
- `argLine: -Dnet.bytebuddy.experimental=true` — required for Mockito on Java 26

---

## 4. `common-lib`

**Package:** `com.reejuven8.common`
**Purpose:** Shared kernel. No service-specific logic here.

### Contents

| File | Class | Role |
|---|---|---|
| `dto/ApiResponse.java` | `ApiResponse<T>` | Envelope: `{ status, data, error, metadata }`. Static factories: `ApiResponse.success(T)`, `ApiResponse.error(ErrorResponse)` |
| `dto/ErrorResponse.java` | `ErrorResponse` | `{ code, message, details[] }`. Static factory: `ErrorResponse.of(code, message)` |
| `dto/PagedResponse.java` | `PagedResponse<T>` | List + nested `Pagination { page, size, totalElements, totalPages }` |
| `event/BaseEvent.java` | `BaseEvent` (abstract) | `eventId` (UUID auto), `timestamp`, `correlationId`, `source`. Uses `@SuperBuilder` |
| `event/ConsentGrantedEvent.java` | `ConsentGrantedEvent` | Kafka: `abdm.consent.granted`. Fields: `consentId`, `patientId`, `doctorId`, `grantedAt`, `expiresAt` |
| `event/AbdmDataReceivedEvent.java` | `AbdmDataReceivedEvent` | Kafka: `abdm.data.received`. Fields: `patientId` |
| `event/DocumentParsedEvent.java` | `DocumentParsedEvent` | Kafka: `document.data.parsed`. Fields: `patientId`, `s3Url`, `parsedObservations` (JSON) |
| `exception/BaseException.java` | `BaseException` | Abstract base with `errorCode` and HTTP status |
| `exception/ResourceNotFoundException.java` | `ResourceNotFoundException` | 404; constructor: `(String resource, String id)` |
| `exception/UnauthorizedException.java` | `UnauthorizedException` | 403; constructor: `(String message)` |
| `security/JwtClaims.java` | `JwtClaims` | Java 21 record: `(UUID userId, String role, String abhaAddress)` |
| `util/CorrelationMdc.java` | `CorrelationMdc` | AutoCloseable MDC restore for event consumers: `try (var x = CorrelationMdc.restore(id))`; generates UUID when inbound id absent |
| `util/DateUtils.java` | `DateUtils` | `eddFromLmp(lmpDate)`, `eddFromIvfDay5Transfer(date)`, `gestationalWeek(eddDate)`, `gestationalDay(eddDate)`, `trimester(week)` |
| `util/FhirUtils.java` | `FhirUtils` | FHIR R4 / LOINC / UCUM constants. LOINC codes: Hb (718-7), BP-systolic (8480-6), BP-diastolic (8462-4), Glucose (2339-0), TSH (3016-3), FHR (55283-6) |

---

## 5. Infrastructure

### Docker Compose — Infra Only (`docker-compose.infra.yml`)

| Service | Image | Port | Notes |
|---|---|---|---|
| `postgres` | `postgres:16-alpine` | 5432 | Init script creates both DBs; health: `pg_isready` |
| `mongodb` | `mongo:7` | 27017 | Init script creates 11 collections + 20 indexes |
| `redis` | `redis:7-alpine` | 6379 | Password-protected; health: `redis-cli ping` |
| `zookeeper` | `confluentinc/cp-zookeeper:7.6.0` | 2181 | Required for Kafka |
| `kafka` | `confluentinc/cp-kafka:7.6.0` | 9092 | Single-broker dev setup |
| `rabbitmq` | `rabbitmq:3.13-management-alpine` | 5672 / 15672 | Management UI on 15672 |
| `prometheus` | `prom/prometheus:v2.53.0` | 9090 | Scrapes all 6 services via `host.docker.internal` |
| `zipkin` | `openzipkin/zipkin:3` | 9411 | Receives OTel spans from all services |
| `grafana` | `grafana/grafana:11.1.0` | 3000 | Pre-wired to Prometheus; admin/dev_password |

All services share network `reejuven8-network` (bridge).

### Prometheus Config (`infrastructure/prometheus/prometheus.yml`)

Scrapes `/actuator/prometheus` on all 6 Java services every 15 seconds via `host.docker.internal:{port}`. The Zipkin endpoint is configurable per service via env var `ZIPKIN_ENDPOINT` (default: `http://localhost:9411/api/v2/spans`).

### PostgreSQL Init (`postgres-init.sql`)

Creates two databases:

| Database | Owner Service | Tables |
|---|---|---|
| `reejuven8_identity` | `identity-abha-service` | users, patient_profiles, doctor_profiles, addresses, user_consents, appointments |
| `reejuven8_ninemo` | `ninemo-clinical-service`, `notification-service` | pregnancy_profiles, child_profiles, vaccination_records, medication_schedules, hospital_bag_items, diet_food_safety, notification_logs |

Extensions enabled: `uuid-ossp`, `pg_trgm` (for fuzzy diet search).

### MongoDB Init (`mongo-init.js`)

Creates database `reejuven8` with 11 collections and all 20 indexes from `Database_Design.md §10.2`:

| Collection | Key Indexes |
|---|---|
| `fhir_resources` | `{ patient_id, resource_type, effective_datetime }` |
| `ninemo_timeline_feed` | `{ pregnancy_profile_id, gestational_week }` (unique) |
| `symptom_logs` | `{ patient_id, logged_at }`, `{ severity_flag, logged_at }` |
| `vitals_logs` | `{ patient_id, vital_type, logged_at }` |
| `kick_counter_sessions` | `{ patient_id, session_start }` |
| `contraction_sessions` | `{ patient_id, session_start }` |
| `growth_measurements` | `{ child_id, measurement_date }`, `{ child_id, age_in_months }` |
| `developmental_milestones` | `{ child_id, month }` |
| `due_date_clubs` | `{ due_date_month }` (unique), `{ members.user_id }` |
| `chat_messages` | `{ club_id, channel_id, sent_at }` |
| `content_articles` | `{ category, is_published }`, `{ gestational_weeks }` |

### Kafka Topics (`create-topics.sh`)

| Topic | Partitions | Producer | Consumers |
|---|---|---|---|
| `abdm.consent.granted` | 3 | `identity-abha-service` | `health-data-service` |
| `abdm.data.received` | 3 | `identity-abha-service` | `health-data-service` |
| `document.data.parsed` | 3 | `ai-parsing-service` | `ninemo-clinical-service`, `health-data-service` |

---

## 6. Event Bus Topology

```
KAFKA (immutable audit streams)
─────────────────────────────────────────────────────────────────────
  abdm.consent.granted        identity-abha-service → health-data-service
  abdm.data.received          identity-abha-service → health-data-service
  document.data.parsed        ai-parsing-service    → ninemo-clinical-service
                                                    → health-data-service

RABBITMQ (worker queues with DLX retry)
─────────────────────────────────────────────────────────────────────
  document.unstructured.uploaded   health-data-service     → ai-parsing-service
  clinical.risk.detected           ninemo-clinical-service → notification-service
  patient.milestone.due            ninemo-clinical-service → notification-service
```

All RabbitMQ queues are declared with `x-dead-letter-exchange` so failed messages retry via DLX before being dead-lettered.

**Rule:** Kafka = facts/audit (replayable, immutable). RabbitMQ = tasks (competing consumers, retry, DLX, delayed delivery). Never swap. Full rationale per link: `Communication_Patterns.md §4.2`.

**Correlation ID in events:** every publisher stamps the current `MDC correlationId` into the outbound message — Kafka events via `BaseEvent.correlationId` (`ConsentService`, `CallbackController`), RabbitMQ payloads via a `correlationId` map key (`ClinicalRiskPublisher`, `MilestoneReminderPublisher`) or record field (`DocumentUploadMessage`). All 6 Java listeners restore it via `CorrelationMdc.restore()` (common-lib, try-with-resources, UUID fallback); the Python pipeline binds it to structlog and forwards it into `DocumentParsedEvent` — one trace spans upload → OCR → parse → persist → notify.

---

## 7. Observability Stack

Every Java service includes the same observability slice:

### Structured Logging (`logback-spring.xml` in each service)

- **Dev** (`!prod, !staging`): colored console with pattern `%d [%thread] %-5level %logger [cid:%X{correlationId}] - %msg`
- **Prod/staging**: JSON via `LogstashEncoder` — fields: `timestamp`, `level`, `service`, `correlationId`, `traceId`, `spanId`, `logger`, `message`

### Correlation ID Propagation

| Service type | Class | Mechanism |
|---|---|---|
| MVC (5 services) | `filter/CorrelationIdFilter` extends `OncePerRequestFilter` | Reads `X-Correlation-Id` header; generates UUID if absent; injects into `MDC`; echoes in response header |
| Reactive (api-gateway) | `filter/CorrelationIdWebFilter` implements `WebFilter` | Same logic via `ServerWebExchange` mutation |

### Prometheus Metrics

All services expose `/actuator/prometheus` (included in `management.endpoints.web.exposure.include`). P99 histogram enabled for `http.server.requests`.

**Custom metrics:**

| Metric | Service | Tags | Description |
|---|---|---|---|
| `ninemo_symptom_triage_total` | `ninemo-clinical-service` | `severity={NORMAL,WARNING,CRITICAL}` | Incremented on every triage evaluation |

### Distributed Tracing

- Bridge: `micrometer-tracing-bridge-otel`
- Exporter: `opentelemetry-exporter-zipkin` → `http://localhost:9411/api/v2/spans`
- Sampling: 100% in dev (`management.tracing.sampling.probability: 1.0`); set to 0.1 for prod
- Zipkin UI: `http://localhost:9411`
- Grafana: `http://localhost:3000` (admin / dev_password)

---

## 8. Service: `api-gateway`

**Port:** 8080
**Package:** `com.reejuven8.gateway`
**Database:** Redis (rate limiting + JWT blacklist)
**Spring artifact:** `spring-cloud-starter-gateway` (reactive WebFlux, not servlet)

### Key Files

| File | Role |
|---|---|
| `ApiGatewayApplication.java` | Entry point |
| `config/RateLimitConfig.java` | Redis-backed sliding-window rate limiter per IP+route |
| `config/FallbackController.java` | Circuit-breaker fallback response |
| `filter/JwtAuthFilter.java` | Validates JWT on every non-public request; checks Redis blacklist (`auth:blacklist:{jti}`); injects `X-User-Id`, `X-User-Role` downstream |
| `filter/CorrelationIdWebFilter.java` | Generates/propagates `X-Correlation-Id` header (reactive `WebFilter`, order `HIGHEST_PRECEDENCE`) |
| `filter/RequestLoggingFilter.java` | Logs method, path, duration, response status |
| `resources/application.yml` | Full route table for all 6 downstream services + observability config |

### Route Table

| Route ID | Path Prefix | Downstream |
|---|---|---|
| `identity-service` | `/api/v1/identity/**` | `identity-abha-service:8081` |
| `health-data-service` | `/api/v1/health/**` | `health-data-service:8082` |
| `ai-parsing-service` | `/api/v1/parse/**` | `ai-parsing-service:8083` |
| `clinical-service` | `/api/v1/ninemo/**` | `ninemo-clinical-service:8084` |
| `notification-service` | `/api/v1/notifications/**` | `notification-service:8085` |
| `community-service` | `/api/v1/community/**`, `/ws/**` | `ninemo-community-service:8086` |

**Public paths** (no JWT): `/api/v1/identity/auth/`, `/api/v1/identity/abha/`, ABDM callback paths, `/api/v1/notifications/callbacks/` (vendor webhooks — signature-verified at service), `/actuator/health`.

---

## 9. Service: `identity-abha-service`

**Port:** 8081
**Package:** `com.reejuven8.identity`
**Database:** PostgreSQL (`reejuven8_identity`) + Redis

### Key Files

| Layer | Files |
|---|---|
| Entry point | `IdentityAbhaApplication.java` |
| Config | `SecurityConfig.java`, `RedisConfig.java`, `KafkaProducerConfig.java`, `AbdmConfig.java` |
| Security | `JwtTokenProvider.java` — HS256 JWT; `RsaEncryptionService.java` — RSA-OAEP via Bouncy Castle |
| Services | `AuthService.java`, `OtpService.java`, `TokenService.java`, `AbhaService.java`, `ConsentService.java` |
| Controllers | `AuthController`, `AbhaController`, `ConsentController`, `CallbackController` |
| Exception | `GlobalExceptionHandler.java` |
| Filter | `filter/CorrelationIdFilter.java` |
| Entities | `User`, `PatientProfile`, `DoctorProfile`, `Address`, `UserConsent` |
| Enums | `UserRole` (PATIENT/DOCTOR/ADMIN), `ConsentStatus`, `AddressType`, `BiologicalSex` |
| DTOs | `LoginRequest`, `TokenResponse`, `AbhaScanRequest`, `ConsentRequest` |
| Repositories | `UserRepository`, `PatientProfileRepository`, `DoctorProfileRepository`, `UserConsentRepository` |

### Business Logic

- **OTP flow:** OTP stored in Redis as `otp:{phone}` with 5-min TTL; consumed and deleted on verify
- **JWT:** HS256 access token (15-min), UUID refresh token stored in Redis `auth:session:{userId}` (7-day)
- **Logout:** JTI blacklisted in Redis `auth:blacklist:{jti}` with remaining access token TTL
- **ABDM encryption:** All Aadhaar/OTP payloads encrypted with `RSA/ECB/OAEPWithSHA-1AndMGF1Padding` before ABDM API calls
- **Consent:** Published to Kafka `abdm.consent.granted` on grant; cascades to `health-data-service`

### Flyway Migrations (`reejuven8_identity`)

| File | Creates |
|---|---|
| `V1__create_enums.sql` | `user_role`, `biological_sex`, `address_type`, `consent_status`, `appointment_type`, `appointment_status` |
| `V2__create_users.sql` | `users` table + `update_updated_at_column()` trigger |
| `V3__create_patient_profiles.sql` | `patient_profiles` (1:1 FK → users) |
| `V4__create_doctor_profiles.sql` | `doctor_profiles` (1:1 FK → users) |
| `V5__create_addresses_and_consents.sql` | `addresses`, `user_consents` |

### API Endpoints

```
POST /api/v1/identity/auth/otp/send
POST /api/v1/identity/auth/login                 ← phone + OTP
POST /api/v1/identity/auth/register
POST /api/v1/identity/auth/refresh
POST /api/v1/identity/auth/logout
POST /api/v1/identity/abha/enroll/otp/generate   ← Start ABHA enrollment OTP
POST /api/v1/identity/abha/enroll/otp/verify     ← Verify OTP, create ABHA
POST /api/v1/identity/abha/enroll/address        ← Link ABHA (PHR) address
POST /api/v1/identity/abha/scan                  ← ABHA QR scan (Phase 2 stub — 501)
POST /api/v1/identity/consent/grant
POST /api/v1/identity/consent/revoke/{consentId}
GET  /api/v1/identity/consent/list
POST /api/v1/identity/callback/consent           ← ABDM webhook (txnId ↔ Redis)
POST /api/v1/identity/callback/data              ← ABDM webhook → Kafka abdm.data.received
```

---

## 10. Service: `health-data-service`

**Port:** 8082
**Package:** `com.reejuven8.healthdata`
**Database:** MongoDB (`reejuven8`) + AWS S3
**Message role:** Kafka consumer (`abdm.consent.granted`, `abdm.data.received`, `document.data.parsed`); RabbitMQ producer (`document.unstructured.uploaded`)

### Key Files

| Layer | Files |
|---|---|
| Entry point | `HealthDataApplication.java` |
| Config | `MongoConfig.java`, `S3Config.java`, `KafkaConsumerConfig.java`, `RabbitConfig.java` |
| Controllers | `HealthRecordController`, `FileUploadController` |
| Services | `FhirResourceService.java`, `FileStorageService.java`, `RecordSyncService.java` |
| Filter | `filter/CorrelationIdFilter.java` |
| MongoDB document | `model/document/FhirResource.java` |
| Repository | `FhirResourceRepository.java` |
| Listeners | `ConsentGrantedListener.java`, `AbdmDataReceivedListener.java` (Kafka) |
| Publisher | `DocumentUploadedPublisher.java` (RabbitMQ) |
| Exception | `GlobalExceptionHandler.java` |

### Business Logic

- **S3 upload:** `FileStorageService.upload()` — key pattern `patients/{id}/documents/{uuid}/{filename}` ; presigned URLs with 15-min expiry
- **RabbitMQ queue:** `document.unstructured.uploaded` declared with DLX (`document.unstructured.dlx`)
- **Kafka consumers:** group ID `health-data-service-group`

### API Endpoints

```
GET  /api/v1/health/records                          ← paged, X-User-Id header
GET  /api/v1/health/records/{id}
GET  /api/v1/health/records/patient/{patientId}?resourceType=
POST /api/v1/health/files/upload                     ← multipart; queues RabbitMQ parse job
GET  /api/v1/health/files/download?s3Key=            ← 15-min presigned URL (query param — s3Key contains slashes)
GET  /api/v1/health/files/events?s3Key=              ← SSE parse progress (PROCESSING → PARSED)
```

**SSE parse progress:** `ParseProgressService` holds per-document `SseEmitter` lists (5-min
timeout); `ParsedDocumentListener` calls `notifyParsed()` when `document.data.parsed` arrives,
broadcasting `PARSED` and completing the stream. Completed docs retained 30 min so late
subscribers get `PARSED` immediately. s3Key is a query param (contains slashes).

---

## 11. Service: `ai-parsing-service`

**Port:** 8083
**Language:** Python 3.11 + FastAPI
**Not a Maven module.** Lives at `services/ai-parsing-service/`.

### Key Files

| File | Role |
|---|---|
| `app/main.py` | FastAPI app, lifespan — starts RabbitMQ consumer as `asyncio.Task` on startup; cancels on shutdown |
| `app/config.py` | Pydantic `BaseSettings` — env vars: AWS, RabbitMQ, Kafka, S3 |
| `app/services/ocr_service.py` | AWS Textract: `extract_text(s3_key)` → raw text string |
| `app/services/ner_service.py` | Hybrid NER: spaCy `en_core_sci_sm` (lazy-loaded with graceful fallback) + regex `([A-Za-z\s]+)\s*:\s*(\d+\.?\d*)\s*([A-Za-z%/]+)` → `ParsedObservation` list |
| `app/services/fhir_mapper.py` | FHIR R4 `Observation` JSON builder from `ParsedObservation` |
| `app/pipeline.py` | Orchestrates: download → OCR → NER → FHIR → publish |
| `app/consumers/rabbitmq_consumer.py` | aio-pika consumer on `document.unstructured.uploaded`; tenacity retry (10 attempts, exponential backoff 2–30s); nack-on-exception → DLX |
| `app/producers/kafka_producer.py` | confluent-kafka non-blocking `produce()` + `poll(0)` → `document.data.parsed` |
| `app/utils/loinc_mapper.py` | Static keyword→LOINC map + fuzzy lookup; confidence 0.95 (exact) / 0.75 (fuzzy) |

### Processing Pipeline

```
RabbitMQ: document.unstructured.uploaded
  → s3_client.download(s3_key)
  → ocr_service.extract_text()           [AWS Textract]
  → ner_service.extract_observations()   [spaCy + regex hybrid]
  → fhir_mapper.build_observations()     [FHIR R4 JSON]
  → kafka_producer.publish()             [document.data.parsed]
```

If spaCy model is not downloaded, service falls back to regex-only NER without crashing.

### API Endpoints

```
GET  /health
POST /api/v1/parse/document   ← Manual trigger for testing
```

---

## 12. Service: `ninemo-clinical-service`

**Port:** 8084
**Package:** `com.reejuven8.ninemo.clinical`
**Database:** PostgreSQL (`reejuven8_ninemo`) + MongoDB (`reejuven8`)
**Message role:** Kafka consumer (`document.data.parsed`); RabbitMQ producer (`clinical.risk.detected`, `patient.milestone.due`)

This is the largest service — the clinical brain of NineMo.

### PostgreSQL Entities (JPA)

| Entity | Table | Key Fields |
|---|---|---|
| `PregnancyProfile` | `pregnancy_profiles` | `userId`, `lmpDate`, `eddDate`, `eddCalculationMethod`, `heightCm`, `baselineBmi`, `highRiskFlags` (JSONB), `isActive`, `deliveryDate`, `deliveryType` |
| `ChildProfile` | `child_profiles` | `pregnancyProfileId` (unique), `parentUserId`, `dateOfBirth`, `biologicalSex`, `birthWeightKg`, `isActive` |
| `VaccinationRecord` | `vaccination_records` | `childId`, `vaccineName`, `vaccineCode`, `doseNumber`, `scheduledDate`, `administeredDate`, `status` (enum); unique on (childId, vaccineName, doseNumber) |
| `MedicationSchedule` | `medication_schedules` | `userId`, `medicationName`, `scheduleTime` (enum), `currentInventoryCount`, `refillThreshold` |
| `HospitalBagItem` | `hospital_bag_items` | `pregnancyProfileId`, `itemName`, `category` (enum), `isPacked` |
| `DietFoodSafety` | `diet_food_safety` | `ingredientName` (unique), `ingredientNameHindi`, `safetyRating` (enum), `trimesterTags` (JSONB), `categories` (JSONB) |

### Enums

| Enum | Values |
|---|---|
| `EddCalculationMethod` | LMP, ULTRASOUND, IVF |
| `DeliveryType` | NORMAL, CAESAREAN, ASSISTED |
| `VaccinationStatus` | PENDING, COMPLETED, SKIPPED, OVERDUE |
| `BiologicalSex` | MALE, FEMALE, OTHER |
| `SeverityFlag` | NORMAL, WARNING, CRITICAL |

### MongoDB Documents

| Class | Collection | Key Fields |
|---|---|---|
| `TimelineFeed` | `ninemo_timeline_feed` | `pregnancyProfileId`, `gestationalWeek`, `babyDevelopment`, `maternalChanges`, `scheduledMilestones`, `dietTips`, `yogaRoutine` |
| `SymptomLog` | `symptom_logs` | `patientId`, `gestationalWeekAtLog`, `symptoms[]`, `vitalsAtLog`, `severityFlag`, `triageResult[]` |
| `VitalsLog` | `vitals_logs` | `patientId`, `vitalType`, `measurements`, `source`, `alertTriggered` |
| `KickCounterSession` | `kick_counter_sessions` | `patientId`, `totalKicks`, `durationTo10KicksMinutes`, `kickTimestamps[]`, `isConcerning` |
| `ContractionSession` | `contraction_sessions` | `patientId`, `contractions[]` (startTime, durationSeconds, intervalFromPreviousSeconds), `averageIntervalSeconds`, `averageDurationSeconds`, `isLaborPattern`, `alertTriggered` |
| `GrowthMeasurement` | `growth_measurements` | `childId`, `ageInMonths`, `heightCm`, `weightKg`, `headCircumferenceCm`, `zScores`, `percentiles`, `previousPercentiles`, `alertFlags[]`, `crossedPercentileLines` |
| `DevelopmentalMilestone` | `developmental_milestones` | `childId`, `month`, `category`, `milestones[]` |

### Service Layer

**Timeline:**
- `TimelineService` — `computeGestationalWeek(eddDate)`: LMP=EDD-280d; week=floor(daysSinceLMP/7)+1, clamped [1,42]. `computeTrimester(week)`: T1≤13, T2≤27, T3≥28. Falls back to generated content if MongoDB feed not seeded.
- EDD strategies: `LMPCalculationStrategy` (Naegele: LMP+280d), `IVFCalculationStrategy` (Day-5 transfer+261d), `UltrasoundCalculationStrategy`

**Triage (chain-of-responsibility, `List<TriageRule>` injected):**
- `PreeclampsiaRule` — BP ≥ 140/90 + neurological symptom + week ≥ 20 → CRITICAL
- `AnemiaRule` — Hb < 11.0 g/dL + week ≥ 14 → WARNING
- `GestationalDiabetesRule` — fasting glucose ≥ 92 mg/dL + weeks 24–28 → WARNING
- `PrematureLaborRule` — labor symptoms + week < 37 → CRITICAL
- `ReducedFetalMovementRule` — reduced FM + week ≥ 28 → CRITICAL
- `SymptomTriageEngine` — evaluates all rules; assigns `SeverityFlag`; increments `ninemo_symptom_triage_total{severity}` Micrometer counter

**Vitals:**
- `VitalsService` — normal range map: bp_systolic [90–140], bp_diastolic [60–90], heart_rate [60–100], fhr [110–160], temperature [36.1–37.5], spo2 [95–100]. Publishes `CLINICAL_RISK_DETECTED` if out of range.

**KickCounter:**
- `KickCounterService` — WHO guideline: concerning if `totalKicks < 10 && sessionMinutes ≥ 120`. Publishes `REDUCED_FETAL_MOVEMENT → CRITICAL` if concerning.

**Contractions:**
- `ContractionService` — labor pattern: `avgInterval ≤ 300s AND avgDuration ≥ 60s`. Publishes `PREMATURE_LABOR_PATTERN → CRITICAL` if labor pattern and `gestationalWeek < 37`.

**Summary Card:**
- `SummaryCardService` — aggregates active `PregnancyProfile`, latest `SymptomLog`, `VitalsLog`, and `KickCounterSession` into a single cross-DB response.

**Pediatric (Phase 5):**

| Service | Implementation |
|---|---|
| `WhoLmsTable` | WHO Child Growth Standards LMS reference data (0–60m, both sexes) for weight/height/head circumference. Linear interpolation between monthly entries. |
| `GrowthChartService` | `recordMeasurement()`: computes WAZ, HAZ, HCZ Z-scores via `Z = ((X/M)^L - 1)/(L*S)`; converts to percentile via rational normal CDF approximation; flags Z<-2 (undernutrition), Z<-3 (severe), Z>+2 (overweight); detects WHO major channel crossings (3/15/50/85/97). |
| `VaccinationScheduleService` | `generateSchedule()`: idempotent IAP 2023 schedule — 37 doses from birth to 10+ years (BCG, Hep-B, OPV, DPT, IPV, Hib, Rotavirus, PCV, MMR, Varicella, Hep-A, TCV, Tdap, HPV). `markCompleted()`: sets COMPLETED status + administeredDate. |
| `ModeTransitionService` | `transitionToPostnatalMode(pregnancyProfileId)`: marks `PregnancyProfile.isActive=false`, finds-or-creates `ChildProfile`, generates IAP schedule, publishes `POSTNATAL_VISIT_WEEK_1` milestone reminder. |
| `DevelopmentalMilestoneService` | WHO milestone checklists (months 2/4/6/9/12/18/24/36/48/60). `getOrCreateCheckIn()` seeds the month's checklist; `markMilestone()` toggles achievement; <50% achieved at check-in → `DEVELOPMENTAL_DELAY_RISK` flag + RabbitMQ risk alert. |

### Event Handling

| Class | Role |
|---|---|
| `ParsedDataListener` | Kafka: `document.data.parsed` consumer |
| `ClinicalRiskPublisher` | RabbitMQ: `clinical.risk.exchange` → `clinical.risk.detected` |
| `MilestoneReminderPublisher` | RabbitMQ: `milestone.reminder.exchange` → `patient.milestone.due` |

### Repositories

| Repository | Type | Key Methods |
|---|---|---|
| `PregnancyProfileRepository` | JPA | `findByUserIdAndIsActiveTrue` |
| `ChildProfileRepository` | JPA | `findByParentUserIdAndIsActiveTrue`, `findByPregnancyProfileId` |
| `VaccinationRecordRepository` | JPA | `findByChildIdOrderByScheduledDateAsc`, `findByChildIdAndStatus` |
| `GrowthMeasurementRepository` | MongoDB | `findByChildIdOrderByMeasurementDateDesc`, `findTop1ByChildIdOrderByMeasurementDateDesc` |
| `TimelineFeedRepository` | MongoDB | `findByPregnancyProfileIdAndGestationalWeek` |
| `DevelopmentalMilestoneRepository` | MongoDB | `findByChildIdAndMonth`, `findByChildIdOrderByMonthAsc` |

### Flyway Migrations (`reejuven8_ninemo`)

| File | Creates |
|---|---|
| `V1__create_enums.sql` | All NineMo PostgreSQL enums + `update_updated_at_column()` trigger |
| `V2__create_pregnancy_profiles.sql` | `pregnancy_profiles` + indexes |
| `V3__create_child_profiles.sql` | `child_profiles` + indexes |
| `V4__create_vaccination_records.sql` | `vaccination_records` + unique constraint |
| `V5__create_medication_schedules.sql` | `medication_schedules` |
| `V6__create_hospital_bag_items.sql` | `hospital_bag_items` |
| `V7__create_diet_food_safety.sql` | `diet_food_safety` + `pg_trgm` + trigram indexes |
| `V8__seed_diet_data.sql` | 15 common Indian ingredients with safety ratings |

### API Endpoints

```
GET  /api/v1/ninemo/timeline/current
GET  /api/v1/ninemo/timeline/week/{week}
GET  /api/v1/ninemo/summary-card/{patientId}
POST /api/v1/ninemo/symptoms
GET  /api/v1/ninemo/symptoms
POST /api/v1/ninemo/vitals
GET  /api/v1/ninemo/vitals/{vitalType}
POST /api/v1/ninemo/kick-counter/sessions
PUT  /api/v1/ninemo/kick-counter/sessions/{sessionId}/kick
PUT  /api/v1/ninemo/kick-counter/sessions/{sessionId}/end
POST /api/v1/ninemo/contractions/sessions
PUT  /api/v1/ninemo/contractions/sessions/{sessionId}/contraction
PUT  /api/v1/ninemo/contractions/sessions/{sessionId}/end
POST /api/v1/ninemo/growth/children/{childId}/measurements
GET  /api/v1/ninemo/growth/children/{childId}/measurements
GET  /api/v1/ninemo/vaccinations/children/{childId}/schedule
PUT  /api/v1/ninemo/vaccinations/{vaccinationId}/mark-completed
     ?administeredDate={YYYY-MM-DD}&administeredBy={name}
POST /api/v1/ninemo/mode/transition-to-postnatal/{pregnancyProfileId}
GET  /api/v1/ninemo/diet/search?q={query}
GET  /api/v1/ninemo/milestones/children/{childId}
GET  /api/v1/ninemo/milestones/children/{childId}/month/{month}
PUT  /api/v1/ninemo/milestones/{documentId}/achieve?milestoneName=&achieved=
```

All endpoints return the `ApiResponse<T>` envelope (normalized in NM-B-165 — no raw DTO responses remain).

---

## 13. Service: `notification-service`

**Port:** 8085
**Package:** `com.reejuven8.notification`
**Database:** PostgreSQL (`reejuven8_ninemo`)
**Message role:** RabbitMQ consumer (`clinical.risk.detected`, `patient.milestone.due`)

### Key Files

| File | Role |
|---|---|
| `NotificationApplication.java` | Entry point |
| `config/RabbitConfig.java` | Declares all queues + exchanges + DLX bindings |
| `config/TwilioConfig.java` | Twilio SDK initialization |
| `service/NotificationOrchestrator.java` | Routes by channel; persists `NotificationLog`; PUSH fans out to all registered devices for the user (legacy explicit-token fallback); local var named `entry` (not `log`) to avoid Lombok `@Slf4j` field name collision |
| `service/DeviceTokenService.java` | FCM token registry — upsert by token (re-login re-binds device to new user), unregister, `tokensForUser()` |
| `controller/DeviceController.java` | `POST /api/v1/notifications/devices` (X-User-Id + fcmToken + platform ANDROID/IOS), `DELETE /devices?fcmToken=` |
| `model/entity/DeviceToken.java` | JPA: `userId`, `fcmToken` (unique), `platform`, timestamps |
| `service/WhatsAppService.java` | Twilio WhatsApp send; sets `statusCallback` when `twilio.status-callback-url` configured |
| `service/SmsService.java` | Twilio SMS send; same statusCallback wiring |
| `service/TwilioCallbackService.java` | Applies delivery callbacks to `NotificationLog` — monotonic (delivered/read → DELIVERED, failed/undelivered → FAILED; terminal states never regress; idempotent) |
| `controller/TwilioCallbackController.java` | `POST /api/v1/notifications/callbacks/twilio` (form-encoded); verifies `X-Twilio-Signature` via Twilio `RequestValidator` |
| `service/PushNotificationService.java` | Firebase Admin SDK FCM send |
| `listener/ClinicalRiskListener.java` | `@RabbitListener` on `clinical.risk.detected`; dispatches PUSH + WHATSAPP |
| `listener/MilestoneReminderListener.java` | `@RabbitListener` on `patient.milestone.due` |
| `filter/CorrelationIdFilter.java` | Correlation ID propagation |
| `model/entity/NotificationLog.java` | JPA: `userId`, `channel`, `eventType`, `status`, `messageBody`, `externalMessageId`, `failureReason`, `sentAt` |
| `model/entity/NotificationChannel.java` | Enum: WHATSAPP, SMS, PUSH, EMAIL |
| `model/entity/NotificationStatus.java` | Enum: PENDING, SENT, DELIVERED, FAILED, **SKIPPED** |

### Flyway Migrations (`reejuven8_ninemo`)

| File | Creates |
|---|---|
| `V1__create_notification_logs.sql` | `notification_logs` table |
| `V2__create_device_tokens.sql` | `device_tokens` table (unique fcm_token, user index) |

---

## 14. Service: `ninemo-community-service`

**Port:** 8086
**Package:** `com.reejuven8.ninemo.community`
**Database:** MongoDB (`reejuven8`)

### Key Files

| File | Role |
|---|---|
| `NinemoCommunityApplication.java` | Entry point |
| `config/WebSocketConfig.java` | STOMP endpoint `/ws/connect` (with SockJS); topic prefix `/topic`; app prefix `/app`; user prefix `/user`; registers auth interceptor on inbound channel |
| `config/StompAuthChannelInterceptor.java` | Validates `Authorization: Bearer` on STOMP CONNECT (sets principal from JWT subject/role); rejects SEND/SUBSCRIBE without principal |
| `security/JwtValidator.java` | HS256 verify via jjwt, shared `jwt.secret` |
| `config/SecurityConfig.java` | Stateless; `/ws/**` permitted at HTTP layer — auth enforced at STOMP layer by the interceptor |
| `filter/CorrelationIdFilter.java` | Correlation ID propagation |
| `model/DueDateClub.java` | MongoDB: `clubName`, `dueDateMonth` (YYYY-MM), `members[]` (userId, alias, joinedAt), `channels[]` (channelId, name, isDefault), `memberCount` |
| `model/ChatMessage.java` | MongoDB: `clubId`, `channelId`, `senderId`, `senderAlias`, `messageType`, `messageBody`, `replyToMessageId`, `imageUrl`, `isDeleted`, `reactions[]`, `sentAt` |
| `model/ContentArticle.java` | MongoDB: `title`, `body`, `summary`, `category`, `tags[]`, `gestationalWeeks[]`, `isPublished`, `publishedAt` |
| `service/DueDateClubService.java` | `join()`: find-or-create club by `dueDateMonth`; auto-provisions 3 default channels (General, Questions, Milestones); join is idempotent |
| `service/ChatMessageService.java` | `send()`: persists message. `getHistory()`: paged by clubId+channelId. `softDelete()`: sets `isDeleted=true` with ownership check |
| `service/ContentService.java` | `listPublished()`, `listByCategory()`, `listByGestationalWeek()` |
| `controller/ChatController.java` | STOMP `@MessageMapping("/chat.send/{clubId}/{channelId}")` → persists → broadcasts to `/topic/club.{clubId}.{channelId}`. Sender identity = authenticated `Principal` (JWT), never the payload |
| `controller/ChatRestController.java` | REST: `GET .../messages` (paged history), `DELETE .../messages/{id}` (soft-delete) |
| `controller/ClubController.java` | REST: `POST /join`, `GET /`, `GET /{id}`, `GET /{id}/channels` |
| `controller/ContentController.java` | REST: `GET /content`, `GET /content/category/{cat}`, `GET /content/week/{week}` |

### Repositories

| Repository | Key Methods |
|---|---|
| `DueDateClubRepository` | `findByDueDateMonth(String)` |
| `ChatMessageRepository` | `findByClubIdAndChannelIdAndIsDeletedFalseOrderBySentAtDesc(Pageable)` |
| `ContentArticleRepository` | `findByIsPublishedTrueOrderByPublishedAtDesc(Pageable)`, `findByIsPublishedTrueAndCategoryOrderByPublishedAtDesc`, `findByIsPublishedTrueAndGestationalWeeksContaining(int)` |

### API Endpoints

```
POST /api/v1/ninemo/community/clubs/join            [X-User-Id header required]
GET  /api/v1/ninemo/community/clubs                 [X-User-Id header required]
GET  /api/v1/ninemo/community/clubs/{id}
GET  /api/v1/ninemo/community/clubs/{id}/channels
GET  /api/v1/ninemo/community/clubs/{clubId}/channels/{channelId}/messages?page=0&size=50
DELETE /api/v1/ninemo/community/clubs/{clubId}/channels/{channelId}/messages/{messageId}
GET  /api/v1/ninemo/community/content?page=0&size=20
GET  /api/v1/ninemo/community/content/category/{category}
GET  /api/v1/ninemo/community/content/week/{gestationalWeek}

WS   /ws/connect (STOMP + SockJS)
  SEND      /app/chat.send/{clubId}/{channelId}
  SUBSCRIBE /topic/club.{clubId}.{channelId}
```

---

## 15. Security Architecture

### JWT Flow

```
Client → POST /api/v1/identity/auth/login
  → identity-abha-service issues:
      accessToken  (15-min TTL, jjwt HS256, signed with ${jwt.secret})
      refreshToken (7-day TTL, UUID stored in Redis auth:session:{userId})

Client → Any API request
  → api-gateway JwtAuthFilter:
      1. Skip public paths
      2. Extract Bearer token
      3. jjwt verify signature → 401 on expired/invalid
      4. Redis.hasKey("auth:blacklist:{jti}") → 401 if blacklisted
      5. Mutate request: add X-User-Id, X-User-Role headers
  → Downstream services trust injected headers
```

### RBAC Roles

| Role | Access |
|---|---|
| `PATIENT` | Own health data only |
| `DOCTOR` | Patient data only with active non-expired `user_consents` record |
| `ADMIN` | Full platform access |

Enforced at: API Gateway (route-level) + each downstream service (method-level, defense in depth).

### ABDM Encryption

| Direction | Algorithm | Library |
|---|---|---|
| Outbound (Aadhaar/OTP → ABDM) | `RSA/ECB/OAEPWithSHA-1AndMGF1Padding` | Bouncy Castle (`RsaEncryptionService`) |
| Inbound (health data from ABDM) | Curve25519 | `nha-abdm-wrapper` |
| ABDM async callbacks | `txnId` cached in Redis with 5-min TTL | Spring Data Redis |

### Security Rules

- Never log Aadhaar numbers, OTPs, or raw JWT payloads
- S3 presigned URLs: 15-minute expiry
- Doctors read patient data only with active non-expired `user_consents` record
- All ABDM sandbox calls: `https://dev.abdm.gov.in`; production: `https://live.abdm.gov.in`

---

## 16. Cross-Database Reference Strategy

PostgreSQL and MongoDB share no native FK relationship. Links are **UUID soft-references**:

| MongoDB Field | References |
|---|---|
| `fhir_resources.patient_id` | `users.id` (PostgreSQL, `reejuven8_identity`) |
| `ninemo_timeline_feed.pregnancy_profile_id` | `pregnancy_profiles.id` (`reejuven8_ninemo`) |
| `symptom_logs.patient_id` | `users.id` |
| `vitals_logs.patient_id` | `users.id` |
| `kick_counter_sessions.patient_id` | `users.id` |
| `contraction_sessions.patient_id` | `users.id` |
| `growth_measurements.child_id` | `child_profiles.id` (`reejuven8_ninemo`) |
| `chat_messages.sender_id` | `users.id` |

Validation is application-level at write time. No DB-level enforcement across services.

---

## 17. Tests & CI

> **JPA + PostgreSQL enum rule:** every `@Enumerated(EnumType.STRING)` field mapped to a native PG enum column **must** also carry `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` — without it inserts fail at runtime (`expression is of type character varying`). Applied to 12 fields across identity/clinical/notification (IS-016).

### Unit Tests — Java (`mvn test`, requires `-Dnet.bytebuddy.experimental=true`, set in Surefire)

| Class | Service | What it covers |
|---|---|---|
| `TimelineServiceTest` (14) | clinical | `computeGestationalWeek` clamping [1,42]; `computeTrimester` boundaries. Parameterized. |
| `WhoLmsTableTest` (11) | clinical | Z-score at/around median, LMS interpolation, percentile conversion + clamp [1,99], girls table, HCZ clamp |
| `KickCounterServiceTest` (3) | clinical | WHO rule: <10 kicks + ≥120 min → concerning + CRITICAL alert |
| `ContractionServiceTest` (4) | clinical | Labor pattern (interval≤300s ∧ duration≥60s) <37w → alert; 38w → none |
| `JwtTokenProviderTest` (6) | identity | generate→validate, userId/role/jti extraction, tampered token rejected |
| `RsaEncryptionServiceTest` (4) | identity | Base64 ciphertext, OAEP randomness (same plaintext ≠ same ciphertext), OTP/Aadhaar payloads |

### Unit Tests — Python (`pytest tests/` in ai-parsing-service)

| File | Covers |
|---|---|
| `test_ner_service.py` | Regex/spaCy hybrid extraction from lab-report text |
| `test_loinc_mapper.py` | Haemoglobin/Hemoglobin/Hb → 718-7, TSH → 3016-3, glucose → 2339-0, unknown → None |
| `test_fhir_mapper.py` | FHIR R4 Observation: resourceType, subject ref, LOINC coding, valueQuantity |

### Integration Tests — Testcontainers (skip without Docker; run in CI)

| Class | Containers | Covers |
|---|---|---|
| `IdentityIntegrationTest` (3) | postgres:16 + redis:7 | Flyway V1–V5 applied; OTP store→5-min TTL→verify→consume→reuse fails; wrong OTP rejected w/o consuming |
| `ClinicalIntegrationTest` (2) | postgres:16 + mongo:7 | Flyway V1–V8 + diet seed; PregnancyProfile JPA insert → SymptomService → Mongo symptom_logs write w/ computed gestational week |
| `HealthDataIntegrationTest` (2) | mongo:7 + rabbitmq:3.13 | fhir_resources CRUD round-trip; DocumentUploadMessage publish→consume via real broker (correlationId intact) |

Local quirk: OrbStack requires `~/.orbstack/config/docker.json` → `{"min-api-version": "1.24"}` (Testcontainers pins client API 1.32 — IS-017). Testcontainers pinned at 1.21.3 in parent POM.

### CI (`.github/workflows/ci.yml`)

| Job | Trigger | Does |
|---|---|---|
| `backend-test` | push/PR | JDK 21 temurin, `mvn clean test` |
| `python-test` | push/PR | Python 3.11, pytest |
| `build-java-images` | main only | Docker build matrix — 6 Java services |
| `build-python-image` | main only | Docker build ai-parsing-service |

---

## 18. Local Development

### Start observability + infrastructure

```bash
cd ninemo-backend/infrastructure
docker compose -f docker-compose.infra.yml up -d
```

### Compile all modules

```bash
cd ninemo-backend
mvn clean compile
# or to include tests:
mvn clean install
```

### Run individual service

```bash
mvn spring-boot:run -pl services/identity-abha-service
```

### Python AI service

```bash
cd services/ai-parsing-service
pip install -r requirements.txt
python -m spacy download en_core_sci_sm  # optional; service falls back to regex without it
uvicorn app.main:app --reload --port 8083
```

### Health checks

```
GET http://localhost:8080/actuator/health   ← api-gateway
GET http://localhost:8081/actuator/health   ← identity-abha-service
GET http://localhost:8082/actuator/health   ← health-data-service
GET http://localhost:8083/health            ← ai-parsing-service (Python)
GET http://localhost:8084/actuator/health   ← ninemo-clinical-service
GET http://localhost:8085/actuator/health   ← notification-service
GET http://localhost:8086/actuator/health   ← ninemo-community-service
```

### Observability UIs

```
http://localhost:9090   ← Prometheus
http://localhost:9411   ← Zipkin
http://localhost:3000   ← Grafana (admin / dev_password)
http://localhost:15672  ← RabbitMQ Management (reejuven8 / dev_password)
```

### Prometheus metrics (per service)

```
GET http://localhost:{port}/actuator/prometheus
```

### Deploy (Kubernetes)

```bash
kubectl apply -f ninemo-backend/k8s/namespace.yaml
kubectl apply -f ninemo-backend/k8s/          # configmap, secrets (fill first!), 8 services, ingress
```

`secrets.yaml` is a template — replace `REPLACE_ME` values or sync via External Secrets Operator before applying.

---

## 19. Communication Patterns (summary)

Full analysis with pros/cons per method: **`Communication_Patterns.md`**. The house rules:

| Who's talking | Method |
|---|---|
| Mobile/web client → backend | REST via api-gateway (always) |
| Client live 2-way (chat) | WebSocket/STOMP `/ws/connect` |
| Client live 1-way (parse progress) | SSE — `GET /health/files/events?s3Key=` |
| Backend → possibly-offline device | FCM push via notification-service |
| Fact happened / audit / fan-out | Kafka |
| Task with retry, backoff, or delay | RabbitMQ (+DLX) |
| Internal hot-path sync / polyglot RPC | gRPC — **deferred**, adopt only on measured need (consent-check hot path, Java↔Python bulk) |
| External party pushes to us | Webhook (idempotent handler + Redis txn correlation) |
| Composed read views | REST with backend aggregation — GraphQL **rejected** (thin-client rule removes its value) |

Per-link rationale for all 6 Kafka/RabbitMQ links: `Communication_Patterns.md §4.2`. The Kafka-vs-RabbitMQ split is intentional and locked (§6 above).

---

## 20. Known Gaps & Planned Changes

Identified in `Communication_Patterns.md §5` and the mobile docs; none block current operation.

| # | Gap | Planned fix | Status |
|---|---|---|---|
| 1 | Client polls upload status — no push equivalent | SSE endpoint `GET /api/v1/health/files/events?s3Key=` (`SseEmitter`, fed by `document.data.parsed` consumer) | ✅ Done (NM-B-160) |
| 2 | STOMP CONNECT not JWT-validated (`/ws/**` is permitAll) — chat spoofing risk | `StompAuthChannelInterceptor` validates Bearer on CONNECT; SEND/SUBSCRIBE require principal; senderId from JWT | ✅ Done (NM-B-161) |
| 3 | No FCM device-token registration endpoint | `POST /api/v1/notifications/devices` + `DELETE /devices?fcmToken=`; orchestrator fan-out per user | ✅ Done (NM-B-162) |
| 4 | Notification delivery status never advances past SENT | Twilio webhook `POST /api/v1/notifications/callbacks/twilio` — signature-verified, monotonic status transitions | ✅ Done (NM-B-164) |
| 5 | Correlation ID stamped by publishers but consumers don't restore it to MDC | `CorrelationMdc` in all 6 Java listeners + Python pipeline passthrough (was silently dropping it — IS-012) | ✅ Done (NM-B-163) |
| 6 | Response envelope inconsistent — some clinical endpoints returned raw DTOs | All 8 clinical controllers wrapped in `ApiResponse<T>` w/ concrete generics; download route moved to `?s3Key=` query param. RN client already expected the envelope — this fixed a latent runtime break (IS-014) | ✅ Done (NM-B-165) |
| 7 | gRPC for internal sync calls | Revisit when a measured hot path exists | Deferred |
| 8 | Integration tests (Testcontainers) | 3 classes / 7 tests, `disabledWithoutDocker=true`. Caught IS-016 (PG enum insert bug, 12 fields fixed with `@JdbcTypeCode(NAMED_ENUM)`) and IS-015 (identity unit tests never compiled) | ✅ Done (NM-B-153) |
| 9 | ABDM is receive-only (HIU) — in-app uploads never reach the ABHA network | HIP registration + care-context linking + consent-bound FHIR data-push (records already stored as FHIR R4; `nha-abdm-wrapper` supports encrypt direction) | ⬜ Future (NM-B-166) |
