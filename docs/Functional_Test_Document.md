# NineMo / Reejuven8 — Functional Test Document

> **Version:** 1.0
> **Date:** 2026-06-21
> **Status:** DRAFT
> **Scope:** All 7 backend microservices — functional, integration, event-flow, security, and clinical accuracy tests

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Test ID Convention & Severity Levels](#2-test-id-convention--severity-levels)
3. [Test Environment Setup](#3-test-environment-setup)
4. [IAB — Identity & ABHA Service](#4-iab--identity--abha-service)
5. [HDS — Health Data Service](#5-hds--health-data-service)
6. [APS — AI Parsing Service](#6-aps--ai-parsing-service)
7. [NCS — NineMo Clinical Service](#7-ncs--ninemo-clinical-service)
8. [NS — Notification Service](#8-ns--notification-service)
9. [COM — Community Service](#9-com--community-service)
10. [GW — API Gateway](#10-gw--api-gateway)
11. [EV — Event Flow Integration Tests](#11-ev--event-flow-integration-tests)
12. [SEC — Security & Compliance Tests](#12-sec--security--compliance-tests)
13. [Test Data Catalogue](#13-test-data-catalogue)

---

## 1. Introduction

This document defines the functional acceptance criteria for the NineMo backend. Each test case maps directly to a functional requirement in `NineMo_Functional_Requirement.txt`, a technical user story in `Technical_Requirement.txt`, or a security constraint in `System_Design.md`.

**What this document covers:**
- Happy-path acceptance for every API endpoint
- Negative / boundary cases for all clinical rules
- RBAC enforcement at every protected resource
- Consent gate enforcement across service boundaries
- Event pipeline correctness (Kafka + RabbitMQ flows)
- Medical calculation accuracy (EDD, WHO Z-scores, triage thresholds)
- Security and ABDM compliance validation

**What this document does not cover:**
- Load / performance testing (handled separately)
- UI / mobile testing
- Infrastructure failover (handled by DevOps runbooks)

---

## 2. Test ID Convention & Severity Levels

### 2.1 Test ID Format

```
{SERVICE}-{CATEGORY}-{NNN}
```

| Code | Service |
|---|---|
| `IAB` | identity-abha-service |
| `HDS` | health-data-service |
| `APS` | ai-parsing-service |
| `NCS` | ninemo-clinical-service |
| `NS` | notification-service |
| `COM` | ninemo-community-service |
| `GW` | api-gateway |
| `EV` | event flow (cross-service) |
| `SEC` | security & compliance |

Examples: `IAB-AUTH-001`, `NCS-TRIAGE-003`, `EV-OCR-FLOW-001`

### 2.2 Severity Levels

| Level | Meaning |
|---|---|
| **P0 — Critical** | Patient safety or data loss. Must pass before any deployment. |
| **P1 — High** | Core user journey broken. Must pass before release. |
| **P2 — Medium** | Feature degraded but workaround exists. Target: fixed in sprint. |
| **P3 — Low** | Minor UX or non-blocking edge case. |

---

## 3. Test Environment Setup

### 3.1 Infrastructure (Testcontainers)

All integration tests spin up the following via Testcontainers:

| Container | Image | Purpose |
|---|---|---|
| PostgreSQL | `postgres:16-alpine` | Identity and clinical relational data |
| MongoDB | `mongo:7` | FHIR resources, timeline feed, community |
| Redis | `redis:7-alpine` | Session cache, rate limits, ABDM txn state |
| Kafka | `confluentinc/cp-kafka:7.6.0` | Audit event streams |
| RabbitMQ | `rabbitmq:3.13-management-alpine` | Worker queues, DLX retry |

### 3.2 External Service Mocks (WireMock)

| External API | Mock Stub | Scenarios Covered |
|---|---|---|
| ABDM Gateway | WireMock | OTP generation, consent callbacks, data delivery |
| AWS Textract | WireMock / LocalStack | OCR success, throttling, timeout |
| Twilio / Gupshup | WireMock | WhatsApp success, SMS fallback, delivery failure |
| Firebase FCM | WireMock | Push success, invalid device token |
| AWS S3 | LocalStack | Upload, presigned URL generation |

### 3.3 Test Data Factories

Reusable builder-pattern factories defined in `common-lib/test/`:

- `UserFactory` — creates PATIENT / DOCTOR / ADMIN users
- `PregnancyProfileFactory` — creates profiles with configurable LMP/EDD/week
- `ChildProfileFactory` — creates postnatal child profiles
- `FhirResourceFactory` — creates sample Observation, DiagnosticReport bundles
- `ConsentFactory` — creates granted / revoked / expired consent records

### 3.4 Golden Reference Values for Medical Calculations

| Calculation | Input | Expected Output |
|---|---|---|
| EDD (LMP) | LMP = 2025-09-14 | EDD = 2026-06-21 |
| EDD (IVF Day 5) | Transfer = 2025-10-03 | EDD = 2026-06-25 |
| Gestational week | EDD = 2026-06-21, today = 2026-03-15 | Week 28, Day 3 |
| WHO Z-score (boy, 6mo) | Weight = 7.9 kg | Z = +0.47 (67th percentile) |
| WHO Z-score alert | Weight drops from 75th → 10th percentile | crossed_percentile_lines = 2 |

---

## 4. IAB — Identity & ABHA Service

### 4.1 Authentication & Registration

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `IAB-AUTH-001` | P1 | ABHA OTP generation succeeds | Valid Aadhaar (RSA-encrypted), clientId/clientSecret | `200 OK`, `transactionId` stored in Redis with 5-min TTL, ABDM stub called with encrypted payload |
| `IAB-AUTH-002` | P0 | OTP payload sent to ABDM is RSA-encrypted | Raw Aadhaar number | Intercepted payload is ciphertext, not plaintext; cipher = `RSA/ECB/OAEPWithSHA-1AndMGF1Padding` |
| `IAB-AUTH-003` | P1 | OTP verification succeeds | Valid OTP + matching transactionId in Redis | `200 OK`, user created in `users` table, role = PATIENT, JWT + refresh token returned |
| `IAB-AUTH-004` | P1 | OTP expired | OTP submitted after Redis TTL evicts transactionId | `400 Bad Request`, error code `OTP_EXPIRED` |
| `IAB-AUTH-005` | P1 | ABHA address already registered | Duplicate `abha_address` on second registration attempt | `409 Conflict`, error code `ABHA_ALREADY_REGISTERED` |
| `IAB-AUTH-006` | P1 | Standard login (phone OTP) | Valid phone number | `200 OK`, JWT access token (15-min TTL) + refresh token (7-day TTL) returned |
| `IAB-AUTH-007` | P1 | Token refresh | Valid refresh token | `200 OK`, new access token issued, old access token blacklisted in Redis |
| `IAB-AUTH-008` | P1 | Login with inactive account | `is_active = false` user | `401 Unauthorized`, error code `ACCOUNT_INACTIVE` |
| `IAB-AUTH-009` | P1 | ABHA QR scan onboarding | Valid QR payload from ABDM app | `200 OK`, user profile pre-populated from ABHA demographic data |

### 4.2 User Profile

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `IAB-PROFILE-001` | P1 | Get own profile | Bearer token (PATIENT) | `200 OK`, user + patient_profile fields returned |
| `IAB-PROFILE-002` | P1 | Update own profile | PATCH with new `emergency_contact_number` | `200 OK`, `updated_at` refreshed |
| `IAB-PROFILE-003` | P2 | Doctor profile created with medical license | Doctor registration payload | `doctor_profiles` row with unique `medical_license_number` |
| `IAB-PROFILE-004` | P1 | Attempt to access another user's profile | Patient token + different userId in path | `403 Forbidden` |
| `IAB-PROFILE-005` | P2 | Add multiple addresses | POST address with type HOME, then CLINIC | Both rows created in `addresses` table for same `user_id` |

### 4.3 Consent Management

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `IAB-CONSENT-001` | P0 | Patient grants consent to doctor | PATIENT token, doctorId, expires_at | `201 Created`, `user_consents` row with `consent_status=GRANTED`, `abdm_consent_id` populated |
| `IAB-CONSENT-002` | P0 | Patient revokes consent | PATIENT token, existing consentId | `200 OK`, `consent_status=REVOKED`, `revoked_at` set |
| `IAB-CONSENT-003` | P0 | Doctor cannot grant consent | DOCTOR token calls POST /consent/grant | `403 Forbidden` |
| `IAB-CONSENT-004` | P1 | Consent expiry enforced | `expires_at` in past | Consent treated as EXPIRED; doctor access returns 403 |
| `IAB-CONSENT-005` | P1 | expires_at must be after granted_at | `expires_at = granted_at - 1 day` | `400 Bad Request`, constraint violation |
| `IAB-CONSENT-006` | P1 | List consents | PATIENT token | All consent records for that patient returned with status |
| `IAB-CONSENT-007` | P0 | Consent grant publishes Kafka event | Grant succeeds | `abdm.consent.granted` message published with `{ patientId, doctorId, consentId, grantedAt, expiresAt }` |

### 4.4 ABDM Async Callbacks

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `IAB-CALLBACK-001` | P1 | Valid consent callback matched | ABDM POSTs to `/callback/consent` with known transactionId | `200 OK`, Redis txnId consumed, consent persisted |
| `IAB-CALLBACK-002` | P1 | Callback with unknown transactionId | transactionId not in Redis | `400 Bad Request`, error logged, no DB write |
| `IAB-CALLBACK-003` | P1 | Data delivery callback | ABDM POSTs health data (Curve25519-encrypted) | Data decrypted via `nha-abdm-wrapper`, `abdm.data.received` Kafka event published |
| `IAB-CALLBACK-004` | P2 | Duplicate callback (retry from ABDM) | Same transactionId delivered twice | Second call is idempotent — no duplicate DB row |

---

## 5. HDS — Health Data Service

### 5.1 FHIR Records

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `HDS-FHIR-001` | P1 | Create FHIR Observation | Valid FHIR R4 Observation JSON (Hemoglobin, LOINC `718-7`) | `201 Created`, stored in `fhir_resources` MongoDB collection |
| `HDS-FHIR-002` | P1 | Retrieve own health records | PATIENT token | `200 OK`, paginated list of FHIR resources for that patient |
| `HDS-FHIR-003` | P1 | Doctor retrieves patient records with consent | DOCTOR token + active consent | `200 OK`, patient records returned |
| `HDS-FHIR-004` | P0 | Doctor retrieves patient records without consent | DOCTOR token + no consent row | `403 Forbidden` |
| `HDS-FHIR-005` | P0 | Doctor retrieves with expired consent | Consent `expires_at` in past | `403 Forbidden` |
| `HDS-FHIR-006` | P1 | Filter records by resource_type | `?type=Observation` | Only Observation resources returned |
| `HDS-FHIR-007` | P1 | ABDM data received triggers record storage | `abdm.data.received` Kafka event consumed | FHIR bundle unpacked, each resource stored as separate `fhir_resources` document |

### 5.2 File Upload & Storage

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `HDS-FILES-001` | P1 | Upload PDF lab report | Multipart PDF, PATIENT token | `202 Accepted`, file stored in S3 at `reejuven8-health/{patientId}/lab-reports/{fileId}.pdf` |
| `HDS-FILES-002` | P1 | Upload triggers OCR event | Any unstructured file upload | `document.unstructured.uploaded` RabbitMQ message published with `{ documentId, s3Url, fileType, patientId }` |
| `HDS-FILES-003` | P1 | Download presigned URL generated | GET `/health/files/{id}/download` | `200 OK`, presigned S3 URL with 15-min TTL |
| `HDS-FILES-004` | P2 | Unsupported file type rejected | Upload `.exe` file | `400 Bad Request`, error code `UNSUPPORTED_FILE_TYPE` |
| `HDS-FILES-005` | P2 | Medical files are private | Direct S3 URL without presigned token | `403` from S3 (bucket policy enforces private access) |

---

## 6. APS — AI Parsing Service

### 6.1 OCR Pipeline

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `APS-OCR-001` | P1 | Standard lab report parsed | Clear PDF scan of CBC report | Raw text extracted, Hemoglobin value identified |
| `APS-OCR-002` | P2 | Low-quality image handled | Blurry/handwritten scan | `confidence_score < 0.6` in output, `parsing_metadata` flagged |
| `APS-OCR-003` | P2 | Textract timeout triggers circuit breaker | WireMock stub returns timeout | Circuit breaker opens after 3 failures in 5 calls; message routed to DLX |

### 6.2 Medical NER & LOINC Mapping

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `APS-NER-001` | P1 | Hemoglobin extracted | Text: "Hemoglobin: 9.2 g/dL" | Entity: `{ name: "Hemoglobin", value: 9.2, unit: "g/dL" }` |
| `APS-NER-002` | P1 | Blood pressure extracted | Text: "BP: 145/95 mmHg" | Two entities: systolic 145 + diastolic 95 |
| `APS-NER-003` | P1 | TSH extracted | Text: "TSH: 4.5 mIU/L" | Entity: `{ name: "TSH", value: 4.5, unit: "mIU/L" }` |
| `APS-NER-004` | P1 | Fasting glucose extracted | Text: "FBS: 98 mg/dL" | Entity mapped to blood sugar |
| `APS-LOINC-001` | P1 | Hemoglobin gets correct LOINC | Extracted Hemoglobin entity | `code.coding[0].code = "718-7"`, `system = "http://loinc.org"` |
| `APS-LOINC-002` | P1 | Systolic BP gets correct LOINC | Extracted systolic BP entity | LOINC code `8480-6` |
| `APS-LOINC-003` | P1 | TSH gets correct LOINC | Extracted TSH entity | LOINC code `3016-3` |

### 6.3 FHIR Output & Event Publishing

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `APS-FHIR-OUT-001` | P1 | Valid FHIR R4 Observation produced | Extracted Hemoglobin entity | Output passes HAPI FHIR R4 validation; `resource_type = "Observation"` |
| `APS-KAFKA-001` | P1 | Parsed data event published | Successful parse | `document.data.parsed` Kafka message contains `{ patientId, observations[], sourceDocumentId }` |
| `APS-DLX-001` | P1 | Failed parse retried via DLX | Parsing fails 4 times | Message reaches dead letter queue after exponential backoff (1s → 5s → 30s → 300s) |
| `APS-DLX-002` | P2 | Retry count tracked | Message fails twice then succeeds | `retry_count = 2` in final notification_logs entry |

---

## 7. NCS — NineMo Clinical Service

### 7.1 Pregnancy Onboarding & EDD Calculation

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `NCS-EDD-001` | P0 | EDD from LMP (Naegele's Rule) | LMP = 2025-09-14 | EDD = 2026-06-21 (LMP + 280 days) |
| `NCS-EDD-002` | P0 | EDD from ultrasound | Ultrasound date = 2025-11-01, gestational age at scan = 12w0d | EDD = 2026-06-15 (US date + 280 − 84 days) |
| `NCS-EDD-003` | P0 | EDD from IVF (Day 5 blastocyst) | Transfer date = 2025-10-03 | EDD = 2026-06-25 (transfer + 266 days) |
| `NCS-EDD-004` | P1 | No date provided | All date fields null | `400 Bad Request`, error code `DATE_INPUT_REQUIRED` |
| `NCS-EDD-005` | P1 | BMI calculated on onboard | height_cm = 162, pre_pregnancy_weight_kg = 58 | `baseline_bmi = 22.1` stored in `pregnancy_profiles` |
| `NCS-EDD-006` | P1 | Gestational week calculated correctly | EDD = 2026-06-21, today = 2026-03-15 | Response: `gestationalWeek = 28`, `gestationalDay = 3` |

### 7.2 Dynamic Timeline

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `NCS-TIMELINE-001` | P1 | Timeline feed returned for current week | PATIENT with active pregnancy profile | `200 OK`, `gestationalWeek`, `babyDevelopment`, `maternalChanges`, `scheduledMilestones` all populated |
| `NCS-TIMELINE-002` | P1 | Milestone calendar auto-generated on onboard | Pregnancy profile created with EDD | NT Scan scheduled at EDD − (280−84) days, Anomaly Scan at EDD − (280−140) days, etc. |
| `NCS-TIMELINE-003` | P1 | NT Scan (11-13w) milestone present | Week 12 timeline | `scheduled_milestones` includes `{ name: "NT Scan + Double Marker", type: "SCREENING" }` |
| `NCS-TIMELINE-004` | P1 | GTT (24-28w) milestone present | Week 25 timeline | `scheduled_milestones` includes GTT with `recommended_week_range: [24, 28]` |
| `NCS-TIMELINE-005` | P2 | Timeline cached in Redis | Two GET timeline calls within 1 hour | Second call served from `ninemo:timeline:{profileId}:{week}` Redis key |
| `NCS-TIMELINE-006` | P1 | No active pregnancy profile | PATIENT with no pregnancy profile | `404 Not Found`, error code `NO_ACTIVE_PREGNANCY` |

### 7.3 Symptom Triage — Clinical Rules

> **P0 severity applies to all triage tests.** Incorrect triage output is a patient safety issue.

| Test ID | Severity | Scenario | Symptoms + Context | Expected Result |
|---|---|---|---|---|
| `NCS-TRIAGE-001` | P0 | **Preeclampsia rule triggered** | BP 145/95 + headache + week 28 | `severity=CRITICAL`, `rules_triggered=["PreeclampsiaRule"]`, `recommendation=CONTACT_DOCTOR_IMMEDIATELY`, `alert_sent=true` |
| `NCS-TRIAGE-002` | P0 | Preeclampsia — blurred vision variant | BP 150/100 + blurred_vision + week 32 | Same CRITICAL result as above |
| `NCS-TRIAGE-003` | P0 | **Preeclampsia NOT triggered — BP below threshold** | BP 130/85 + headache + week 28 | `severity=WARNING` or `NORMAL`, PreeclampsiaRule NOT in `rules_triggered` |
| `NCS-TRIAGE-004` | P0 | **Preeclampsia NOT triggered — week too early** | BP 145/95 + headache + week 18 | PreeclampsiaRule NOT triggered (requires week ≥ 20) |
| `NCS-TRIAGE-005` | P0 | **Preeclampsia NOT triggered — no symptoms** | BP 145/95 alone + week 28 | Rule requires BP AND neurological symptom; not triggered by BP alone |
| `NCS-TRIAGE-006` | P0 | **Anemia rule triggered** | Hb = 10.5 g/dL + Trimester 2 | `severity=WARNING`, `rules_triggered=["AnemiaRule"]`, remediation includes iron supplementation tip |
| `NCS-TRIAGE-007` | P0 | Anemia NOT triggered — Hb above threshold | Hb = 11.5 g/dL + Trimester 2 | AnemiaRule NOT triggered (threshold is < 11 g/dL) |
| `NCS-TRIAGE-008` | P0 | Anemia NOT triggered — Trimester 1 | Hb = 10.5 g/dL + Trimester 1 | AnemiaRule NOT triggered (rule applies to T2/T3 only) |
| `NCS-TRIAGE-009` | P0 | **GDM rule triggered** | Fasting glucose = 95 mg/dL + week 26 | `severity=WARNING`, `rules_triggered=["GestationalDiabetesRule"]`, recommendation to take GTT |
| `NCS-TRIAGE-010` | P0 | GDM NOT triggered — outside week range | Fasting glucose = 95 mg/dL + week 20 | GDM rule applies only at weeks 24–28 |
| `NCS-TRIAGE-011` | P0 | **Preterm labor rule triggered** | Regular contractions + week 34 | `severity=CRITICAL`, `rules_triggered=["PrematureLaborRule"]`, alert sent |
| `NCS-TRIAGE-012` | P0 | Preterm labor NOT triggered — term pregnancy | Regular contractions + week 38 | Rule only applies at week < 37; not triggered |
| `NCS-TRIAGE-013` | P0 | **Reduced fetal movement rule triggered** | 6 kicks in 120 min + week 30 | `severity=CRITICAL`, `rules_triggered=["ReducedFetalMovementRule"]`, `clinical.risk.detected` published |
| `NCS-TRIAGE-014` | P0 | Reduced movement NOT triggered — early trimester | 6 kicks + week 26 | Rule requires week ≥ 28 |
| `NCS-TRIAGE-015` | P1 | **Benign symptom — home care tip returned** | Nausea + week 8 | `severity=NORMAL`, `remediation_tips` populated, no alert sent, `alert_sent=false` |
| `NCS-TRIAGE-016` | P1 | Multiple rules triggered simultaneously | BP 145/95 + headache + Hb 10.5 + week 28 | Both PreeclampsiaRule and AnemiaRule in `rules_triggered`; highest severity (CRITICAL) wins |

### 7.4 Vitals Monitoring

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `NCS-VITALS-001` | P0 | High BP alert triggered | Systolic 142, diastolic 92 | `alert_triggered=true`, `clinical.risk.detected` RabbitMQ message published |
| `NCS-VITALS-002` | P1 | Normal BP — no alert | Systolic 118, diastolic 76 | `alert_triggered=false`, no RabbitMQ message |
| `NCS-VITALS-003` | P1 | Weight logged | weight_kg = 68.5 | Stored in `vitals_logs` with `vital_type=WEIGHT`, `gestational_week` auto-calculated |
| `NCS-VITALS-004` | P2 | Bluetooth source recorded | `source=BLUETOOTH_DEVICE`, `device_name="Omron BP"` | Source and device name stored in `vitals_logs` |
| `NCS-VITALS-005` | P2 | Weight trend retrievable | Multiple weight logs across weeks | GET vitals returns time-series ordered by `logged_at` |

### 7.5 Kick Counter & Contraction Timer

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `NCS-KICK-001` | P1 | 10 kicks logged — duration calculated | Session with 10 kick timestamps over 45 min | `total_kicks=10`, `duration_to_10_kicks_minutes=45`, `is_concerning=false` |
| `NCS-KICK-002` | P0 | Fewer than 10 kicks in 2 hours at week ≥ 28 | 7 kicks, session duration 125 min, week 30 | `is_concerning=true`, `clinical.risk.detected` published |
| `NCS-KICK-003` | P1 | Kick concern NOT triggered — early week | 7 kicks, 125 min session, week 26 | `is_concerning=false` (rule requires week ≥ 28) |
| `NCS-CONTRACTION-001` | P1 | Contraction metrics calculated | 3 contractions: durations [45s, 52s, 48s], intervals [480s, 490s] | `average_duration_seconds=48`, `average_interval_seconds=485`, `total_contractions=3` |
| `NCS-CONTRACTION-002` | P0 | Labor pattern detected | Interval < 300s, duration > 60s consistently | `is_labor_pattern=true` |
| `NCS-CONTRACTION-003` | P0 | Preterm labor alert | `is_labor_pattern=true` + week < 37 | `alert_triggered=true`, `clinical.risk.detected` published |

### 7.6 Doctor's Summary Card

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `NCS-SUMMARY-001` | P0 | PATIENT cannot access summary card | PATIENT token calling GET `/summary-card` | `403 Forbidden` |
| `NCS-SUMMARY-002` | P0 | Doctor without consent cannot access | DOCTOR token, no consent record | `403 Forbidden` |
| `NCS-SUMMARY-003` | P1 | Doctor with consent gets full card | DOCTOR token, active consent | `200 OK`, response includes: `gestationalWeek`, `cumulativeWeightGain`, `recentVitals`, `activeMedications`, `recentSymptomLogs` (last 5), `upcomingMilestones` |
| `NCS-SUMMARY-004` | P2 | Summary card cached | Two calls within 15 min | Second call served from `ninemo:summary:{patientId}` Redis key |
| `NCS-SUMMARY-005` | P2 | Cache invalidated on new symptom log | New symptom logged after first summary call | Next summary call fetches fresh data (cache evicted) |

### 7.7 Diet & Food Safety Lookup

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `NCS-DIET-001` | P1 | Avoid food identified | `query=Papaya` | `safety_rating=AVOID`, `medical_reasoning` populated |
| `NCS-DIET-002` | P1 | Safe food identified | `query=Spinach` | `safety_rating=SAFE` |
| `NCS-DIET-003` | P1 | Fuzzy search works | `query=papya` (typo) | Returns Papaya result via pg_trgm index |
| `NCS-DIET-004` | P2 | Hindi name search | `query=पपीता` | Returns Papaya result via `ingredient_name_hindi` |
| `NCS-DIET-005` | P2 | Unknown ingredient | `query=XYZ123` | `404 Not Found` or empty results array |

### 7.8 Postnatal Mode Transition

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `NCS-TRANSITION-001` | P0 | Delivery logged — mode switches | POST `/delivery` with `delivery_date`, `delivery_type=NORMAL` | `pregnancy_profiles.is_active=false`, `delivery_date` set, `child_profiles` row created |
| `NCS-TRANSITION-002` | P0 | IAP vaccination schedule auto-populated | Delivery logged for child born 2026-06-21 | `vaccination_records` rows created for BCG, OPV-0, HepB-0 at birth; DPT-1 at 6 weeks; etc. |
| `NCS-TRANSITION-003` | P1 | Pregnancy timeline locked after delivery | GET `/timeline` after delivery | Returns `410 Gone` or mode-transition response, not antenatal content |
| `NCS-TRANSITION-004` | P1 | Duplicate delivery log rejected | Second POST `/delivery` on same profile | `409 Conflict` |

### 7.9 WHO Growth Charts & Pediatric Monitoring

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `NCS-GROWTH-001` | P0 | Z-score correct for known reference value | Boy, age 6 months, weight = 7.9 kg | `z_scores.weight_for_age ≈ 0.47`, `percentiles.weight_for_age ≈ 67` |
| `NCS-GROWTH-002` | P0 | Z-score = 0 maps to 50th percentile | Input matching WHO median | `percentiles.weight_for_age = 50` |
| `NCS-GROWTH-003` | P0 | Two-percentile-line drop triggers alert | Previous measurement at 75th percentile; new measurement at 8th percentile | `crossed_percentile_lines=2`, `alert_flags` populated |
| `NCS-GROWTH-004` | P1 | Growth chart endpoint returns WHO curves + child data | GET `/child/growth/chart` | Response includes WHO reference curve data points AND child's logged measurements |
| `NCS-GROWTH-005` | P2 | Multiple measurements plotted in order | 3 growth logs at months 3, 6, 9 | Chart data sorted by `measurement_date` ascending |

### 7.10 Vaccination Tracker

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `NCS-VAX-001` | P1 | Mark vaccination complete | POST `/vaccinations/{id}/complete` with `administered_date`, `administered_by` | `status=COMPLETED`, `administered_date` set |
| `NCS-VAX-002` | P1 | Overdue vaccination detected | `scheduled_date` in past + `status=PENDING` | GET vaccinations returns `status=OVERDUE` for that record |
| `NCS-VAX-003` | P1 | Vaccination schedule pre-loaded correctly | Child born 2026-06-21 | BCG scheduled 2026-06-21, OPV-0 scheduled 2026-06-21, DPT-1 scheduled ~2026-08-02 (6 weeks) |
| `NCS-VAX-004` | P2 | Duplicate vaccine dose rejected | POST complete for (child_id, vaccine_name, dose_number) already completed | `409 Conflict` |

---

## 8. NS — Notification Service

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `NS-WHATSAPP-001` | P0 | Clinical risk alert dispatched via WhatsApp | `clinical.risk.detected` consumed with `severity=CRITICAL` | WhatsApp API stub called, `notification_logs` row with `channel=WHATSAPP`, `status=SENT` |
| `NS-SMS-FALLBACK-001` | P0 | SMS fallback on WhatsApp failure | WhatsApp stub returns 503 | SMS attempted next; `notification_logs` shows WHATSAPP=FAILED, SMS=SENT |
| `NS-PUSH-001` | P1 | Milestone push notification dispatched | `patient.milestone.due` consumed | FCM stub called, `notification_logs` row with `channel=PUSH`, `status=DELIVERED` |
| `NS-RETRY-001` | P1 | DLX retry sequence followed | All channels fail on first attempt | Retry after 1s, 5s, 30s, 300s (4 total attempts) |
| `NS-DLQ-001` | P1 | Dead letter queue receives after max retries | 4 retries all fail | Message in `clinical.risk.detected.dlx` queue; ops alert triggered |
| `NS-LOG-001` | P1 | Notification log record complete | Any notification sent | `notification_logs` row with `channel`, `event_type`, `status`, `retry_count`, `external_message_id`, `sent_at` |
| `NS-TIMING-001` | P1 | Milestone reminder fires 7 days before | Milestone scheduled 7 days out | Delayed RabbitMQ message fires at correct time (±1 min) |
| `NS-TIMING-002` | P1 | Milestone reminder fires 1 day before | Milestone scheduled 1 day out | Second reminder fires at T-24h |
| `NS-EMAIL-001` | P2 | Weekly summary email dispatched | Scheduled email event | AWS SES stub called, `channel=EMAIL`, non-urgent delivery |

---

## 9. COM — Community Service

### 9.1 Due Date Clubs

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `COM-CLUB-001` | P1 | User auto-assigned to correct club | Patient with EDD in March 2026 calls POST `/clubs/join` | Assigned to "March 2026 Moms" club; `due_date_clubs.due_date_month = "2026-03"` |
| `COM-CLUB-002` | P1 | Club auto-created for new month | First patient with May 2026 EDD joins | New `due_date_clubs` document created with `club_name = "May 2026 Moms"` |
| `COM-CLUB-003` | P1 | Alias assigned on join | Any patient joins a club | `alias` field populated (e.g. "MomBee_42"), `user_id` not exposed in club member list |
| `COM-CLUB-004` | P2 | Default channels present | New club created | `General` channel auto-created with `is_default=true` |
| `COM-CLUB-005` | P2 | Duplicate join idempotent | Patient calls join twice | No duplicate membership; `200 OK` on second call |

### 9.2 Real-Time Chat (WebSocket)

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `COM-CHAT-001` | P1 | Message sent and persisted | Client sends to `/app/chat.send` with `clubId`, `channelId`, `message_body` | Stored in `chat_messages` MongoDB collection |
| `COM-CHAT-002` | P1 | Message broadcast to subscribers | Second client subscribed to `/topic/club.{clubId}` | Message received within 500ms |
| `COM-CHAT-003` | P1 | Threaded reply stored | Message with `reply_to_message_id` set | `reply_to_message_id` stored; original message retrievable |
| `COM-CHAT-004` | P1 | Unauthenticated WebSocket rejected | Connection attempt without valid JWT | Connection refused at STOMP handshake |
| `COM-CHAT-005` | P2 | Image message stored | `message_type=IMAGE`, `image_url` provided | Image URL stored, `message_type` correct |

### 9.3 Content Delivery

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `COM-CONTENT-001` | P1 | Week-appropriate articles returned | PATIENT at week 28 calls GET `/content` | Only articles with `target_gestational_weeks` containing 28 returned |
| `COM-CONTENT-002` | P1 | Postnatal content after delivery | PATIENT in child mode calls GET `/content` | Articles filtered by `target_postnatal_months`, antenatal articles excluded |
| `COM-CONTENT-003` | P2 | Unpublished articles excluded | `is_published=false` article in DB | Not returned in content feed |

---

## 10. GW — API Gateway

| Test ID | Severity | Scenario | Input | Expected Result |
|---|---|---|---|---|
| `GW-ROUTE-001` | P1 | `/api/v1/identity/**` routed correctly | Request to identity path | Reaches `identity-abha-service:8081` |
| `GW-ROUTE-002` | P1 | `/api/v1/health/**` routed correctly | Request to health path | Reaches `health-data-service:8082` |
| `GW-ROUTE-003` | P1 | `/api/v1/ninemo/**` routed correctly | Request to ninemo path | Reaches `ninemo-clinical-service:8084` |
| `GW-ROUTE-004` | P1 | `/ws/**` routed to community service | WebSocket upgrade request | Reaches `ninemo-community-service:8086` |
| `GW-JWT-001` | P0 | Missing Bearer token rejected | No Authorization header | `401 Unauthorized` |
| `GW-JWT-002` | P0 | Expired JWT rejected | Token with `exp` in past | `401 Unauthorized` |
| `GW-JWT-003` | P0 | Blacklisted JWT rejected | Token JTI present in Redis blacklist | `401 Unauthorized` |
| `GW-JWT-004` | P1 | Valid JWT passes through | Active, valid JWT | Request forwarded; `X-User-Id` and `X-User-Role` headers injected into downstream request |
| `GW-RBAC-001` | P0 | PATIENT blocked from DOCTOR-only endpoint | PATIENT JWT on GET `/ninemo/summary-card/{id}` | `403 Forbidden` |
| `GW-RBAC-002` | P0 | DOCTOR blocked from PATIENT-only endpoint | DOCTOR JWT on POST `/identity/consent/grant` | `403 Forbidden` |
| `GW-RATE-001` | P1 | Standard rate limit enforced | 101 requests/min from same IP | 101st request returns `429 Too Many Requests` |
| `GW-RATE-002` | P1 | ABHA onboarding rate limit (stricter) | 11 requests/min to `/identity/abha/**` | 11th request returns `429 Too Many Requests` |
| `GW-CIRCUIT-001` | P1 | Circuit breaker opens on downstream failure | Target service returns 503 five times | Circuit opens; subsequent requests return `503 Service Unavailable` without hitting downstream |
| `GW-CIRCUIT-002` | P2 | Circuit breaker half-opens and recovers | Service recovers after 30s wait | Circuit transitions OPEN → HALF_OPEN → CLOSED |

---

## 11. EV — Event Flow Integration Tests

These tests span multiple services and verify the complete event pipeline end-to-end.

| Test ID | Severity | Scenario | Steps | Expected Final State |
|---|---|---|---|---|
| `EV-CONSENT-001` | P0 | **Consent-to-record-sync pipeline** | 1. Patient grants consent → 2. `abdm.consent.granted` published → 3. `health-data-service` consumes event → 4. ABDM stub returns FHIR bundle → 5. Bundle stored | `fhir_resources` collection populated with patient's records |
| `EV-OCR-001` | P1 | **File-to-vitals pipeline** | 1. Patient uploads lab report PDF → 2. `document.unstructured.uploaded` published → 3. `ai-parsing-service` downloads from S3, runs OCR → 4. Hemoglobin extracted → 5. `document.data.parsed` published → 6. `ninemo-clinical-service` consumes → 7. Vitals plotted | `vitals_logs` contains new entry with `vital_type=BLOOD_TEST`, Hb value, LOINC code |
| `EV-RISK-001` | P0 | **Symptom-to-WhatsApp pipeline** | 1. Patient logs BP 145/95 + headache at week 28 → 2. PreeclampsiaRule fires → 3. `clinical.risk.detected` published → 4. `notification-service` consumes → 5. WhatsApp dispatched | WhatsApp stub called; `notification_logs` row with `status=DELIVERED`, `event_type=clinical.risk.detected` |
| `EV-MILESTONE-001` | P1 | **Milestone reminder pipeline** | 1. Pregnancy onboarded with EDD → 2. NT Scan milestone scheduled for week 12 → 3. Delayed `patient.milestone.due` message fires 7 days before → 4. `notification-service` sends push | FCM stub called 7 days before milestone date |
| `EV-ABDM-DATA-001` | P1 | **ABDM data delivery pipeline** | 1. ABDM sends health data to `/callback/data` → 2. Curve25519 decryption applied → 3. `abdm.data.received` Kafka event published → 4. `health-data-service` persists FHIR bundle | `fhir_resources` populated; `source=ABDM` |

---

## 12. SEC — Security & Compliance Tests

| Test ID | Severity | Scenario | Verification Method | Expected Result |
|---|---|---|---|---|
| `SEC-RSA-001` | P0 | Aadhaar encrypted before ABDM transit | Intercept outbound request to ABDM stub | Payload body is RSA ciphertext; plaintext Aadhaar not present |
| `SEC-RSA-002` | P0 | Correct RSA cipher used | Inspect encryption service | Cipher = `RSA/ECB/OAEPWithSHA-1AndMGF1Padding` |
| `SEC-CURVE-001` | P0 | Inbound ABDM health data decrypted correctly | Send Curve25519-encrypted payload to callback | Data extracted matches original fixture |
| `SEC-CONSENT-001` | P0 | No consent = no data access | Any DOCTOR request without consent record | `403 Forbidden` across all patient data endpoints |
| `SEC-CONSENT-002` | P0 | Expired consent = no data access | Consent `expires_at` 1 second in past | `403 Forbidden` |
| `SEC-AUDIT-001` | P0 | Consent state change logged to Kafka | REVOKE consent | `abdm.consent.granted` / revoke event in Kafka with timestamp; immutable |
| `SEC-S3-001` | P1 | Medical file presigned URL expires | Generate URL, wait 16 minutes, attempt access | S3 returns `403 AccessDenied` |
| `SEC-NOPHI-001` | P0 | Aadhaar not in logs | Trigger OTP flow | Search application logs for 12-digit sequences — none found |
| `SEC-NOPHI-002` | P0 | OTP not in logs | Trigger OTP verification | OTP value not present in any log output |
| `SEC-NOPHI-003` | P0 | Raw JWT not in logs | Any authenticated request | JWT token string not logged at any level |
| `SEC-SOFTDEL-001` | P1 | Soft-deleted user cannot authenticate | Set `is_active=false`, attempt login | `401 Unauthorized` |
| `SEC-SOFTDEL-002` | P1 | Soft-deleted user's data inaccessible | Existing valid JWT for deactivated user | All endpoints return `401` |
| `SEC-BCRYPT-001` | P2 | Passwords stored as bcrypt hash | Create standard (non-ABHA) account | `password_hash` column starts with `$2b$12$` (12 rounds) |

---

## 13. Test Data Catalogue

### 13.1 Seed Users

| Alias | Role | Phone | ABHA Address | Notes |
|---|---|---|---|---|
| `patient_alice` | PATIENT | +91-9000000001 | alice@abdm | Active pregnancy, week 28 |
| `patient_bob` | PATIENT | +91-9000000002 | bob@abdm | Postnatal, child 6 months old |
| `doctor_sharma` | DOCTOR | +91-9000000010 | sharma@abdm | Has consent for alice |
| `doctor_mehta` | DOCTOR | +91-9000000011 | mehta@abdm | No consent for any patient |
| `admin_root` | ADMIN | +91-9000000099 | — | System admin |

### 13.2 Seed Pregnancy Profiles

| Profile | User | LMP | EDD | Week (at test time) | High Risk Flags |
|---|---|---|---|---|---|
| `profile_alice_active` | patient_alice | 2025-09-14 | 2026-06-21 | 28 | `["HYPOTHYROIDISM"]` |
| `profile_bob_delivered` | patient_bob | 2025-09-07 | 2026-06-14 | — | `[]` (delivered 2025-12-21) |

### 13.3 Sample Lab Report Fixtures (for APS tests)

| Fixture File | Content | Entities Expected |
|---|---|---|
| `cbc_clear.pdf` | Clear CBC report with Hemoglobin 9.2 g/dL, WBC 8.5 | Hemoglobin (LOINC 718-7), WBC |
| `bp_reading.jpg` | Typed BP report: 145/95 mmHg | Systolic (8480-6), Diastolic (8462-4) |
| `tsh_report.pdf` | TSH 4.8 mIU/L, T3/T4 values | TSH (3016-3) |
| `blurry_scan.jpg` | Low-quality handwritten scan | confidence_score < 0.6 |

### 13.4 WHO Z-Score Reference Values (for NCS growth tests)

| Sex | Age (months) | Median weight (kg) | SD | Test input | Expected Z |
|---|---|---|---|---|---|
| Male | 6 | 7.52 | 0.82 | 7.9 kg | +0.45 |
| Female | 6 | 6.87 | 0.76 | 6.87 kg | 0.00 (50th %ile) |
| Male | 12 | 9.53 | 1.00 | 7.5 kg | −2.03 (2nd %ile) |

### 13.5 WireMock Stub Inventory

| Stub | URL | Response | Used By |
|---|---|---|---|
| ABDM OTP generate | `POST /v3/enrollment/request/otp` | `200 { "txnId": "test-txn-001" }` | IAB-AUTH-001 |
| ABDM consent callback | `POST /api/v1/identity/callback/consent` | Fired by test harness | IAB-CALLBACK-001 |
| Textract success | `POST /` (LocalStack) | CBC text response | APS-OCR-001 |
| Textract timeout | `POST /` (LocalStack) | 30s delay + 504 | APS-OCR-003 |
| Twilio WhatsApp success | `POST /Messages.json` | `{ "sid": "SM123", "status": "sent" }` | NS-WHATSAPP-001 |
| Twilio WhatsApp failure | `POST /Messages.json` | `503 Service Unavailable` | NS-SMS-FALLBACK-001 |
| FCM push success | `POST /fcm/send` | `{ "success": 1 }` | NS-PUSH-001 |
