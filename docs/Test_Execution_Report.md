# NineMo Backend — Test Execution Report

> **Executed:** 2026-07-16 (local, macOS, Java 26, Maven full reactor; containers via OrbStack)
> **Result:** ✅ **BUILD SUCCESS — 50 Java tests, 0 failures, 0 errors, 0 skipped** (Docker up, integration tests executed for real)
> Python suite: 18 tests, last executed 2026-06-21 (see §4).
>
> ⚠️ **Related, pre-existing doc:** `Functional_Test_Document.md` — the functional **acceptance-test plan**
> (test-case catalogue IAB/HDS/APS/NCS/NS/COM/GW/EV/SEC with expected results; status DRAFT, no
> execution results recorded). This report is the **execution record** of the automated suites and
> complements — does not replace — that plan.

---

## 1. Summary by Module

| Module | Suite type | Tests | Pass | Fail | Command |
|---|---|---|---|---|---|
| identity-abha-service | Unit + Integration | 14 | 14 | 0 | `mvn test -pl services/identity-abha-service` |
| ninemo-clinical-service | Unit + Integration | 34 | 34 | 0 | `mvn test -pl services/ninemo-clinical-service` |
| health-data-service | Integration | 2 | 2 | 0 | `mvn test -pl services/health-data-service` |
| api-gateway / notification / community / common-lib | — | 0 | — | — | no test classes (covered via integration + plan) |
| **Java total** | | **50** | **50** | **0** | `mvn test` (reactor) |
| ai-parsing-service (Python) | Unit | 18 | 18 | 0 | `pytest tests/` (last run 2026-06-21; see §4) |

Integration tests are annotated `@Testcontainers(disabledWithoutDocker = true)` — they run against
**real PostgreSQL 16 / MongoDB 7 / Redis 7 / RabbitMQ 3.13 containers** when Docker is available
(local + CI) and skip cleanly otherwise.

---

## 2. Java Unit Tests — Detail (all ✅ PASS)

### 2.1 identity-abha-service

| # | Test | Verifies | Result |
|---|---|---|---|
| 1 | `JwtTokenProviderTest.generateToken_thenExtractClaims_roundTrips` | Token → claims: userId, role, abhaAddress intact | ✅ |
| 2 | `JwtTokenProviderTest.extractRole_matchesInput` | DOCTOR role round-trips | ✅ |
| 3 | `JwtTokenProviderTest.tamperedToken_throwsJwtException` | Signature tamper detected → rejected | ✅ |
| 4 | `JwtTokenProviderTest.extractJti_isNonNullAndUniquePerToken` | Unique JTI per token (blacklist key) | ✅ |
| 5 | `JwtTokenProviderTest.remainingTtl_isPositiveAndAtMostConfiguredExpiry` | TTL ≤ 15-min config | ✅ |
| 6 | `JwtTokenProviderTest.differentUsers_produceDifferentTokens` | No token collisions | ✅ |
| 7 | `RsaEncryptionServiceTest.encrypt_producesBase64Output` | ABDM payload is valid Base64 | ✅ |
| 8 | `RsaEncryptionServiceTest.encrypt_roundTripsWithPrivateKey` | OAEP ciphertext decrypts to original (real 2048-bit keypair) | ✅ |
| 9 | `RsaEncryptionServiceTest.encrypt_oaepRandomnessProducesDifferentCiphertexts` | Same plaintext ≠ same ciphertext (OAEP random padding) | ✅ |
| 10 | `RsaEncryptionServiceTest.encrypt_otpAndAadhaarPayloads_doNotThrow` | OTP / Aadhaar-length payloads encrypt | ✅ |
| 11 | `RsaEncryptionServiceTest.encrypt_invalidPublicKey_throwsIllegalState` | Garbage key → clean failure | ✅ |

### 2.2 ninemo-clinical-service

| # | Test class | Tests | Verifies | Result |
|---|---|---|---|---|
| 12–25 | `TimelineServiceTest` | 14 | Gestational week from EDD (EDD today → wk 41; 10 wks out → wk 31), clamping to [1,42], trimester boundaries (wk 13/14, 27/28) — parameterized | ✅ |
| 26–36 | `WhoLmsTableTest` | 11 | WHO Z-scores: 0 at median, sign above/below, LMS interpolation between month entries, Z→percentile (0 → 50th, ±1.96 → 2.5/97.5), clamp [1,99], girls' table, head-circumference out-of-range clamp | ✅ |
| 37–39 | `KickCounterServiceTest` | 3 | WHO rule: <10 kicks in ≥120 min → concerning + CRITICAL alert; 10+ kicks → fine; <120 min never concerning | ✅ |
| 40–43 | `ContractionServiceTest` | 4 | Labour pattern (interval ≤300s ∧ duration ≥60s) before wk 37 → alert; at wk 38 → none; wide interval / short duration → no pattern | ✅ |

