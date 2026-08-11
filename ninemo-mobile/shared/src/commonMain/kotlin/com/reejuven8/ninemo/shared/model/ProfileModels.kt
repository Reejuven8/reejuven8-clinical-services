package com.reejuven8.ninemo.shared.model

import kotlinx.serialization.Serializable

// ─── Pregnancy profile / onboarding ───────────────────────────────────────────
// Verified against the shipped PregnancyProfileController / PregnancyProfileRequest /
// PregnancyProfileResponse (NM-B-167, closes IS-018).
//
// Rewritten from the F0 stub, which was invented from prose: it wrapped the dates in a
// `dateBasis` object, sent `ageYears` (no such column exists — the wizard still collects it
// for display, but it is not persisted), and named the response fields patientId/edd/bmi/
// riskFlags. The real payload is flat and mirrors the pregnancy_profiles columns.
//
// Exactly ONE dating basis may be sent. Ultrasound additionally requires the gestational age
// measured at the scan — sending ultrasoundDate alone is a 400.
/**
 * Onboarding step-1 UI state, NOT a wire type — the backend takes the dates flat.
 * OnboardingViewModel maps this onto [CreatePregnancyProfileRequest].
 */
data class DateBasis(
    val lmpDate: String? = null,
    val ultrasoundDate: String? = null,
    val ultrasoundWeeks: Int? = null,
    val ivfTransferDate: String? = null,
)

@Serializable
data class CreatePregnancyProfileRequest(
    val lmpDate: String? = null,                        // ISO-8601 date
    val ultrasoundDate: String? = null,
    val ultrasoundGestationalAgeWeeks: Int? = null,
    val ultrasoundGestationalAgeDays: Int? = null,
    val ivfTransferDate: String? = null,
    val eddCalculationMethod: String? = null,           // LMP | ULTRASOUND | IVF; inferred when null
    val heightCm: Double,
    val prePregnancyWeightKg: Double,
    val bloodGroup: String,                             // A+/A-/B+/B-/AB+/AB-/O+/O-
    val highRiskFlags: List<String> = emptyList(),
)

@Serializable
data class PregnancyProfileResponse(
    val id: String,
    val userId: String,
    val lmpDate: String? = null,
    val ultrasoundDate: String? = null,
    val ivfTransferDate: String? = null,
    val eddDate: String,              // server-computed — never derived on client
    val eddCalculationMethod: String,
    val heightCm: Double,
    val prePregnancyWeightKg: Double,
    val baselineBmi: Double,          // server-computed
    val bloodGroup: String,
    val highRiskFlags: List<String> = emptyList(),
    val gestationalWeek: Int,         // server-computed on every read
    val trimester: Int,
    val active: Boolean,
    val deliveryDate: String? = null,
    val deliveryType: String? = null,
)

// ChildProfileResponse + mode-transition DTOs live in PediatricModels.kt.

// ─── Device registration (push) — used in F8 ──────────────────────────────────
@Serializable
data class DeviceRegistrationRequest(
    val token: String,
    val platform: String, // ANDROID | IOS
)

// ─── Consent (verified against ConsentController/ConsentRequest/ConsentResponse.java) ──
// No durationDays server-side — client computes an absolute expiresAt and sends that.
// No doctor search/lookup endpoint exists anywhere in identity-abha-service; the caller
// must already have the doctor's raw user UUID (matches the design mockup's "doctor's
// NineMo ID" manual-entry flow, so no gap vs the UI spec — just vs the mobile stub).
@Serializable
data class ConsentGrantRequest(
    val doctorId: String,
    val expiresAt: String, // ISO-8601 Instant
)

// consentStatus never auto-transitions to EXPIRED server-side (no scheduled job exists) —
// a GRANTED consent past its expiresAt still reports GRANTED. Client must compare
// expiresAt itself to render an "expired" state (rendering already-provided data, not
// clinical computation).
@Serializable
data class ConsentResponse(
    val id: String,
    val patientId: String,
    val doctorId: String,
    val consentStatus: String, // GRANTED | REVOKED | EXPIRED (server enum; EXPIRED never actually set)
    val grantedAt: String,
    val expiresAt: String,
    val revokedAt: String? = null,
)

// ─── Health records (verified against HealthRecordController/FhirResourceResponse.java)
// Records are raw FHIR resource documents, not a flat "health record" model — no title,
// no recordType (real field is resourceType), no fileKey (real field is sourceFileS3Url).
// code/valueQuantity are untyped FHIR CodeableConcept/Quantity maps (no fixed schema).
@Serializable
data class HealthRecordResponse(
    val id: String,
    val patientId: String,
    val resourceType: String, // "Observation" | "Bundle" | "DocumentReference" etc — FHIR type
    val resourceId: String? = null,
    val status: String? = null,
    val effectiveDatetime: String? = null, // often absent — createdAt is the reliable sort key
    val category: String? = null,
    val code: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
    val valueQuantity: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
    val source: String? = null,
    val sourceFileS3Url: String? = null,
    val tags: List<String> = emptyList(), // never actually populated server-side today
    val notes: String? = null,
    val createdAt: String,
)

// GET /health/records returns Spring Data's own Page<T> JSON shape (not common-lib's
// PagedResponse<T> used elsewhere) — modeled only for the fields the client actually uses.
@Serializable
data class SpringPage<T>(
    val content: List<T> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
    val size: Int = 0,
    val last: Boolean = true,
)

// ─── File upload + SSE parse status (verified against FileUploadController.java) ──────
// Upload never returns a recordId — FhirResource documents are created asynchronously,
// one per parsed observation, with no correlation id surfaced back to the client. The
// only two parseStatus values that ever exist are PROCESSING/PARSED — no FAILED path is
// wired up backend-side. SSE (/health/files/events?s3Key=) sends a bare string as `data`
// under the named event "parse-progress", not a JSON payload.
@Serializable
data class FileUploadResponse(
    val s3Key: String,
    val presignedDownloadUrl: String,
    val status: String, // always "PROCESSING" at upload time
)
