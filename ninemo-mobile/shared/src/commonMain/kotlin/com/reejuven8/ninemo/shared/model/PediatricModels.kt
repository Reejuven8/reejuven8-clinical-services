package com.reejuven8.ninemo.shared.model

import kotlinx.serialization.Serializable

// ─── Growth (verified against GrowthController/GrowthChartService/GrowthMeasurement.java) ──
// POST/GET /ninemo/growth/children/{childId}/measurements. No X-User-Id, no ownership check
// (backend security gap — see IS-027). Controller returns the raw Mongo document (the
// GrowthMeasurementResponse DTO is dead code); GET is a plain List (not Page/PagedResponse).
@Serializable
data class GrowthInputRequest(
    val ageInMonths: Int, // client-supplied + required; server does NOT derive from DOB
    val heightCm: Double,
    val weightKg: Double,
    val headCircumferenceCm: Double? = null,
    // no measurementDate — server stamps Instant.now()
)

@Serializable
data class GrowthMeasurementResponse(
    val id: String,
    val childId: String,
    val ageInMonths: Int,
    val measurementDate: String, // ISO-8601 Instant
    val heightCm: Double,
    val weightKg: Double,
    val headCircumferenceCm: Double? = null,
    // Map keys are snake_case WHO indicators: weight_for_age, height_for_age,
    // head_circumference_for_age (last only when HC present & age<=24m). No weight_for_height.
    val zScores: Map<String, Double> = emptyMap(),
    val percentiles: Map<String, Int> = emptyMap(), // Int (0-100), not Double
    val previousPercentiles: Map<String, Int> = emptyMap(),
    // Human-readable strings with interpolated values (e.g. "WEIGHT_Z_UNDERNUTRITION (Z=-2.4)")
    // — NOT stable codes; never pattern-match on these for logic.
    val alertFlags: List<String> = emptyList(),
    val crossedPercentileLines: Int = 0,
    val notes: String? = null,
    val createdAt: String? = null,
)

// ─── Vaccination (verified against VaccinationController/VaccinationRecord.java) ──────
// GET /schedule is idempotent-generate (first call persists the full IAP schedule).
// mark-completed is PUT with QUERY PARAMS (administeredDate?, administeredBy?), no body.
// No "overdue" boolean exists — derive from status == OVERDUE.
@Serializable
data class VaccinationRecordResponse(
    val id: String,
    val childId: String,
    val vaccineName: String,
    val vaccineCode: String,
    val doseNumber: Int, // OPV birth dose is 0 — don't assume >=1
    val scheduledDate: String, // date-only
    val administeredDate: String? = null,
    val status: VaccinationStatus,
    val certificateS3Url: String? = null,
    val administeredBy: String? = null,
    val notes: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    val isOverdue: Boolean get() = status == VaccinationStatus.OVERDUE
}

// ─── Developmental Milestones (verified against MilestoneController/DevelopmentalMilestone.java)
// GET /children/{childId} and /children/{childId}/month/{month} (get-or-create). achieve is
// PUT with QUERY PARAMS milestoneName (exact text) + achieved, no body.
@Serializable
data class MilestoneItem(
    val name: String,
    val achieved: Boolean = false,
)

@Serializable
data class DevelopmentalMilestoneResponse(
    val id: String,
    val childId: String,
    val month: Int,
    val milestones: List<MilestoneItem> = emptyList(), // list of {name,achieved}, NOT a Map
    val category: String? = null, // single doc-level string e.g. "WHO_MILESTONE" (no per-item category)
    val alertFlags: List<String> = emptyList(),
    val reviewedAt: String? = null,
    val updatedAt: String? = null,
    val createdAt: String? = null,
) {
    val hasDelayRisk: Boolean get() = alertFlags.any { it.contains("DEVELOPMENTAL_DELAY_RISK") }
    val achievedCount: Int get() = milestones.count { it.achieved }
}

// ─── Child profile / mode transition (verified against ModeTransitionController +
// ChildProfileController + ChildProfileResponse.java after NM-B-169 landed)
//
// Changes from F5: the transition now accepts an OPTIONAL body, so the baby's name/sex/
// birth measurements are captured at transition time instead of being dropped (IS-029);
// the response is a ChildProfileResponse DTO carrying a server-computed ageInMonths; and
// GET /ninemo/children lists them, so childId survives a reinstall (IS-028). Every
// child-scoped route is X-User-Id ownership-checked now and 403s for a foreign childId
// (IS-027) — the app must never assume a childId it holds is still readable.
@Serializable
data class ChildProfileResponse(
    val id: String,
    val pregnancyProfileId: String? = null,
    val parentUserId: String? = null,
    val childName: String? = null,
    val biologicalSex: String? = null, // MALE/FEMALE/OTHER; defaults OTHER
    val dateOfBirth: String? = null,
    val ageInMonths: Int = 0,          // server-computed — do not derive on the client
    val birthWeightKg: Double? = null,
    val birthHeightCm: Double? = null,
    val headCircumferenceCm: Double? = null,
    val bloodGroup: String? = null,
    val active: Boolean? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/** Optional body for the mode transition, and the body of PATCH /ninemo/children/{id}. */
@Serializable
data class ChildProfileUpdateRequest(
    val childName: String? = null,
    val dateOfBirth: String? = null,
    val biologicalSex: String? = null, // MALE | FEMALE | OTHER
    val birthWeightKg: Double? = null,
    val birthHeightCm: Double? = null,
    val headCircumferenceCm: Double? = null,
    val bloodGroup: String? = null,
)