---

## 3. Integration Tests — Detail (all ✅ PASS, real containers)

| # | Test | Containers | Scenario | Result |
|---|---|---|---|---|
| 44 | `IdentityIntegrationTest.flywayMigrationsApplied` | postgres:16 | V1–V5 applied; `users` table exists | ✅ |
| 45 | `IdentityIntegrationTest.otpStoredWithTtlThenVerifiedAndConsumed` | postgres:16 + redis:7 | OTP in Redis with ≤5-min TTL → verify OK → second use rejected (consumed) | ✅ |
| 46 | `IdentityIntegrationTest.wrongOtpRejectedAndNotConsumed` | redis:7 | Wrong OTP → Unauthorized; correct OTP still works after | ✅ |
| 47 | `ClinicalIntegrationTest.flywayMigrationsAppliedIncludingDietSeed` | postgres:16 | V1–V8 applied; ≥15 diet rows seeded | ✅ |
| 48 | `ClinicalIntegrationTest.symptomLogWritesToMongoWithComputedGestationalWeek` | postgres:16 + mongo:7 | Real `pregnancy_profiles` insert → SymptomService → Mongo `symptom_logs` doc, week ≈31 computed, severity NORMAL | ✅ |
| 49 | `HealthDataIntegrationTest.fhirResourceCrudRoundTrip` | mongo:7 | FHIR Observation save → read → delete in `fhir_resources` | ✅ |
| 50 | `HealthDataIntegrationTest.documentUploadPublishConsumeRoundTrip` | mongo:7 + rabbitmq:3.13 | `DocumentUploadMessage` published → consumed from real queue; patientId/s3Key/**correlationId** intact | ✅ |

---

## 4. Python Suite — ai-parsing-service

Last executed **2026-06-21** (authoring session), all passing. Not re-run on 2026-07-16
(no local pytest venv); runs on every push via CI `python-test` job.

| Test file | Tests | Verifies | Result (2026-06-21) |
|---|---|---|---|
| `test_health.py` | 1 | `/health` returns UP | ✅ |
| `test_ner_service.py` | 6 | Lab-report text → `ParsedObservation` list (regex/spaCy hybrid; value+unit extraction) | ✅ |
| `test_loinc_mapper.py` | 6 | "Haemoglobin"/"Hemoglobin"/"Hb" → `718-7`; TSH → `3016-3`; glucose → `2339-0`; unknown → None | ✅ |
| `test_fhir_mapper.py` | 5 | FHIR R4 Observation JSON: resourceType, subject reference, LOINC coding, valueQuantity, display name | ✅ |

---

## 5. Defects Found by These Tests

The integration suite paid for itself on first run — see `Issue_Tracker.md` for full entries:

| Issue | Severity | Found by | Summary |
|---|---|---|---|
| IS-016 | 🔴 | test #48 | **Every JPA insert into a PostgreSQL enum column failed** (Hibernate binds enums as VARCHAR). Broke registration, pregnancy onboarding, and notification logging in any real PG environment — invisible to mocked unit tests. Fixed: `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` on 12 fields across 11 entities. |
| IS-015 | 🔴 | compile of §2.1 suite | Both identity unit-test files had never compiled (written against invented APIs); NM-B-151 was previously marked done without a module test run. Rewritten against real signatures. |
| IS-017 | 🟡 | first container run | OrbStack rejects Testcontainers' pinned Docker API 1.32. Fixed: Testcontainers 1.20.1→1.21.3 + `~/.orbstack/config/docker.json` `{"min-api-version": "1.24"}`. CI unaffected. |
| IS-014 | 🔴 | manual contract audit (NM-B-165) | Backend raw-DTO responses vs frontend expecting `ApiResponse<T>` envelope — would fail on first app launch. Fixed by normalizing 8 controllers. |

---

## 6. How to Reproduce

```bash
cd ninemo-backend

# Full Java suite (integration tests need Docker running)
mvn test

# Single module
mvn test -pl services/ninemo-clinical-service

# Python suite
cd services/ai-parsing-service && pip install -r requirements.txt && pytest tests/ -v
```

Environment notes (must not regress — `Issue_Tracker.md` IS-001/002/013/017):
Java 26 needs Lombok 1.18.38 + Surefire `-Dnet.bytebuddy.experimental=true` (both in parent POM);
after editing `common-lib`, run `mvn install -pl common-lib` before `-pl` builds;
OrbStack users need the min-api-version config above.
