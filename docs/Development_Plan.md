# Reejuven8 & NineMo — Master Development Plan

> **Version:** 1.0  
> **Date:** 2026-06-10  
> **Status:** DRAFT — Pending Approval  
> **Repository:** Monorepo (`/Work/Backend/NineMo`)  
> **AI Parsing Service:** Python (FastAPI)  
> **Local Infrastructure:** Docker Compose  

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Architecture Overview](#2-architecture-overview)
3. [Design Patterns & Principles](#3-design-patterns--principles)
4. [Monorepo Structure & Project Scaffold](#4-monorepo-structure--project-scaffold)
5. [Microservice #1 — `identity-abha-service`](#5-microservice-1--identity-abha-service)
6. [Microservice #2 — `health-data-service`](#6-microservice-2--health-data-service)
7. [Microservice #3 — `ai-parsing-service`](#7-microservice-3--ai-parsing-service)
8. [Microservice #4 — `notification-service`](#8-microservice-4--notification-service)
9. [Microservice #5 — `ninemo-clinical-service`](#9-microservice-5--ninemo-clinical-service)
10. [Microservice #6 — `ninemo-community-service`](#10-microservice-6--ninemo-community-service)
11. [Microservice #7 — `api-gateway`](#11-microservice-7--api-gateway)
12. [Database Design — PostgreSQL (Relational Anchor)](#12-database-design--postgresql-relational-anchor)
13. [Database Design — MongoDB (FHIR Data Lake)](#13-database-design--mongodb-fhir-data-lake)
14. [Hybrid Event-Driven Architecture (Kafka + RabbitMQ)](#14-hybrid-event-driven-architecture-kafka--rabbitmq)
15. [Security, Encryption & ABDM Compliance](#15-security-encryption--abdm-compliance)
16. [Caching Strategy (Redis)](#16-caching-strategy-redis)
17. [API Contract Design](#17-api-contract-design)
18. [Error Handling & Resiliency](#18-error-handling--resiliency)
19. [Observability, Logging & Monitoring](#19-observability-logging--monitoring)
20. [Testing Strategy](#20-testing-strategy)
21. [DevOps, CI/CD & Infrastructure](#21-devops-cicd--infrastructure)
22. [Non-Functional Requirements](#22-non-functional-requirements)
23. [Phased Execution Plan](#23-phased-execution-plan)
24. [Risk Register](#24-risk-register)

---

## 1. Executive Summary

Reejuven8 is a cloud-native Electronic Health Record (EHR) platform designed for the fragmented Indian healthcare ecosystem. It acts as a digital "bridge" connecting isolated small clinics, pathology labs, patients, and the government's Ayushman Bharat Digital Mission (ABDM) network.

**NineMo** is the first specialized vertical application under the Reejuven8 umbrella — an intelligent "Vertical Health Locker" dedicated to the maternity and childcare journey, guiding users through the 40 weeks of pregnancy and the first five years of childhood.

### Core Problem Statements
- Indian healthcare is siloed; patient data cannot travel between clinics.
- Doctors see high patient volumes; typing-based EHRs cause burnout and break eye contact.
- Most lab reports arrive as physical PDFs, images, or WhatsApp forwards — not structured FHIR data.
- No existing app provides an *active*, medically-grounded maternity timeline for the Indian context.

### Key Architectural Decisions (Locked)
| Decision | Choice | Rationale |
|---|---|---|
| Repository Structure | **Monorepo** | Shared libraries, unified versioning, single Docker Compose |
| Backend Language | **Java 21 + Spring Boot 3.x** | Native ABDM wrapper compatibility, strong typing for medical logic |
| AI Service Language | **Python + FastAPI** | Best-in-class ML/NLP ecosystem for OCR and NER pipelines |
| Relational DB | **PostgreSQL 16** | ACID compliance for user identity, appointments, consent |
| Document DB | **MongoDB 7** | Native FHIR JSON storage, flexible medical schemas |
| Cache | **Redis 7** | ABDM callback state, drug DB caching, session management |
| Stream Broker | **Apache Kafka** | Immutable audit logs, high-throughput FHIR data streams |
| Task Broker | **RabbitMQ** | Worker queues, delayed messaging, DLX retry patterns |
| API Gateway | **Spring Cloud Gateway** | Centralized routing, rate limiting, JWT validation |
| Object Storage | **AWS S3** | Encrypted medical file storage (PDFs, images) |
| FHIR Library | **HAPI FHIR R4** | Industry-standard Java ↔ FHIR JSON conversion |
| ABDM Library | **nha-abdm-wrapper** | Government open-source Spring Boot wrapper for encryption and callbacks |
| Containerization | **Docker + Docker Compose** (dev), **Kubernetes** (prod) | Polyglot microservices orchestration |

> **Complexity Rating: 2/10** — Decisions are locked; no ambiguity remains.

---

## 2. Architecture Overview

### 2.1 High-Level Architecture ("The Bridge")

```
┌──────────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                                 │
│  ┌─────────────┐  ┌──────────────────┐  ┌─────────────────────────┐ │
│  │ Patient App  │  │ Doctor Dashboard │  │ Legacy Lab Bridge Agent │ │
│  │ (React Native│  │ (React / Next.js)│  │ (Java Swing/FX Desktop) │ │
│  │  / Native)   │  │                  │  │                         │ │
│  └──────┬───────┘  └────────┬─────────┘  └───────────┬─────────────┘ │
│         │                   │                        │               │
└─────────┼───────────────────┼────────────────────────┼───────────────┘
          │                   │                        │
          ▼                   ▼                        ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      API GATEWAY (Spring Cloud Gateway)              │
│         Route → /api/v1/identity/** → identity-abha-service          │
│         Route → /api/v1/health/**   → health-data-service            │
│         Route → /api/v1/ninemo/**   → ninemo-clinical-service        │
│         Route → /ws/**              → ninemo-community-service       │
│         JWT Validation | Rate Limiting | Circuit Breaker             │
└──────────────────────────────┬───────────────────────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          ▼                    ▼                    ▼
┌─────────────────┐ ┌──────────────────┐ ┌──────────────────────┐
│ identity-abha   │ │ health-data      │ │ ninemo-clinical      │
│ -service        │ │ -service         │ │ -service             │
│ (Spring Boot)   │ │ (Spring Boot)    │ │ (Spring Boot)        │
│                 │ │                  │ │                      │
│ • ABHA CRUD     │ │ • FHIR data lake │ │ • Gestational engine │
│ • Consent mgmt  │ │ • S3 file vault  │ │ • Symptom triage     │
│ • RSA/AES crypto│ │ • Record sync    │ │ • WHO growth charts  │
│ • RBAC / JWT    │ │                  │ │ • Pediatric mode     │
└────────┬────────┘ └────────┬─────────┘ └──────────┬───────────┘
         │                   │                      │
         ▼                   ▼                      ▼
┌──────────────────────────────────────────────────────────────────────┐
│               EVENT BUS (Kafka + RabbitMQ Hybrid)                    │
│                                                                      │
│  Kafka Topics:                    RabbitMQ Queues:                    │
│  • abdm.consent.granted           • document.unstructured.uploaded    │
│  • abdm.data.received             • clinical.risk.detected           │
│  • document.data.parsed           • patient.milestone.due            │
└──────────────────────────────┬───────────────────────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          ▼                    ▼                    ▼
┌─────────────────┐ ┌──────────────────┐ ┌──────────────────────┐
│ ai-parsing      │ │ notification     │ │ ninemo-community     │
│ -service        │ │ -service         │ │ -service             │
│ (Python/FastAPI)│ │ (Spring Boot)    │ │ (Spring Boot)        │
│                 │ │                  │ │                      │
│ • OCR (Textract)│ │ • WhatsApp/SMS   │ │ • Due Date Clubs     │
│ • Medical NER   │ │ • Push notifs    │ │ • WebSocket STOMP    │
│ • SLM inference │ │ • Email          │ │ • Real-time chat     │
└─────────────────┘ └──────────────────┘ └──────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          ▼                    ▼                    ▼
┌─────────────────┐ ┌──────────────────┐ ┌──────────────────────┐
│ PostgreSQL 16   │ │ MongoDB 7        │ │ Redis 7              │
│                 │ │                  │ │                      │
│ • users         │ │ • fhir_resources │ │ • ABDM txn IDs       │
│ • patient_      │ │ • ninemo_        │ │ • Drug DB cache      │
│   profiles      │ │   timeline_feed  │ │ • Session tokens     │
│ • doctor_       │ │ • symptom_logs   │ │ • Rate limit counters│
│   profiles      │ │                  │ │                      │
│ • pregnancy_    │ │                  │ │                      │
│   profiles      │ │                  │ │                      │
│ • appointments  │ │                  │ │                      │
│ • user_consents │ │                  │ │                      │
│ • addresses     │ │                  │ │                      │
└─────────────────┘ └──────────────────┘ └──────────────────────┘

                    ┌──────────────────┐
                    │   AWS S3         │
                    │ • Encrypted PDFs │
                    │ • Lab images     │
                    │ • Digital sigs   │
                    └──────────────────┘

                    ┌──────────────────┐
                    │  ABDM Gateway    │
                    │ (Government)     │
                    │ • ABHA creation  │
                    │ • Consent flows  │
                    │ • Health records │
                    └──────────────────┘
```

### 2.2 Bounded Context Map (Domain-Driven Design)

| Bounded Context | Microservice | Domain Model | Relationship |
|---|---|---|---|
| **Identity & Access** | `identity-abha-service` | User, ABHAProfile, Consent | Upstream (provides identity to all) |
| **Clinical Data** | `health-data-service` | FHIRResource, MedicalFile | Core (data backbone) |
| **AI Intelligence** | `ai-parsing-service` | ParsedObservation, OCRResult | Downstream consumer of raw data |
| **Communication** | `notification-service` | Notification, Alert, Channel | Downstream consumer of events |
| **Maternity Logic** | `ninemo-clinical-service` | PregnancyProfile, Timeline, SymptomLog, GrowthMetric | Vertical domain (NineMo) |
| **Community** | `ninemo-community-service` | DueDateClub, ChatMessage, Channel | Vertical domain (NineMo) |
| **Routing** | `api-gateway` | — | Infrastructure (cross-cutting) |

> **Complexity Rating: 6/10** — Multi-service coordination with clear domain boundaries but requires careful inter-service contract management.

---

## 3. Design Patterns & Principles

### 3.1 Architectural Patterns Applied

| Pattern | Where Applied | Implementation Detail |
|---|---|---|
| **Microservices Architecture** | Entire backend | Each service is an independently deployable Spring Boot / FastAPI application with its own database |
| **Domain-Driven Design (DDD)** | Service boundaries | Bounded contexts mapped to services; shared kernel via `common-lib` |
| **Hybrid Event-Driven Architecture** | Inter-service comms | Kafka for streams + RabbitMQ for tasks; no synchronous REST between services |
| **API Gateway Pattern** | Client ingress | Spring Cloud Gateway handles routing, auth, rate limiting |
| **Polyglot Persistence** | Data layer | PostgreSQL (relational) + MongoDB (document) + Redis (cache) + S3 (object) |
| **Strangler Fig** | ABDM integration | Wrap legacy ABDM APIs behind clean internal interfaces; swap implementations as ABDM evolves |
| **Backend-for-Frontend (BFF)** | API Gateway | Gateway tailors responses per client type (mobile vs web) |

### 3.2 Design Patterns Applied

| Pattern | Where Applied | Implementation Detail |
|---|---|---|
| **Saga Pattern (Choreography)** | Distributed transactions | Event-based coordination; each service publishes compensating events on failure |
| **Competing Consumers** | `ai-parsing-service` | Multiple FastAPI worker instances consume from `document.unstructured.uploaded` RabbitMQ queue |
| **Publish/Subscribe** | `document.data.parsed` Kafka topic | Multiple downstream consumers (NineMo, analytics, data lake) subscribe to parsed data |
| **Dead Letter Exchange (DLX)** | RabbitMQ queues | Failed messages route to DLX with exponential backoff retry (1s → 5s → 30s → 5min) |
| **Circuit Breaker** | External API calls | Resilience4j wraps ABDM Gateway, Twilio, AWS Textract calls to prevent cascade failures |
| **Repository Pattern** | All services | Spring Data JPA repositories (PostgreSQL) and Spring Data MongoDB repositories abstract persistence |
| **Factory Pattern** | FHIR resource creation | `FHIRResourceFactory` creates properly structured FHIR Patient, Observation, and Encounter resources via HAPI FHIR |
| **Strategy Pattern** | EDD calculation | `EDDCalculationStrategy` interface with `LMPStrategy`, `UltrasoundStrategy`, `IVFStrategy` implementations |
| **Observer Pattern** | Symptom triage | Rule engine observes incoming symptom logs and triggers evaluation against gestational context |
| **Builder Pattern** | Notification construction | `NotificationBuilder` assembles messages across channels (SMS, WhatsApp, Push, Email) |
| **Template Method** | ABDM API calls | Abstract `AbdmApiTemplate` class handles authentication, encryption, and error handling; subclasses implement specific endpoints |

### 3.3 SOLID Principles Enforcement

| Principle | Enforcement Mechanism |
|---|---|
| **Single Responsibility** | Each microservice owns exactly one bounded context; each class has one reason to change |
| **Open/Closed** | New verticals (e.g., Oncology module) plug into the Reejuven8 core without modifying it |
| **Liskov Substitution** | `EDDCalculationStrategy` implementations are interchangeable; `NotificationChannel` implementations are swappable |
| **Interface Segregation** | Thin, role-specific interfaces (e.g., `PatientRepository` vs `DoctorRepository` vs `ConsentRepository`) |
| **Dependency Inversion** | Services depend on abstractions (`EncryptionService` interface), not concrete crypto implementations |

### 3.4 Additional Principles

- **12-Factor App**: Externalized config via environment variables, stateless processes, disposable containers, dev/prod parity
- **CQRS (Light)**: Separate read models (MongoDB aggregation pipelines for summary cards) from write models (PostgreSQL transactional inserts)
- **Hexagonal Architecture (Ports & Adapters)**: Each service has clear ports (interfaces for inbound/outbound) and adapters (REST controllers, Kafka listeners, repository implementations)

> **Complexity Rating: 7/10** — Requires disciplined implementation of multiple patterns; risk of over-engineering if not kept pragmatic.

---

## 4. Monorepo Structure & Project Scaffold

### 4.1 Directory Layout

```
NineMo/
├── docs/                                 # Documentation (this file, FRD, System Design)
│   ├── NineMo_Functional_Requirement.txt
│   ├── Technical_Requirement.txt
│   ├── System_Design.md
│   └── Development_Plan.md               # THIS FILE
│
├── services/                             # All microservices
│   ├── api-gateway/                      # Spring Cloud Gateway
│   │   ├── src/main/java/com/reejuven8/gateway/
│   │   │   ├── config/
│   │   │   │   ├── RouteConfig.java          # Route definitions
│   │   │   │   ├── RateLimitConfig.java       # Redis-based rate limiter
│   │   │   │   ├── CorsConfig.java            # CORS policies
│   │   │   │   └── SecurityConfig.java        # JWT filter chain
│   │   │   └── filter/
│   │   │       ├── JwtAuthFilter.java         # Global JWT validation
│   │   │       └── RequestLoggingFilter.java  # Structured request logging
│   │   ├── src/main/resources/
│   │   │   └── application.yml
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── identity-abha-service/            # Identity & ABHA
│   │   ├── src/main/java/com/reejuven8/identity/
│   │   │   ├── IdentityAbhaApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   ├── KafkaProducerConfig.java
│   │   │   │   └── AbdmConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AbhaController.java        # POST /api/v1/identity/abha/scan
│   │   │   │   ├── CallbackController.java    # Receives ABDM async callbacks
│   │   │   │   ├── ConsentController.java     # Consent grant/revoke endpoints
│   │   │   │   └── AuthController.java        # Login, register, refresh token
│   │   │   ├── service/
│   │   │   │   ├── AbhaService.java
│   │   │   │   ├── ConsentService.java
│   │   │   │   ├── AuthService.java
│   │   │   │   └── TokenService.java
│   │   │   ├── security/
│   │   │   │   ├── RsaEncryptionService.java      # RSA/ECB/OAEPWithSHA-1AndMGF1Padding
│   │   │   │   ├── DiffieHellmanService.java      # Curve25519 key exchange
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   └── AbdmDecryptionService.java     # Wraps nha-abdm-wrapper DecryptionService
│   │   │   ├── model/
│   │   │   │   ├── entity/
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── PatientProfile.java
│   │   │   │   │   ├── DoctorProfile.java
│   │   │   │   │   ├── Address.java
│   │   │   │   │   └── UserConsent.java
│   │   │   │   ├── enums/
│   │   │   │   │   ├── UserRole.java              # PATIENT, DOCTOR, ADMIN
│   │   │   │   │   ├── ConsentStatus.java         # GRANTED, REVOKED, EXPIRED
│   │   │   │   │   ├── AddressType.java           # HOME, CLINIC, BILLING
│   │   │   │   │   └── BiologicalSex.java         # MALE, FEMALE, OTHER
│   │   │   │   └── dto/
│   │   │   │       ├── AbhaScanRequest.java
│   │   │   │       ├── AbhaProfileResponse.java
│   │   │   │       ├── ConsentRequest.java
│   │   │   │       ├── LoginRequest.java
│   │   │   │       └── TokenResponse.java
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── PatientProfileRepository.java
│   │   │   │   ├── DoctorProfileRepository.java
│   │   │   │   └── UserConsentRepository.java
│   │   │   ├── event/
│   │   │   │   ├── ConsentGrantedEvent.java
│   │   │   │   ├── AbdmDataReceivedEvent.java
│   │   │   │   └── EventPublisher.java
│   │   │   └── exception/
│   │   │       ├── AbdmCommunicationException.java
│   │   │       ├── EncryptionException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   ├── src/main/resources/
│   │   │   ├── application.yml
│   │   │   └── db/migration/                     # Flyway migrations
│   │   │       ├── V1__create_users_table.sql
│   │   │       ├── V2__create_patient_profiles.sql
│   │   │       ├── V3__create_doctor_profiles.sql
│   │   │       ├── V4__create_addresses.sql
│   │   │       └── V5__create_user_consents.sql
│   │   ├── src/test/java/com/reejuven8/identity/
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── health-data-service/              # FHIR Data Lake & File Vault
│   │   ├── src/main/java/com/reejuven8/healthdata/
│   │   │   ├── HealthDataApplication.java
│   │   │   ├── config/
│   │   │   │   ├── MongoConfig.java
│   │   │   │   ├── S3Config.java
│   │   │   │   ├── KafkaConsumerConfig.java
│   │   │   │   └── RabbitConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── HealthRecordController.java
│   │   │   │   └── FileUploadController.java
│   │   │   ├── service/
│   │   │   │   ├── FhirResourceService.java
│   │   │   │   ├── FileStorageService.java
│   │   │   │   └── RecordSyncService.java
│   │   │   ├── model/
│   │   │   │   ├── document/
│   │   │   │   │   └── FhirResource.java         # MongoDB document
│   │   │   │   ├── dto/
│   │   │   │   │   ├── UploadRequest.java
│   │   │   │   │   └── FhirResourceResponse.java
│   │   │   │   └── factory/
│   │   │   │       └── FHIRResourceFactory.java   # HAPI FHIR factory
│   │   │   ├── repository/
│   │   │   │   └── FhirResourceRepository.java    # Spring Data MongoDB
│   │   │   ├── listener/
│   │   │   │   ├── ConsentGrantedListener.java    # @KafkaListener
│   │   │   │   └── AbdmDataReceivedListener.java  # @KafkaListener
│   │   │   └── publisher/
│   │   │       └── DocumentUploadedPublisher.java  # RabbitMQ publisher
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── ai-parsing-service/               # Python FastAPI — OCR & NER
│   │   ├── app/
│   │   │   ├── main.py                           # FastAPI app entry
│   │   │   ├── config.py                         # Environment config
│   │   │   ├── routers/
│   │   │   │   └── parse_router.py               # HTTP endpoints (health checks)
│   │   │   ├── services/
│   │   │   │   ├── ocr_service.py                # AWS Textract integration
│   │   │   │   ├── ner_service.py                # Medical NER pipeline
│   │   │   │   ├── fhir_mapper.py                # Map extracted entities → FHIR JSON
│   │   │   │   └── scribe_service.py             # Audio → structured notes (SLM)
│   │   │   ├── consumers/
│   │   │   │   └── rabbitmq_consumer.py          # document.unstructured.uploaded listener
│   │   │   ├── producers/
│   │   │   │   └── kafka_producer.py             # document.data.parsed publisher
│   │   │   ├── models/
│   │   │   │   ├── parsed_observation.py
│   │   │   │   └── ocr_result.py
│   │   │   └── utils/
│   │   │       ├── s3_client.py
│   │   │       └── loinc_mapper.py               # Map keywords → LOINC codes
│   │   ├── tests/
│   │   ├── requirements.txt
│   │   ├── Dockerfile
│   │   └── pyproject.toml
│   │
│   ├── notification-service/             # Omnichannel Notifications
│   │   ├── src/main/java/com/reejuven8/notification/
│   │   │   ├── NotificationApplication.java
│   │   │   ├── config/
│   │   │   │   ├── RabbitConfig.java
│   │   │   │   └── TwilioConfig.java
│   │   │   ├── listener/
│   │   │   │   ├── ClinicalRiskListener.java      # @RabbitListener
│   │   │   │   └── MilestoneReminderListener.java  # @RabbitListener
│   │   │   ├── service/
│   │   │   │   ├── NotificationOrchestrator.java
│   │   │   │   ├── WhatsAppService.java           # Twilio/Gupshup integration
│   │   │   │   ├── SmsService.java
│   │   │   │   ├── PushNotificationService.java   # FCM integration
│   │   │   │   └── EmailService.java
│   │   │   ├── model/
│   │   │   │   ├── Notification.java
│   │   │   │   ├── NotificationChannel.java       # Interface (Strategy)
│   │   │   │   └── NotificationBuilder.java       # Builder pattern
│   │   │   └── exception/
│   │   │       └── DeliveryFailedException.java
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── ninemo-clinical-service/          # NineMo Maternity Logic
│   │   ├── src/main/java/com/reejuven8/ninemo/clinical/
│   │   │   ├── NinemoClinicalApplication.java
│   │   │   ├── config/
│   │   │   │   ├── KafkaConsumerConfig.java
│   │   │   │   ├── RabbitProducerConfig.java
│   │   │   │   └── MongoConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── TimelineController.java        # GET /api/v1/ninemo/timeline
│   │   │   │   ├── SummaryCardController.java     # GET /api/v1/ninemo/summary-card
│   │   │   │   ├── SymptomController.java         # POST /api/v1/ninemo/symptoms
│   │   │   │   ├── VitalsController.java          # POST /api/v1/ninemo/vitals
│   │   │   │   ├── KickCounterController.java     # POST /api/v1/ninemo/kick-counter
│   │   │   │   ├── ContractionController.java     # POST /api/v1/ninemo/contractions
│   │   │   │   ├── GrowthController.java          # POST /api/v1/ninemo/child/growth
│   │   │   │   ├── VaccinationController.java     # GET/POST /api/v1/ninemo/child/vaccinations
│   │   │   │   └── DietController.java            # GET /api/v1/ninemo/diet/lookup
│   │   │   ├── service/
│   │   │   │   ├── timeline/
│   │   │   │   │   ├── TimelineService.java
│   │   │   │   │   ├── EDDCalculationStrategy.java       # Interface
│   │   │   │   │   ├── LMPCalculationStrategy.java
│   │   │   │   │   ├── UltrasoundCalculationStrategy.java
│   │   │   │   │   └── IVFCalculationStrategy.java
│   │   │   │   ├── triage/
│   │   │   │   │   ├── SymptomTriageEngine.java          # Rule engine wrapper
│   │   │   │   │   ├── TriageRule.java                   # Rule interface
│   │   │   │   │   ├── PreeclampsiaRule.java             # BP > 140/90 + week context
│   │   │   │   │   ├── AnemiaRule.java                   # Hb < 11 + trimester context
│   │   │   │   │   └── GestationalDiabetesRule.java
│   │   │   │   ├── pediatric/
│   │   │   │   │   ├── ModeTransitionService.java        # Pregnancy → Child mode
│   │   │   │   │   ├── GrowthChartService.java           # WHO Z-score calculation
│   │   │   │   │   ├── VaccinationScheduleService.java   # IAP schedule engine
│   │   │   │   │   └── DevelopmentalMilestoneService.java
│   │   │   │   ├── SummaryCardService.java
│   │   │   │   ├── VitalsService.java
│   │   │   │   ├── KickCounterService.java
│   │   │   │   └── DietLookupService.java
│   │   │   ├── model/
│   │   │   │   ├── entity/
│   │   │   │   │   └── PregnancyProfile.java      # PostgreSQL entity
│   │   │   │   ├── document/
│   │   │   │   │   ├── TimelineFeed.java           # MongoDB document
│   │   │   │   │   └── SymptomLog.java             # MongoDB document
│   │   │   │   ├── dto/
│   │   │   │   │   ├── TimelineResponse.java
│   │   │   │   │   ├── SummaryCardResponse.java
│   │   │   │   │   ├── SymptomLogRequest.java
│   │   │   │   │   ├── VitalsLogRequest.java
│   │   │   │   │   ├── GrowthInputRequest.java
│   │   │   │   │   └── DietLookupResponse.java
│   │   │   │   └── enums/
│   │   │   │       ├── Trimester.java
│   │   │   │       ├── SeverityFlag.java
│   │   │   │       └── AppointmentStatus.java
│   │   │   ├── repository/
│   │   │   │   ├── PregnancyProfileRepository.java  # Spring Data JPA
│   │   │   │   ├── TimelineFeedRepository.java       # Spring Data MongoDB
│   │   │   │   └── SymptomLogRepository.java          # Spring Data MongoDB
│   │   │   ├── listener/
│   │   │   │   └── ParsedDataListener.java           # @KafkaListener for document.data.parsed
│   │   │   └── publisher/
│   │   │       ├── ClinicalRiskPublisher.java         # RabbitMQ
│   │   │       └── MilestoneReminderPublisher.java    # RabbitMQ (delayed)
│   │   ├── src/main/resources/
│   │   │   ├── application.yml
│   │   │   ├── db/migration/
│   │   │   │   └── V1__create_pregnancy_profiles.sql
│   │   │   └── data/
│   │   │       ├── iap_vaccination_schedule.json     # Static IAP data
│   │   │       ├── who_growth_data.json              # WHO Z-score reference tables
│   │   │       ├── indian_diet_safety.json           # Food safety ratings
│   │   │       └── gestational_milestones.json       # Week-by-week milestone definitions
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   └── ninemo-community-service/         # Real-Time Chat & Community
│       ├── src/main/java/com/reejuven8/ninemo/community/
│       │   ├── NinemoCommunityApplication.java
│       │   ├── config/
│       │   │   └── WebSocketConfig.java              # STOMP broker config
│       │   ├── controller/
│       │   │   ├── ChatController.java               # WebSocket message handler
│       │   │   └── ClubController.java               # REST: create/join clubs
│       │   ├── service/
│       │   │   ├── DueDateClubService.java
│       │   │   └── ChatMessageService.java
│       │   ├── model/
│       │   │   ├── DueDateClub.java                  # MongoDB document
│       │   │   └── ChatMessage.java                  # MongoDB document
│       │   └── repository/
│       │       ├── DueDateClubRepository.java
│       │       └── ChatMessageRepository.java
│       ├── Dockerfile
│       └── pom.xml
│
├── common-lib/                           # Shared library (DDD Shared Kernel)
│   ├── src/main/java/com/reejuven8/common/
│   │   ├── dto/
│   │   │   ├── ApiResponse.java                      # Standard API response wrapper
│   │   │   ├── ErrorResponse.java
│   │   │   └── PagedResponse.java
│   │   ├── event/
│   │   │   ├── BaseEvent.java                        # Common event fields (id, timestamp, correlationId)
│   │   │   ├── ConsentGrantedEvent.java
│   │   │   ├── AbdmDataReceivedEvent.java
│   │   │   └── DocumentParsedEvent.java
│   │   ├── exception/
│   │   │   ├── BaseException.java
│   │   │   ├── ResourceNotFoundException.java
│   │   │   └── UnauthorizedException.java
│   │   ├── security/
│   │   │   └── JwtClaims.java                        # Shared JWT claims extraction
│   │   └── util/
│   │       ├── DateUtils.java                        # Gestational week calculation helpers
│   │       └── FhirUtils.java                        # FHIR resource helpers
│   └── pom.xml
│
├── infrastructure/                       # Docker & DevOps
│   ├── docker-compose.yml                # Full local stack
│   ├── docker-compose.infra.yml          # Only databases + brokers
│   ├── init-scripts/
│   │   ├── postgres-init.sql             # Create databases for each service
│   │   └── mongo-init.js                 # Create collections and indexes
│   └── kafka/
│       └── create-topics.sh              # Pre-create Kafka topics
│
├── pom.xml                               # Parent POM (Maven multi-module)
├── .gitignore
├── .editorconfig
└── README.md
```

### 4.2 Parent POM Strategy (Maven Multi-Module)

The parent `pom.xml` manages:
- **Dependency Management**: Lock Spring Boot 3.x, Spring Cloud, HAPI FHIR, Resilience4j versions centrally
- **Plugin Management**: Shared Flyway, Jib (Docker builds), Surefire (unit tests), Failsafe (integration tests)
- **Module Declaration**: All services + `common-lib` as child modules
- **Properties**: Java 21, encoding, test config

Each service POM inherits from the parent and declares only service-specific dependencies.

### 4.3 Common Library (`common-lib`)

Shared code that all Java microservices depend on:
- **`BaseEvent`**: Standard event envelope with `eventId`, `timestamp`, `correlationId`, `source`
- **`ApiResponse<T>`**: Standardized REST response wrapper with `status`, `data`, `error`, `timestamp`
- **`JwtClaims`**: Shared utility to extract user ID, role, ABHA address from JWT tokens
- **`DateUtils`**: Gestational week calculation, trimester determination, EDD calculation helpers

> **Complexity Rating: 5/10** — Structurally complex but follows well-documented Maven multi-module conventions.

---

## 5. Microservice #1 — `identity-abha-service`

### 5.1 Purpose
The foundational identity layer. Manages user registration (both ABHA-based and standard), authentication via JWT, RBAC enforcement, and bidirectional communication with the government ABDM Gateway for consent management.

### 5.2 Responsibilities
- ABHA account creation via Aadhaar OTP and Biometric/Face flows
- Async callback handling from ABDM Gateway
- RSA encryption of outbound Aadhaar/OTP payloads
- Curve25519 / Diffie-Hellman decryption of inbound health data
- JWT token generation, validation, and refresh
- RBAC role management (PATIENT, DOCTOR, ADMIN)
- Consent grant/revoke lifecycle management
- Kafka event publishing for consent state changes

### 5.3 External Dependencies
| Dependency | Purpose |
|---|---|
| `nha-abdm-wrapper` | Government-provided Spring Boot library for ABDM encryption/decryption and callback logic |
| Spring Security | JWT filter chain and RBAC |
| Spring Data JPA | PostgreSQL ORM |
| Spring Data Redis | Transaction ID caching during async callbacks |
| Spring Kafka | Event publishing to `abdm.consent.granted` and `abdm.data.received` topics |
| Bouncy Castle | Curve25519 and RSA cryptographic operations |

### 5.4 API Endpoints

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/api/v1/identity/abha/scan` | Initiate ABHA profile creation via QR scan | Public |
| `POST` | `/api/v1/identity/abha/otp/generate` | Request Aadhaar OTP from ABDM | Public |
| `POST` | `/api/v1/identity/abha/otp/verify` | Verify OTP and complete ABHA enrollment | Public |
| `POST` | `/api/v1/identity/auth/login` | User login (phone + OTP) | Public |
| `POST` | `/api/v1/identity/auth/refresh` | Refresh JWT access token | Bearer |
| `GET` | `/api/v1/identity/users/me` | Get current user profile | Bearer |
| `PUT` | `/api/v1/identity/users/me` | Update user profile | Bearer |
| `POST` | `/api/v1/identity/consent/grant` | Grant data access consent to a doctor | Bearer (PATIENT) |
| `POST` | `/api/v1/identity/consent/revoke` | Revoke previously granted consent | Bearer (PATIENT) |
| `GET` | `/api/v1/identity/consent/list` | List all active/expired consents | Bearer |
| `POST` | `/api/v1/identity/callback/consent` | ABDM Gateway consent callback | ABDM Signed |
| `POST` | `/api/v1/identity/callback/data` | ABDM Gateway data delivery callback | ABDM Signed |

### 5.5 Event Publishing

| Event | Broker | Topic/Queue | Payload |
|---|---|---|---|
| Consent Granted | Kafka | `abdm.consent.granted` | `{ patientId, doctorId, consentId, grantedAt, expiresAt }` |
| Health Data Received | Kafka | `abdm.data.received` | `{ patientId, fhirBundle, source, receivedAt }` |

### 5.6 Key Implementation Details

**ABDM Callback Flow:**
1. Frontend calls `POST /api/v1/identity/abha/otp/generate` with encrypted Aadhaar number
2. Service encrypts payload with RSA (ABDM Public Key), sends to `POST /v3/enrollment/request/otp`
3. ABDM returns `200 OK` (acknowledgment only)
4. Service stores `transactionId` in Redis with 5-minute TTL
5. ABDM asynchronously calls back `POST /api/v1/identity/callback/consent` with actual result
6. `CallbackController` matches `transactionId` from Redis, processes result
7. On success: creates `User` in PostgreSQL, publishes Kafka event

**RSA Encryption:**
- Cipher: `RSA/ECB/OAEPWithSHA-1AndMGF1Padding` (default) or `PKCS1Padding` (endpoint-specific)
- Key: ABDM Public Key (fetched from ABDM Sandbox/Production)
- All sensitive fields (Aadhaar, OTP, Mobile) encrypted individually before JSON payload construction

### 5.7 Database Tables Owned
`users`, `patient_profiles`, `doctor_profiles`, `addresses`, `user_consents`

> **Complexity Rating: 9/10** — Government async callback architecture, multi-cipher RSA encryption, Diffie-Hellman key exchange, and compliance requirements make this the hardest service to implement correctly.

---

## 6. Microservice #2 — `health-data-service`

### 6.1 Purpose
The central data backbone. Acts as the gateway to the FHIR data lake (MongoDB) and the encrypted file vault (AWS S3). Consumes identity events from Kafka and publishes parsing requests to RabbitMQ.

### 6.2 Responsibilities
- Store and retrieve FHIR-formatted medical records in MongoDB
- Upload and serve encrypted medical files (PDFs, images) via AWS S3
- Consume `abdm.consent.granted` events to initiate health record fetching from ABDM
- Consume `abdm.data.received` events to persist incoming FHIR bundles
- Publish `document.unstructured.uploaded` events to RabbitMQ when a new file needs OCR parsing
- Smart document categorization by type and medical tags

### 6.3 API Endpoints

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/api/v1/health/records` | Create a new health record (FHIR resource) | Bearer |
| `GET` | `/api/v1/health/records` | List patient's health records (paginated, filterable) | Bearer |
| `GET` | `/api/v1/health/records/{id}` | Get specific health record | Bearer |
| `POST` | `/api/v1/health/files/upload` | Upload a medical file (PDF/image) to S3 | Bearer |
| `GET` | `/api/v1/health/files/{id}/download` | Get presigned S3 URL for file download | Bearer |
| `GET` | `/api/v1/health/records/patient/{patientId}` | Get all records for a patient (doctor view) | Bearer (DOCTOR) |

### 6.4 Event Consumption & Publishing

| Direction | Broker | Topic/Queue | Action |
|---|---|---|---|
| Consume | Kafka | `abdm.consent.granted` | Initiates FHIR record fetch from ABDM network for the consented patient |
| Consume | Kafka | `abdm.data.received` | Persists raw FHIR bundle into MongoDB `fhir_resources` collection |
| Publish | RabbitMQ | `document.unstructured.uploaded` | Triggers OCR parsing when an unstructured file (image/PDF) is uploaded |

### 6.5 MongoDB Collections Owned
`fhir_resources`

### 6.6 Key Implementation Details

**FHIR Resource Storage (HAPI FHIR):**
- All medical data stored as FHIR R4 JSON documents in MongoDB
- Uses HAPI FHIR library's `IParser` to serialize/deserialize Java ↔ FHIR JSON
- `FHIRResourceFactory` creates properly structured `Patient`, `Observation`, `DiagnosticReport`, and `MedicationRequest` resources
- MongoDB indexes on `patient_id`, `resource_type`, `effective_datetime` for efficient querying

**S3 File Storage:**
- Files encrypted at rest (SSE-S3 or SSE-KMS)
- Presigned URLs with 15-minute expiry for downloads
- File metadata stored alongside FHIR resource in MongoDB (`source_file_s3_url` field)

> **Complexity Rating: 6/10** — Standard CRUD with FHIR formatting adds moderate complexity; HAPI FHIR library handles most of the heavy lifting.

---

## 7. Microservice #3 — `ai-parsing-service`

### 7.1 Purpose
The AI intelligence layer. Translates unstructured Indian medical data (photos of lab reports, handwritten prescriptions, voice recordings) into structured FHIR observations.

### 7.2 Responsibilities
- Consume `document.unstructured.uploaded` from RabbitMQ
- Download the source file from S3
- Execute OCR via AWS Textract to extract raw text
- Apply Medical Named Entity Recognition (NER) to identify vitals, medications, diagnoses
- Map extracted entities to LOINC codes (e.g., BPD → `11820-8`, Hemoglobin → `718-7`)
- Format results as FHIR `Observation` resources
- Publish `document.data.parsed` to Kafka for downstream consumption
- AI Medical Scribe: process audio → structured case notes (future phase)

### 7.3 Internal Pipeline

```
S3 File → Textract OCR → Raw Text → Medical NER → Structured Entities → LOINC Mapper → FHIR JSON → Kafka
```

### 7.4 Key Technologies
| Technology | Purpose |
|---|---|
| AWS Textract | OCR for extracting text from images and PDFs |
| spaCy + scispaCy | Medical NER for entity extraction (medications, vitals, diagnoses) |
| Custom regex pipeline | Fallback for extracting known patterns (e.g., `Hemoglobin: 9.2 g/dL`) |
| LOINC code mapping | Static lookup table mapping medical terms → standard LOINC codes |
| Pydantic | Request/response validation models |
| aio-pika | Async RabbitMQ consumer |
| confluent-kafka | Kafka producer |

### 7.5 Competing Consumers Pattern
- Multiple instances of this service can run in parallel
- RabbitMQ distributes messages across instances using round-robin
- Each instance acknowledges only after successful processing
- Failed messages route to DLX with retry headers

### 7.6 LOINC Code Mapping Examples

| Extracted Entity | LOINC Code | FHIR Resource Type |
|---|---|---|
| Hemoglobin (Hb) | `718-7` | Observation |
| Blood Pressure (Systolic) | `8480-6` | Observation |
| Blood Pressure (Diastolic) | `8462-4` | Observation |
| Fetal Heart Rate (FHR) | `55283-6` | Observation |
| Biparietal Diameter (BPD) | `11820-8` | Observation |
| Femur Length (FL) | `11963-6` | Observation |
| Blood Sugar (Fasting) | `1558-6` | Observation |
| TSH | `3016-3` | Observation |

> **Complexity Rating: 8/10** — OCR accuracy on Indian lab reports is unpredictable; Medical NER requires training data; LOINC mapping needs extensive domain knowledge.

---

## 8. Microservice #4 — `notification-service`

### 8.1 Purpose
Omnichannel notification dispatcher. Consumes clinical alerts and milestone reminders from RabbitMQ and delivers them across WhatsApp, SMS, push notifications, and email.

### 8.2 Responsibilities
- Consume `clinical.risk.detected` events → dispatch urgent alerts
- Consume `patient.milestone.due` events → dispatch scheduled reminders
- Channel routing: determine optimal channel based on user preferences and urgency
- Delivery tracking and retry with DLX

### 8.3 Notification Channels

| Channel | Provider | Use Case |
|---|---|---|
| WhatsApp | Twilio / Gupshup API | Urgent clinical alerts, appointment reminders |
| SMS | Twilio / Gupshup API | Fallback for WhatsApp delivery failures |
| Push Notification | Firebase Cloud Messaging (FCM) | Real-time app notifications |
| Email | AWS SES / SendGrid | Non-urgent summaries, weekly digests |

### 8.4 Builder Pattern for Notifications

```java
Notification notification = new NotificationBuilder()
    .withRecipient(patientId)
    .withPriority(Priority.CRITICAL)
    .withTitle("⚠️ High BP Alert")
    .withBody("Your BP of 145/95 is concerning at Week 34. Contact your doctor immediately.")
    .withChannel(Channel.WHATSAPP)
    .withFallback(Channel.SMS)
    .withMetadata("gestational_week", 34)
    .build();
```

### 8.5 DLX Retry Strategy

| Attempt | Delay | Action |
|---|---|---|
| 1st retry | 1 second | Immediate retry |
| 2nd retry | 5 seconds | Short delay |
| 3rd retry | 30 seconds | Medium delay |
| 4th retry | 5 minutes | Extended delay |
| 5th failure | — | Route to dead letter queue; alert ops team |

> **Complexity Rating: 4/10** — Well-understood patterns; main complexity is integrating multiple third-party APIs and handling their failure modes.

---

## 9. Microservice #5 — `ninemo-clinical-service`

### 9.1 Purpose
The heart of the NineMo vertical. Contains all the specialized maternity, triage, and pediatric business logic.

### 9.2 Responsibilities

#### Antenatal (Pregnancy)
- **EDD Calculation Engine**: Calculate Estimated Due Date from LMP, Ultrasound, or IVF date using the Strategy pattern
- **Dynamic Timeline Engine**: Generate 40-week gestational content feed with fetal development data and maternal changes
- **Auto-Scheduling**: Map Indian medical milestones (NT Scan, Anomaly Scan, GTT, TT) to specific dates based on EDD
- **Symptom Triage**: Evaluate daily symptoms against gestational week context to determine severity
- **Vitals Monitoring**: Track weight, blood pressure, and trigger alerts on threshold breaches
- **Kick Counter**: Log fetal movements; track time-to-10-kicks
- **Contraction Timer**: Log contraction start/end times; calculate frequency and duration
- **Indian Diet Lookup**: Searchable pregnancy food safety database
- **Garbh Sanskar**: Trimester-specific yoga and Ayurvedic content delivery

#### Postnatal (Pediatric)
- **Mode Transition**: Automatically switch from pregnancy to child mode upon delivery date entry
- **IAP Vaccination Tracker**: Pre-load Indian immunization schedule from static data
- **WHO Growth Charts**: Calculate Z-scores and percentiles from height/weight/head circumference inputs
- **Developmental Milestones**: Monthly cognitive and physical checklists with alert on missed milestones

#### Clinical Output
- **Doctor's Summary Card**: Single-page aggregation of current week, weight gain, medications, recent labs, allergies

### 9.3 API Endpoints

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/api/v1/ninemo/pregnancy/onboard` | Create pregnancy profile (LMP/US/IVF) | Bearer (PATIENT) |
| `GET` | `/api/v1/ninemo/timeline` | Get current week's timeline feed | Bearer (PATIENT) |
| `GET` | `/api/v1/ninemo/timeline/milestones` | Get auto-scheduled medical milestones | Bearer (PATIENT) |
| `POST` | `/api/v1/ninemo/symptoms` | Log daily symptoms | Bearer (PATIENT) |
| `POST` | `/api/v1/ninemo/vitals/weight` | Log weight measurement | Bearer (PATIENT) |
| `POST` | `/api/v1/ninemo/vitals/bp` | Log blood pressure | Bearer (PATIENT) |
| `POST` | `/api/v1/ninemo/kick-counter` | Submit kick count session | Bearer (PATIENT) |
| `POST` | `/api/v1/ninemo/contractions` | Submit contraction timing data | Bearer (PATIENT) |
| `GET` | `/api/v1/ninemo/summary-card` | Generate doctor's summary card | Bearer (DOCTOR) |
| `GET` | `/api/v1/ninemo/summary-card/{patientId}` | Generate summary for specific patient | Bearer (DOCTOR) |
| `GET` | `/api/v1/ninemo/diet/lookup?query={food}` | Search food safety database | Bearer (PATIENT) |
| `POST` | `/api/v1/ninemo/delivery` | Log delivery date (triggers mode transition) | Bearer (PATIENT) |
| `GET` | `/api/v1/ninemo/child/vaccinations` | Get vaccination schedule | Bearer (PATIENT) |
| `POST` | `/api/v1/ninemo/child/vaccinations/{id}/complete` | Mark vaccination as completed | Bearer (PATIENT) |
| `POST` | `/api/v1/ninemo/child/growth` | Log child growth metrics | Bearer (PATIENT) |
| `GET` | `/api/v1/ninemo/child/growth/chart` | Get WHO growth chart data with Z-scores | Bearer (PATIENT) |

### 9.4 Strategy Pattern — EDD Calculation

```java
public interface EDDCalculationStrategy {
    LocalDate calculateEDD(PregnancyOnboardingRequest request);
    GestationalAge calculateCurrentAge(LocalDate edd);
}

// LMP-based: EDD = LMP + 280 days (Naegele's Rule)
// Ultrasound-based: EDD = Ultrasound Date + (280 - gestational_age_at_scan_days)
// IVF-based: EDD = Transfer Date + 266 days (for Day 5 blastocyst)
```

### 9.5 Rule Engine — Symptom Triage

The triage engine evaluates symptoms using a chain of `TriageRule` implementations:

| Rule | Trigger Condition | Action |
|---|---|---|
| `PreeclampsiaRule` | BP > 140/90 AND (headache OR vision changes) AND week ≥ 20 | CRITICAL → `clinical.risk.detected` |
| `AnemiaRule` | Hemoglobin < 11 g/dL AND Trimester 2-3 | WARNING → suggest iron supplementation |
| `GestationalDiabetesRule` | Fasting glucose > 92 mg/dL AND week 24-28 | WARNING → recommend GTT test |
| `PrematureLaborRule` | Regular contractions AND week < 37 | CRITICAL → `clinical.risk.detected` |
| `ReducedFetalMovementRule` | Kick count < 10 in 2 hours AND week ≥ 28 | CRITICAL → `clinical.risk.detected` |

### 9.6 WHO Z-Score Calculation

```
Z-Score = (Measured Value - Median) / Standard Deviation

Percentile = Φ(Z-Score)  // Standard normal cumulative distribution

Alert Trigger: If weight drops across 2 major percentile lines → RED FLAG
```

Static WHO reference data (median and SD by age in months, sex) loaded from `who_growth_data.json`.

### 9.7 Indian Medical Milestone Schedule

| Gestational Week | Milestone | Type |
|---|---|---|
| 8-10 | Dating Scan (USG) | Ultrasound |
| 11-13 | NT Scan + Double Marker | Screening |
| 16 | Triple/Quad Marker (if indicated) | Blood Test |
| 18-20 | Anomaly Scan (TIFFA) | Ultrasound |
| 24-28 | Glucose Tolerance Test (GTT) | Blood Test |
| 26-28 | Tetanus Toxoid (TT) Injection | Vaccination |
| 28-32 | Growth Scan | Ultrasound |
| 34-36 | Group B Streptococcus (GBS) screening | Swab |
| 36-37 | Presentation Scan | Ultrasound |
| 37+ | Weekly NST (Non-Stress Test) | Monitoring |

### 9.8 MongoDB Collections Accessed
- **Read/Write**: `ninemo_timeline_feed`, `symptom_logs`
- **Read**: `fhir_resources` (for summary card aggregation)

### 9.9 PostgreSQL Tables Accessed
- **Read/Write**: `pregnancy_profiles`
- **Read**: `users`, `appointments`

> **Complexity Rating: 8/10** — Dense domain logic with medical triage rules, multiple calculation strategies, WHO statistical models, and a complex state machine for the pregnancy-to-pediatric transition.

---

## 10. Microservice #6 — `ninemo-community-service`

### 10.1 Purpose
Real-time community engagement. Manages "Due Date Clubs" — anonymous chat groups based on expected delivery month — and serves contextual expert content.

### 10.2 Responsibilities
- Auto-assign users to anonymous chat groups by due date month (e.g., "March 2026 Moms")
- Support threaded topic channels within groups (e.g., "C-Section Recovery", "Name Suggestions")
- Real-time messaging via WebSocket STOMP
- Serve verified medical articles mapped to the user's current gestational week
- Push "data ready" notifications to the frontend when async operations complete

### 10.3 WebSocket Endpoints

| Endpoint | Direction | Description |
|---|---|---|
| `/ws/connect` | Client → Server | STOMP WebSocket handshake |
| `/app/chat.send` | Client → Server | Send message to a channel |
| `/topic/club.{clubId}` | Server → Client | Subscribe to club messages |
| `/user/queue/notifications` | Server → Client | Private user notifications |

### 10.4 REST Endpoints

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `GET` | `/api/v1/ninemo/community/clubs` | List user's clubs | Bearer |
| `POST` | `/api/v1/ninemo/community/clubs/join` | Join/create a due date club | Bearer |
| `GET` | `/api/v1/ninemo/community/clubs/{id}/channels` | List channels in a club | Bearer |
| `GET` | `/api/v1/ninemo/community/content` | Get week-appropriate articles | Bearer |

### 10.5 MongoDB Collections Owned
`due_date_clubs`, `chat_messages`

> **Complexity Rating: 5/10** — WebSocket setup with STOMP is well-documented in Spring Boot; main challenge is message persistence and scaling WebSocket connections.

---

## 11. Microservice #7 — `api-gateway`

### 11.1 Purpose
Single entry point for all client requests. Handles routing, authentication, rate limiting, CORS, and request logging.

### 11.2 Route Definitions

| Route Predicate | Target Service | Strip Prefix | Notes |
|---|---|---|---|
| `/api/v1/identity/**` | `identity-abha-service:8081` | No | Auth endpoints, ABHA, consent |
| `/api/v1/health/**` | `health-data-service:8082` | No | FHIR records, file uploads |
| `/api/v1/ninemo/**` | `ninemo-clinical-service:8083` | No | All NineMo REST endpoints |
| `/api/v1/ninemo/community/**` | `ninemo-community-service:8084` | No | Community REST endpoints |
| `/ws/**` | `ninemo-community-service:8084` | No | WebSocket passthrough |

### 11.3 Cross-Cutting Concerns

| Concern | Implementation |
|---|---|
| **JWT Validation** | Global filter validates Bearer tokens; extracts user claims; passes as headers to downstream services |
| **Rate Limiting** | Redis-based sliding window; configurable per-route (e.g., 100 req/min for standard, 10 req/min for ABHA onboarding) |
| **CORS** | Configurable allowed origins for web and mobile clients |
| **Circuit Breaker** | Resilience4j per-route circuit breakers to isolate service failures |
| **Request Logging** | Structured JSON logging of method, path, status, latency |
| **Load Balancing** | Spring Cloud LoadBalancer for service discovery (or direct host in Docker Compose) |

> **Complexity Rating: 4/10** — Spring Cloud Gateway is mature; configuration-driven rather than code-heavy.

---

## 12. Database Design — PostgreSQL (Relational Anchor)

### 12.1 Schema: `reejuven8_identity` (Owned by `identity-abha-service`)

#### Table: `users`
| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK, DEFAULT gen_random_uuid() | Global identifier |
| `first_name` | VARCHAR(100) | NOT NULL | |
| `middle_name` | VARCHAR(100) | NULLABLE | |
| `last_name` | VARCHAR(100) | NOT NULL | |
| `abha_address` | VARCHAR(255) | UNIQUE | e.g., `patient@abdm` |
| `phone_number` | VARCHAR(15) | UNIQUE, NOT NULL | Primary OTP channel |
| `email_id` | VARCHAR(255) | UNIQUE, NULLABLE | |
| `password_hash` | VARCHAR(255) | NULLABLE | Bcrypt hash (for non-ABHA auth) |
| `role` | ENUM | NOT NULL | `PATIENT`, `DOCTOR`, `ADMIN` |
| `profile_picture_url` | VARCHAR(500) | NULLABLE | S3 presigned URL |
| `is_active` | BOOLEAN | DEFAULT TRUE | Soft delete flag |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() | Audit |
| `updated_at` | TIMESTAMPTZ | DEFAULT NOW() | Audit (trigger-updated) |

**Indexes:**
- `idx_users_abha_address` on `abha_address`
- `idx_users_phone_number` on `phone_number`
- `idx_users_role` on `role`

#### Table: `patient_profiles`
| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK | |
| `user_id` | UUID | FK → `users.id`, UNIQUE | Strict 1:1 |
| `date_of_birth` | DATE | NOT NULL | Age calculation for clinical triage |
| `biological_sex` | ENUM | NOT NULL | `MALE`, `FEMALE`, `OTHER` |
| `emergency_contact_name` | VARCHAR(200) | NULLABLE | |
| `emergency_contact_number` | VARCHAR(15) | NULLABLE | |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() | |
| `updated_at` | TIMESTAMPTZ | DEFAULT NOW() | |

#### Table: `doctor_profiles`
| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK | |
| `user_id` | UUID | FK → `users.id`, UNIQUE | Strict 1:1 |
| `medical_license_number` | VARCHAR(50) | UNIQUE, NOT NULL | Legal compliance for e-Rx |
| `specialization` | VARCHAR(100) | NOT NULL | e.g., "Obstetrician" |
| `qualifications` | VARCHAR(200) | NOT NULL | e.g., "MBBS, MD - OBG" |
| `years_of_experience` | INTEGER | NULLABLE | |
| `consultation_fee` | DECIMAL(10,2) | NOT NULL, DEFAULT 0.00 | |
| `bio` | TEXT | NULLABLE | Patient-facing summary |
| `digital_signature_url` | VARCHAR(500) | NULLABLE | S3 URL; legally required for e-Rx |
| `is_accepting_patients` | BOOLEAN | DEFAULT TRUE | |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() | |
| `updated_at` | TIMESTAMPTZ | DEFAULT NOW() | |

#### Table: `addresses`
| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK | |
| `user_id` | UUID | FK → `users.id` | 1:N relationship |
| `address_type` | ENUM | NOT NULL | `HOME`, `CLINIC`, `BILLING` |
| `address_line_1` | VARCHAR(255) | NOT NULL | |
| `address_line_2` | VARCHAR(255) | NULLABLE | |
| `city` | VARCHAR(100) | NOT NULL | |
| `state` | VARCHAR(100) | NOT NULL | |
| `pincode` | VARCHAR(10) | NOT NULL | Geolocation doctor search |
| `country` | VARCHAR(100) | NOT NULL, DEFAULT 'India' | |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() | |
| `updated_at` | TIMESTAMPTZ | DEFAULT NOW() | |

#### Table: `user_consents`
| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK | |
| `patient_id` | UUID | FK → `users.id` | |
| `doctor_id` | UUID | FK → `users.id` | |
| `consent_status` | ENUM | NOT NULL | `GRANTED`, `REVOKED`, `EXPIRED` |
| `abdm_consent_id` | VARCHAR(255) | NULLABLE | Government consent artifact ID |
| `granted_at` | TIMESTAMPTZ | NOT NULL | |
| `expires_at` | TIMESTAMPTZ | NOT NULL | ABDM consents are time-bound |
| `revoked_at` | TIMESTAMPTZ | NULLABLE | |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() | |
| `updated_at` | TIMESTAMPTZ | DEFAULT NOW() | |

**Indexes:**
- `idx_consents_patient_doctor` on `(patient_id, doctor_id)`
- `idx_consents_status` on `consent_status`
- `idx_consents_expires_at` on `expires_at` (for scheduled expiry jobs)

#### Table: `appointments`
| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK | |
| `patient_id` | UUID | FK → `users.id` | |
| `doctor_id` | UUID | FK → `users.id` | |
| `appointment_type` | ENUM | NOT NULL | `ONLINE`, `WALKIN`, `TELEHEALTH` |
| `scheduled_time` | TIMESTAMPTZ | NOT NULL | |
| `duration_minutes` | INTEGER | DEFAULT 30 | |
| `status` | ENUM | NOT NULL | `BOOKED`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW` |
| `notes` | TEXT | NULLABLE | |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() | |
| `updated_at` | TIMESTAMPTZ | DEFAULT NOW() | |

### 12.2 Schema: `reejuven8_ninemo` (Owned by `ninemo-clinical-service`)

#### Table: `pregnancy_profiles`
| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK | |
| `user_id` | UUID | FK → identity.users.id | |
| `lmp_date` | DATE | NULLABLE | Last Menstrual Period |
| `ultrasound_date` | DATE | NULLABLE | |
| `ivf_transfer_date` | DATE | NULLABLE | |
| `edd_date` | DATE | NOT NULL | Calculated from input above |
| `edd_calculation_method` | ENUM | NOT NULL | `LMP`, `ULTRASOUND`, `IVF` |
| `height_cm` | DECIMAL(5,2) | NOT NULL | |
| `pre_pregnancy_weight_kg` | DECIMAL(5,2) | NOT NULL | |
| `baseline_bmi` | DECIMAL(4,1) | NOT NULL | Calculated on onboard |
| `blood_group` | VARCHAR(5) | NOT NULL | e.g., "A+", "O-" |
| `high_risk_flags` | JSONB | NULLABLE | e.g., `["PCOS","Hypothyroidism"]` |
| `is_active` | BOOLEAN | DEFAULT TRUE | |
| `delivery_date` | DATE | NULLABLE | Triggers mode transition |
| `delivery_type` | ENUM | NULLABLE | `NORMAL`, `CAESAREAN`, `ASSISTED` |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() | |
| `updated_at` | TIMESTAMPTZ | DEFAULT NOW() | |

### 12.3 Migration Strategy
- **Tool**: Flyway
- **Naming Convention**: `V{version}__{description}.sql`
- **Execution**: Automatically on service startup (`spring.flyway.enabled=true`)
- **Rollback**: Each migration has a corresponding `U{version}__{description}.sql` undo script

> **Complexity Rating: 5/10** — Standard relational modeling with a few medical-domain nuances (JSONB for flexible risk flags, enum management).

---

## 13. Database Design — MongoDB (FHIR Data Lake)

### 13.1 Collection: `fhir_resources`

```json
{
  "_id": ObjectId,
  "patient_id": "UUID",
  "resource_type": "Observation | DiagnosticReport | MedicationRequest | Encounter",
  "resource_id": "FHIR resource ID",
  "effective_datetime": ISODate,
  "category": "laboratory | vital-signs | imaging",
  "code": {
    "coding": [
      {
        "system": "http://loinc.org",
        "code": "718-7",
        "display": "Hemoglobin"
      }
    ]
  },
  "value_quantity": {
    "value": 11.2,
    "unit": "g/dL",
    "system": "http://unitsofmeasure.org",
    "code": "g/dL"
  },
  "interpretation": "normal | low | high | critical",
  "source": "ABDM | UPLOAD | OCR_PARSED",
  "source_file_s3_url": "s3://reejuven8-health/encrypted/...",
  "raw_fhir_json": { /* Full FHIR R4 resource */ },
  "tags": ["pregnancy", "trimester-2", "blood-test"],
  "parsed_by": "ai-parsing-service" | null,
  "created_at": ISODate,
  "updated_at": ISODate
}
```

**Indexes:**
- `{ patient_id: 1, resource_type: 1, effective_datetime: -1 }` — Primary query pattern
- `{ patient_id: 1, "code.coding.code": 1 }` — Vital trend queries (e.g., all Hb values)
- `{ source: 1 }` — Filter by data source
- `{ tags: 1 }` — Tag-based searches

### 13.2 Collection: `ninemo_timeline_feed`

```json
{
  "_id": ObjectId,
  "pregnancy_profile_id": "UUID",
  "gestational_week": 24,
  "baby_size_comparison": "Size of a corn ear (~30 cm)",
  "baby_weight_grams": 600,
  "maternal_changes": [
    "You may notice Braxton Hicks contractions",
    "Increased back pain is common this week"
  ],
  "maternal_symptoms_expected": ["back_pain", "heartburn", "leg_cramps"],
  "scheduled_milestones": [
    {
      "name": "Glucose Tolerance Test (GTT)",
      "type": "BLOOD_TEST",
      "recommended_week_range": [24, 28],
      "scheduled_date": ISODate,
      "status": "PENDING | COMPLETED | SKIPPED"
    }
  ],
  "diet_tips": ["Increase iron-rich foods", "Avoid papaya"],
  "yoga_routine_id": "trimester_2_week_24",
  "pinned_reports": [ObjectId, ObjectId],
  "created_at": ISODate
}
```

### 13.3 Collection: `symptom_logs`

```json
{
  "_id": ObjectId,
  "patient_id": "UUID",
  "pregnancy_profile_id": "UUID",
  "gestational_week_at_log": 34,
  "trimester": 3,
  "symptoms": ["headache", "blurred_vision", "swelling_hands"],
  "vitals_at_log": {
    "blood_pressure_systolic": 145,
    "blood_pressure_diastolic": 95,
    "weight_kg": 72.5
  },
  "severity_flag": "CRITICAL | WARNING | NORMAL",
  "triage_result": {
    "rules_triggered": ["PreeclampsiaRule"],
    "recommendation": "CONTACT_DOCTOR_IMMEDIATELY",
    "alert_sent": true,
    "alert_channel": "WHATSAPP"
  },
  "logged_at": ISODate,
  "created_at": ISODate
}
```

> **Complexity Rating: 5/10** — Flexible document schemas are straightforward; the complexity lies in properly structuring FHIR-compliant JSON using HAPI FHIR.

---

## 14. Hybrid Event-Driven Architecture (Kafka + RabbitMQ)

### 14.1 Broker Selection Rationale

| Requirement | Kafka | RabbitMQ | Winner |
|---|---|---|---|
| Immutable audit log | ✅ Append-only log | ❌ | Kafka |
| High throughput streams | ✅ | ❌ | Kafka |
| Event replay | ✅ | ❌ | Kafka |
| Worker queue distribution | ❌ | ✅ Competing consumers | RabbitMQ |
| Delayed messaging | ❌ (Complex) | ✅ Native plugin | RabbitMQ |
| Dead letter handling | ❌ (Manual) | ✅ DLX built-in | RabbitMQ |
| Complex routing | ❌ | ✅ Exchange types | RabbitMQ |

### 14.2 Complete Event Topology

| Event | Broker | Producer | Consumer(s) | Payload Summary |
|---|---|---|---|---|
| `abdm.consent.granted` | **Kafka** | `identity-abha-service` | `health-data-service` | `{ patientId, doctorId, consentId, grantedAt, expiresAt }` |
| `abdm.data.received` | **Kafka** | `identity-abha-service` | `health-data-service` | `{ patientId, fhirBundle, source }` |
| `document.unstructured.uploaded` | **RabbitMQ** | `health-data-service` | `ai-parsing-service` (competing) | `{ documentId, s3Url, fileType, patientId }` |
| `document.data.parsed` | **Kafka** | `ai-parsing-service` | `ninemo-clinical-service`, `health-data-service` | `{ patientId, observations[], sourceDocumentId }` |
| `clinical.risk.detected` | **RabbitMQ** | `ninemo-clinical-service` | `notification-service` | `{ patientId, riskType, severity, message, gestationalWeek }` |
| `patient.milestone.due` | **RabbitMQ** | `ninemo-clinical-service` | `notification-service` | `{ patientId, milestone, dueDate, reminderType }` |

### 14.3 Kafka Configuration

| Setting | Value | Rationale |
|---|---|---|
| `replication.factor` | 3 (prod) / 1 (dev) | Durability |
| `min.insync.replicas` | 2 (prod) / 1 (dev) | Consistency |
| `acks` | `all` | No message loss |
| Partitioning key | `patient_id` | Message ordering per patient |
| Consumer group | `{service-name}-group` | One consumer per service |
| Retention | 30 days | Audit replay window |

### 14.4 RabbitMQ Configuration

| Setting | Value | Rationale |
|---|---|---|
| Exchange type | `direct` for worker queues, `fanout` for broadcasts | Routing control |
| Prefetch count | 1 (for OCR tasks) | Fair distribution to competing consumers |
| Message TTL (DLX) | Exponential: 1s → 5s → 30s → 300s | Retry with backoff |
| Max retries | 4 | Avoid infinite loops |
| Dead Letter Queue | `{queue}.dlx` | Capture failed messages for investigation |
| Delayed Message Plugin | Enabled | For scheduling milestone reminders |

### 14.5 Message Ordering Guarantee
- **Kafka**: Partitioning by `patient_id` guarantees chronological processing per patient
- **RabbitMQ**: Single-consumer queues maintain FIFO; competing consumers may reorder across patients (acceptable for independent tasks like OCR)

> **Complexity Rating: 7/10** — Dual-broker topology requires careful configuration; message ordering, DLX retry logic, and saga compensation add engineering overhead.

---

## 15. Security, Encryption & ABDM Compliance

### 15.1 Authentication Flows

**Internal Authentication (JWT):**
1. User authenticates via phone OTP or ABHA scan
2. `identity-abha-service` issues JWT access token (15 min) + refresh token (7 days)
3. API Gateway validates JWT on every request
4. Claims contain: `userId`, `role`, `abhaAddress`

**ABDM Authentication (Bearer Token):**
1. `identity-abha-service` calls ABDM Gateway with `clientId` + `clientSecret`
2. Receives `accessToken` (valid for 30 mins)
3. Token cached in Redis with TTL = 29 minutes
4. All subsequent ABDM API calls include this token as Bearer header

### 15.2 Encryption Matrix

| Data Type | Direction | Algorithm | Key | Cipher |
|---|---|---|---|---|
| Aadhaar Number | Outbound → ABDM | RSA | ABDM Public Key | `RSA/ECB/OAEPWithSHA-1AndMGF1Padding` |
| OTP | Outbound → ABDM | RSA | ABDM Public Key | `PKCS1Padding` (endpoint-specific) |
| Mobile Number | Outbound → ABDM | RSA | ABDM Public Key | `RSA/ECB/OAEPWithSHA-1AndMGF1Padding` |
| Health Data Payload | Inbound ← ABDM | Curve25519 + DH | Generated Key Pair | Diffie-Hellman Key Exchange |
| Files at Rest | S3 | AES-256 | AWS KMS | SSE-KMS |
| JWT Tokens | Internal | HMAC-SHA256 | Server Secret | — |
| Passwords | Storage | Bcrypt | — | 12 rounds |

### 15.3 RBAC Matrix

| Resource | PATIENT | DOCTOR | ADMIN |
|---|---|---|---|
| Own profile CRUD | ✅ | ✅ | ✅ |
| Own health records | ✅ Read | ✅ Read (with consent) | ✅ Read |
| Upload health files | ✅ | ✅ | ✅ |
| Patient's summary card | ❌ | ✅ (with consent) | ✅ |
| Grant/Revoke consent | ✅ | ❌ | ❌ |
| Prescriptions CRUD | ❌ | ✅ | ✅ |
| Appointment management | ✅ Own | ✅ Own | ✅ All |
| User management | ❌ | ❌ | ✅ |

> **Complexity Rating: 9/10** — Multi-algorithm encryption (RSA + Curve25519 + AES), government compliance, async key exchange, and strict RBAC enforcement make this one of the most demanding areas.

---

## 16. Caching Strategy (Redis)

| Cache Key Pattern | TTL | Service | Purpose |
|---|---|---|---|
| `abdm:txn:{transactionId}` | 5 min | `identity-abha-service` | Store ABDM transaction IDs during async callbacks |
| `abdm:token:access` | 29 min | `identity-abha-service` | Cache ABDM Gateway access token |
| `auth:refresh:{userId}` | 7 days | `identity-abha-service` | Refresh token storage |
| `drug:autocomplete:{prefix}` | 24 hours | `health-data-service` | Drug database for e-Prescription autocomplete |
| `gateway:ratelimit:{ip}:{route}` | 1 min | `api-gateway` | Sliding window rate limit counters |
| `ninemo:timeline:{profileId}:{week}` | 1 hour | `ninemo-clinical-service` | Cache rendered timeline feed |

> **Complexity Rating: 3/10** — Standard Redis caching patterns with well-defined TTLs.

---

## 17. API Contract Design

### 17.1 Standard Response Envelope

```json
{
  "status": "success" | "error",
  "data": { /* response payload */ },
  "error": {
    "code": "ABDM_COMMUNICATION_FAILURE",
    "message": "Unable to reach ABDM Gateway. Please retry.",
    "details": []
  },
  "metadata": {
    "timestamp": "2026-06-10T22:30:00Z",
    "requestId": "uuid",
    "version": "v1"
  }
}
```

### 17.2 Pagination

```json
{
  "data": [ /* items */ ],
  "pagination": {
    "page": 1,
    "size": 20,
    "totalElements": 156,
    "totalPages": 8,
    "hasNext": true
  }
}
```

### 17.3 API Versioning
- URL-based: `/api/v1/...`
- All endpoints versioned from day one
- Breaking changes → increment version (`v2`)

### 17.4 OpenAPI / Swagger
- Each service exposes `/swagger-ui.html` and `/v3/api-docs`
- Configured via `springdoc-openapi-starter-webmvc-ui`

> **Complexity Rating: 2/10** — Standardized conventions; implementation is mostly configuration.

---

## 18. Error Handling & Resiliency

### 18.1 Global Exception Handling
Each Spring Boot service implements a `@RestControllerAdvice` `GlobalExceptionHandler`:

| Exception | HTTP Status | Error Code |
|---|---|---|
| `ResourceNotFoundException` | 404 | `RESOURCE_NOT_FOUND` |
| `UnauthorizedException` | 401 | `UNAUTHORIZED` |
| `AccessDeniedException` | 403 | `FORBIDDEN` |
| `AbdmCommunicationException` | 502 | `ABDM_GATEWAY_ERROR` |
| `EncryptionException` | 500 | `ENCRYPTION_FAILURE` |
| `ConstraintViolationException` | 400 | `VALIDATION_ERROR` |
| `DeliveryFailedException` | 500 | `NOTIFICATION_DELIVERY_FAILED` |
| Generic `Exception` | 500 | `INTERNAL_SERVER_ERROR` |

### 18.2 Circuit Breaker (Resilience4j)

| Circuit | Service | Failure Threshold | Wait Duration | Fallback |
|---|---|---|---|---|
| ABDM Gateway | `identity-abha-service` | 5 failures in 10 calls | 30 seconds | Return cached state / "Service temporarily unavailable" |
| AWS Textract | `ai-parsing-service` | 3 failures in 5 calls | 60 seconds | Queue message for later retry |
| Twilio/Gupshup | `notification-service` | 3 failures in 5 calls | 30 seconds | Fallback to next channel (WhatsApp → SMS → Push) |

### 18.3 Saga Pattern (Choreography-Based)
For the document parsing saga:
1. `health-data-service` saves file to S3 → publishes `document.unstructured.uploaded`
2. If `ai-parsing-service` fails → message goes to DLX → retried with backoff
3. If all retries exhausted → message in dead letter queue → ops alert
4. `health-data-service` marks document as `PARSING_FAILED`
5. User notified: "Unable to process your document. Please try re-uploading."

### 18.4 Eventual Consistency Handling
- When a user uploads a document, the frontend receives `202 Accepted` immediately
- The parsed data won't be available until the async event pipeline completes
- Frontend handles this via:
  - Loading states ("Processing your report...")
  - WebSocket push notification when parsing completes
  - Polling fallback (every 5 seconds for 2 minutes)

> **Complexity Rating: 6/10** — Resilience4j and DLX are well-documented but require thoughtful configuration per external dependency.

---

## 19. Observability, Logging & Monitoring

### 19.1 Structured Logging
- **Format**: JSON structured logs (ELK/Loki compatible)
- **Library**: SLF4J + Logback (Spring Boot default)
- **Correlation ID**: Every request generates a `correlationId` propagated through Kafka/RabbitMQ headers
- **Standard Fields**: `timestamp`, `level`, `service`, `correlationId`, `userId`, `method`, `path`, `status`, `durationMs`

### 19.2 Distributed Tracing
- **Tool**: Micrometer Tracing + Zipkin/Jaeger
- **Propagation**: Trace context propagated across HTTP, Kafka, and RabbitMQ via W3C Trace Context headers
- **Sampling**: 10% in production, 100% in development

### 19.3 Metrics
- **Tool**: Micrometer → Prometheus
- **Key Metrics**:
  - `http_server_requests_seconds` — API latency histograms
  - `kafka_consumer_records_lag` — Consumer lag per partition
  - `rabbitmq_queue_messages_ready` — Queue depth
  - `resilience4j_circuitbreaker_state` — Circuit breaker state
  - `jvm_memory_used_bytes` — JVM heap usage
  - Custom: `ninemo_symptom_triage_total` — Triage invocations by severity

### 19.4 Health Checks
- Each service exposes `/actuator/health` with component health indicators
- Docker Compose uses health checks for dependency ordering
- Checks: database connectivity, Redis connectivity, Kafka/RabbitMQ broker availability

### 19.5 Alerting Rules (Proposed)

| Alert | Condition | Severity |
|---|---|---|
| ABDM Gateway Down | Circuit breaker OPEN for > 5 min | CRITICAL |
| Kafka Consumer Lag | Lag > 1000 messages for > 10 min | WARNING |
| RabbitMQ DLQ Buildup | Dead letter queue > 50 messages | CRITICAL |
| API Latency | P99 > 5 seconds | WARNING |
| Service Down | Health check failed for > 30 sec | CRITICAL |

> **Complexity Rating: 5/10** — Spring Boot Actuator + Micrometer provides most of this out of the box; custom metrics and tracing propagation across brokers add moderate effort.

---

## 20. Testing Strategy

### 20.1 Test Pyramid

| Layer | Tool | Coverage Target | Scope |
|---|---|---|---|
| **Unit Tests** | JUnit 5 + Mockito (Java), pytest (Python) | 80%+ line coverage | Service classes, rule engine, calculations |
| **Integration Tests** | Testcontainers + Spring Boot Test | Key flows | Database queries, Kafka/RabbitMQ message flow |
| **Contract Tests** | Spring Cloud Contract | All inter-service APIs | API compatibility between producer ↔ consumer |
| **E2E Tests** | REST Assured / Playwright | Critical paths | Full user journeys through the API Gateway |

### 20.2 Critical Unit Test Scenarios

| Service | Test | Rationale |
|---|---|---|
| `identity-abha-service` | RSA encryption output matches expected ciphertext format | Government compliance |
| `identity-abha-service` | JWT token generation and validation round-trip | Auth correctness |
| `ninemo-clinical-service` | EDD calculation from LMP (Naegele's Rule: LMP + 280 days) | Medical accuracy |
| `ninemo-clinical-service` | Preeclampsia rule triggers at BP > 140/90 in week ≥ 20 | Patient safety |
| `ninemo-clinical-service` | WHO Z-score calculation matches reference tables | Statistical accuracy |
| `ninemo-clinical-service` | Mode transition locks pregnancy timeline on delivery date | State machine correctness |
| `ai-parsing-service` | Hemoglobin extraction from sample lab report text | OCR pipeline accuracy |
| `ai-parsing-service` | LOINC code mapping for common Indian lab terms | Interoperability |

### 20.3 Integration Test Infrastructure
- **Testcontainers**: Spin up PostgreSQL, MongoDB, Redis, Kafka, RabbitMQ in Docker for each test suite
- **WireMock**: Mock ABDM Gateway and Twilio APIs for deterministic testing
- **Embedded Kafka**: For lightweight Kafka integration tests without full Docker

### 20.4 Test Data Strategy
- **Static test data files**: Sample lab report images, FHIR JSON bundles, IAP vaccination schedules
- **Factories**: Builder-pattern test data factories for `User`, `PregnancyProfile`, `FhirResource`
- **Database seeding**: Flyway test migrations with representative data

> **Complexity Rating: 6/10** — Testcontainers setup for the full polyglot stack (Postgres + Mongo + Redis + Kafka + RabbitMQ) is non-trivial; medical accuracy tests require domain expertise.

---

## 21. DevOps, CI/CD & Infrastructure

### 21.1 Local Development Stack (Docker Compose)

```yaml
# infrastructure/docker-compose.infra.yml
services:
  postgres:
    image: postgres:16-alpine
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: reejuven8
      POSTGRES_USER: reejuven8
      POSTGRES_PASSWORD: dev_password
    volumes:
      - ./init-scripts/postgres-init.sql:/docker-entrypoint-initdb.d/init.sql

  mongodb:
    image: mongo:7
    ports: ["27017:27017"]
    environment:
      MONGO_INITDB_DATABASE: reejuven8

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    ports: ["2181:2181"]

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    ports: ["9092:9092"]
    depends_on: [zookeeper]

  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    ports: ["5672:5672", "15672:15672"]
```

### 21.2 CI/CD Pipeline (Proposed — GitHub Actions)

```
Push to main/feature branch
    │
    ├── Stage 1: Build & Test
    │   ├── common-lib: mvn test
    │   ├── identity-abha-service: mvn test + Testcontainers
    │   ├── health-data-service: mvn test + Testcontainers
    │   ├── ninemo-clinical-service: mvn test + Testcontainers
    │   ├── notification-service: mvn test
    │   ├── ninemo-community-service: mvn test
    │   ├── api-gateway: mvn test
    │   └── ai-parsing-service: pytest + coverage
    │
    ├── Stage 2: Contract Tests
    │   └── Spring Cloud Contract verification
    │
    ├── Stage 3: Docker Build
    │   └── Jib builds for Java services, Dockerfile for Python
    │
    ├── Stage 4: Push to Registry
    │   └── Push images to AWS ECR / GitHub Container Registry
    │
    └── Stage 5: Deploy (on merge to main)
        └── Update Kubernetes manifests / Helm charts
```

### 21.3 Environment Strategy

| Environment | Infrastructure | Data | Purpose |
|---|---|---|---|
| **Local** | Docker Compose | Seed data | Developer workstation |
| **Dev** | Kubernetes (small) | Test data | Integration testing |
| **Staging** | Kubernetes (prod-mirror) | Anonymized prod data | Pre-production validation |
| **Production** | Kubernetes (HA) | Real data | Live system |

### 21.4 ABDM Environment Mapping

| Our Environment | ABDM Environment | Base URL |
|---|---|---|
| Local / Dev | Sandbox | `https://dev.abdm.gov.in` |
| Staging | Sandbox | `https://dev.abdm.gov.in` |
| Production | Production | `https://live.abdm.gov.in` |

> **Complexity Rating: 6/10** — Docker Compose for local dev is straightforward; full Kubernetes + CI/CD pipeline adds significant infrastructure complexity.

---

## 22. Non-Functional Requirements

### 22.1 Performance Targets

| Metric | Target | Rationale |
|---|---|---|
| API response time (P50) | < 200ms | Doctor dashboard must feel instant |
| API response time (P99) | < 2 seconds | Maximum tolerable latency |
| Document upload to parsing complete | < 60 seconds | User expects near-real-time OCR results |
| WebSocket message delivery | < 500ms | Real-time chat must feel responsive |
| Timeline feed load | < 1 second | Critical first impression |

### 22.2 Scalability

| Component | Scaling Strategy |
|---|---|
| Spring Boot services | Horizontal pod autoscaling based on CPU/memory |
| `ai-parsing-service` | Scale competing consumers based on RabbitMQ queue depth |
| Kafka | Partition count determines consumer parallelism |
| PostgreSQL | Read replicas for reporting; connection pooling via HikariCP |
| MongoDB | Sharding by `patient_id` if data volume exceeds single-node capacity |
| Redis | Cluster mode for high availability |

### 22.3 Availability

| Component | Target | Strategy |
|---|---|---|
| API Gateway | 99.9% | Multi-instance behind load balancer |
| Core services | 99.9% | At least 2 replicas per service |
| Database | 99.95% | Managed cloud services with automated failover |
| Message brokers | 99.9% | Kafka: replication factor 3; RabbitMQ: mirrored queues |

### 22.4 Data Compliance
- All health data encrypted at rest (AES-256) and in transit (TLS 1.3)
- ABDM-mandated encryption for all government API interactions
- FHIR R4 data standard for interoperability
- Consent-based access: no doctor can see patient data without explicit, time-bound consent
- Audit logging via Kafka immutable logs for all consent state changes
- Data retention policies aligned with Indian healthcare regulations

> **Complexity Rating: 4/10** — Targets are clear; achieving them requires proper infrastructure sizing and monitoring.

---

## 23. Phased Execution Plan

### Phase 0: Foundation (Week 1-2)
**Goal**: Runnable monorepo with local infrastructure.

| Task | Deliverable |
|---|---|
| Initialize Maven multi-module monorepo | Parent POM + module declarations |
| Create `common-lib` with shared DTOs, events, exceptions | Compiled shared library |
| Write `docker-compose.infra.yml` | PostgreSQL, MongoDB, Redis, Kafka, RabbitMQ running locally |
| Create Kafka topic initialization script | All 3 Kafka topics pre-created |
| Create PostgreSQL init script | Schemas for `identity` and `ninemo` databases |
| Create MongoDB init script | Collections with indexes |
| Scaffold all 7 service modules | Runnable Spring Boot / FastAPI apps with health endpoints |

> **Complexity Rating: 4/10**

---

### Phase 1: Identity & Security Core (Week 3-5)
**Goal**: Users can register via ABHA, authenticate via JWT, and manage consent.

| Task | Deliverable |
|---|---|
| Implement Flyway migrations for all PostgreSQL tables | `V1` through `V5` migration scripts |
| Build `User`, `PatientProfile`, `DoctorProfile` JPA entities | Mapped entities with validation |
| Implement RSA encryption service | Outbound Aadhaar/OTP encryption |
| Implement Curve25519 decryption service (via `nha-abdm-wrapper`) | Inbound health data decryption |
| Build `AbhaController` + `AbhaService` | ABHA QR scan onboarding endpoint |
| Build `CallbackController` | ABDM async callback receiver |
| Integrate Redis for transaction ID caching | Callback state management |
| Build `AuthController` + JWT token provider | Login, token refresh endpoints |
| Implement Spring Security filter chain with RBAC | Role-based access enforcement |
| Build `ConsentController` + `ConsentService` | Grant/revoke consent + Kafka publishing |
| Write unit tests for encryption and auth | >80% coverage on security layer |

> **Complexity Rating: 9/10**

---

### Phase 2: Data Lake & File Vault (Week 6-7)
**Goal**: Health records can be stored, retrieved, and searched. Files upload to S3.

| Task | Deliverable |
|---|---|
| Configure MongoDB connections and collections | Spring Data MongoDB repositories |
| Implement `FHIRResourceFactory` using HAPI FHIR | FHIR R4 resource creation |
| Build `HealthRecordController` CRUD endpoints | REST API for health records |
| Build `FileUploadController` + S3 integration | Presigned upload/download URLs |
| Implement `ConsentGrantedListener` (Kafka consumer) | Auto-fetch records on consent |
| Implement `AbdmDataReceivedListener` (Kafka consumer) | Persist FHIR bundles |
| Implement `DocumentUploadedPublisher` (RabbitMQ) | Trigger OCR on file upload |
| Write integration tests with Testcontainers | MongoDB + Kafka test flows |

> **Complexity Rating: 6/10**

---

### Phase 3: AI Parsing Pipeline (Week 8-10)
**Goal**: Uploaded lab reports are automatically parsed into structured FHIR observations.

| Task | Deliverable |
|---|---|
| Scaffold FastAPI project with async RabbitMQ consumer | Running Python service |
| Integrate AWS Textract SDK | OCR text extraction |
| Build Medical NER pipeline (spaCy + custom patterns) | Entity extraction from raw text |
| Build LOINC code mapper | Structured mapping to standard codes |
| Build FHIR JSON formatter | Output valid FHIR Observation JSON |
| Implement Kafka producer for `document.data.parsed` | Publish parsed results |
| Write pytest tests with sample lab reports | Pipeline accuracy validation |
| Configure competing consumers (multiple instances) | Horizontal scaling readiness |

> **Complexity Rating: 8/10**

---

### Phase 4: NineMo Clinical Core (Week 11-14)
**Goal**: The maternity timeline, symptom triage, and clinical monitoring tools are functional.

| Task | Deliverable |
|---|---|
| Build `PregnancyProfile` entity + Flyway migration | PostgreSQL table |
| Implement EDD calculation strategies (LMP, Ultrasound, IVF) | Strategy pattern classes |
| Build `TimelineService` + static gestational data | 40-week content feed |
| Implement auto-scheduling engine for Indian milestones | Milestone calendar generation |
| Build `SymptomTriageEngine` with clinical rules | Preeclampsia, Anemia, GDM rules |
| Implement vitals logging (weight, BP) + threshold alerting | Real-time health monitoring |
| Build kick counter and contraction timer services | Third-trimester tools |
| Implement `SummaryCardService` with MongoDB aggregation | Doctor's flash card generation |
| Implement Indian diet safety lookup | Food query endpoint |
| Build `ParsedDataListener` (Kafka consumer) | Consume parsed FHIR → plot trends |
| Build `ClinicalRiskPublisher` (RabbitMQ) | Alert on high-risk triage results |
| Build `MilestoneReminderPublisher` (RabbitMQ delayed) | Schedule milestone reminders |
| Write comprehensive unit tests for all triage rules | Medical accuracy validation |

> **Complexity Rating: 8/10**

---

### Phase 5: Pediatric Mode & Notifications (Week 15-17)
**Goal**: Postnatal transition works; notifications flow across all channels.

| Task | Deliverable |
|---|---|
| Implement `ModeTransitionService` | Pregnancy → Child mode state machine |
| Build IAP vaccination schedule engine | Auto-populated schedule from static data |
| Implement WHO Z-score growth chart calculations | Percentile plotting |
| Build developmental milestone checklists | Monthly check-ins with alerts |
| Build `notification-service` listeners | Consume clinical.risk.detected + patient.milestone.due |
| Integrate Twilio/Gupshup for WhatsApp + SMS | Delivery channel implementation |
| Integrate FCM for push notifications | Mobile app notifications |
| Implement DLX retry strategy | Resilient delivery with backoff |
| Write E2E test: symptom → triage → alert → WhatsApp | Full pipeline validation |

> **Complexity Rating: 6/10**

---

### Phase 6: Community & API Gateway (Week 18-19)
**Goal**: Real-time chat, content delivery, and unified API routing.

| Task | Deliverable |
|---|---|
| Implement WebSocket STOMP configuration | Real-time messaging infrastructure |
| Build Due Date Club auto-assignment logic | Group users by EDD month |
| Build chat message persistence (MongoDB) | Chat history storage |
| Build contextual content delivery engine | Week-appropriate articles and videos |
| Configure Spring Cloud Gateway routes | Unified API entry point |
| Implement JWT validation filter | Gateway-level auth |
| Implement Redis-based rate limiting | Per-route rate limits |
| Implement Resilience4j circuit breakers | Per-route isolation |

> **Complexity Rating: 5/10**

---

### Phase 7: Polish, Observability & Hardening (Week 20-22)
**Goal**: Production-ready system with monitoring, logging, and comprehensive tests.

| Task | Deliverable |
|---|---|
| Add structured JSON logging across all services | ELK/Loki-ready logs |
| Add correlation ID propagation (HTTP → Kafka → RabbitMQ) | End-to-end traceability |
| Configure Micrometer metrics + Prometheus endpoints | Operational metrics |
| Set up Zipkin/Jaeger distributed tracing | Request flow visualization |
| Configure health checks for all services | Docker Compose + Kubernetes readiness |
| Write Spring Cloud Contract tests for all inter-service APIs | Contract compatibility guarantee |
| Performance testing with realistic load | Latency and throughput baselines |
| Security audit: encryption, RBAC, ABDM compliance review | Compliance sign-off |
| Documentation: API docs (Swagger), architecture diagrams, runbooks | Operational documentation |

> **Complexity Rating: 5/10**

---

## 24. Risk Register

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| ABDM Sandbox instability / API changes | HIGH | HIGH | Mock ABDM Gateway with WireMock for development; abstract behind interface |
| OCR accuracy on Indian lab reports (varied formats, handwriting) | HIGH | MEDIUM | Start with Textract; layer custom regex patterns; allow manual correction |
| Kafka/RabbitMQ message loss during broker failures | LOW | CRITICAL | Kafka `acks=all` + replication; RabbitMQ mirrored queues + DLX |
| Medical triage rule errors leading to false alerts | MEDIUM | CRITICAL | Extensive unit testing with edge cases; review rules with medical professionals |
| FHIR schema complexity leading to data inconsistencies | MEDIUM | MEDIUM | Use HAPI FHIR library for validation; integration tests against sample bundles |
| Monorepo build times increasing as services grow | MEDIUM | LOW | Maven incremental builds; consider service-specific CI pipeline triggers |
| Government compliance changes to ABDM spec | MEDIUM | HIGH | Abstract ABDM interactions behind interfaces; monitor NHA GitHub for updates |
| WebSocket scaling issues under high concurrent users | MEDIUM | MEDIUM | Sticky sessions; consider Redis-backed STOMP broker for horizontal scaling |

> **Complexity Rating: 3/10** — Risk identification is straightforward; mitigation execution is where the effort lies.

---

## Overall Project Complexity Summary

| Section | Complexity (1-10) |
|---|---|
| Executive Summary & Decisions | 2 |
| Architecture Overview | 6 |
| Design Patterns & Principles | 7 |
| Monorepo Structure | 5 |
| `identity-abha-service` | **9** |
| `health-data-service` | 6 |
| `ai-parsing-service` | **8** |
| `notification-service` | 4 |
| `ninemo-clinical-service` | **8** |
| `ninemo-community-service` | 5 |
| `api-gateway` | 4 |
| PostgreSQL Schema | 5 |
| MongoDB Schema | 5 |
| Hybrid EDA (Kafka + RabbitMQ) | 7 |
| Security & ABDM Compliance | **9** |
| Caching (Redis) | 3 |
| API Contract Design | 2 |
| Error Handling & Resiliency | 6 |
| Observability & Monitoring | 5 |
| Testing Strategy | 6 |
| DevOps & CI/CD | 6 |
| Non-Functional Requirements | 4 |
| **Weighted Average** | **~5.8** |

---

> **Total Estimated Development Timeline:** 22 weeks (5.5 months) for a team of 2-3 backend developers + 1 ML/AI engineer.
>
> **Critical Path:** Phase 0 → Phase 1 (Identity) → Phase 2 (Data) → Phase 3 (AI) → Phase 4 (NineMo Core). Phases 5-7 can partially overlap with Phase 4.
