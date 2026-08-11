package com.reejuven8.ninemo.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Symptoms (verified against SymptomController/SymptomLogRequest/SymptomLog.java) ──
// symptoms/vitalsAtLog are untyped Map<String,Object> backend-side (no fixed DTO) — these
// shapes are inferred from the documented schema (Database_Design.md §6.3) and the actual
// keys SymptomTriageEngine rules read (PreeclampsiaRule, AnemiaRule, etc).
@Serializable
data class SymptomItem(
    val name: String, // e.g. "headache", "blurred_vision", "reduced_fetal_movement" — triage rules match on this
    val category: String? = null,
    val severity: String? = null, // "mild" | "moderate" | "severe" (free text server-side, not an enum)
)

@Serializable
data class VitalsAtLog(
    @SerialName("blood_pressure_systolic") val bloodPressureSystolic: Int? = null,
    @SerialName("blood_pressure_diastolic") val bloodPressureDiastolic: Int? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("temperature_celsius") val temperatureCelsius: Double? = null,
    @SerialName("heart_rate_bpm") val heartRateBpm: Int? = null,
)

@Serializable
data class SymptomLogRequest(
    val symptoms: List<SymptomItem>,
    val vitalsAtLog: VitalsAtLog? = null,
)

// POST /ninemo/symptoms response — SymptomLogResponse.java (leaner than the GET history shape)
@Serializable
data class SymptomLogResponse(
    val id: String,
    val patientId: String,
    val gestationalWeek: Int,
    val symptoms: List<SymptomItem>,
    val severityFlag: SeverityFlag,
    val triggeredRules: List<String>,
    val loggedAt: String,
)

@Serializable
data class TriageResult(
    @SerialName("rules_triggered") val rulesTriggered: List<String> = emptyList(),
    val recommendation: String? = null,
    @SerialName("remediation_tips") val remediationTips: List<String> = emptyList(),
    @SerialName("alert_sent") val alertSent: Boolean = false,
    @SerialName("alert_channels") val alertChannels: List<String> = emptyList(),
)

// GET /ninemo/symptoms (history) — returns the raw SymptomLog Mongo document, a richer/
// different shape than the POST response (has pregnancyProfileId/trimester/createdAt,
// and triageResult here is the full object, not just triggeredRules).
@Serializable
data class SymptomLogHistoryEntry(
    val id: String,
    val patientId: String,
    val pregnancyProfileId: String,
    val gestationalWeekAtLog: Int,
    val trimester: Int,
    val symptoms: List<SymptomItem>,
    val vitalsAtLog: VitalsAtLog? = null,
    val severityFlag: SeverityFlag,
    val triageResult: TriageResult? = null,
    val loggedAt: String,
    val createdAt: String,
)

// ─── Vitals (verified against VitalsController/VitalsLogRequest/VitalsLog.java) ────────
enum class VitalType { WEIGHT, BLOOD_PRESSURE, BLOOD_SUGAR, TEMPERATURE }

// measurements is untyped Map<String,Object> backend-side; keys per Database_Design.md §6.4 —
// only the keys relevant to vitalType are populated per submission.
@Serializable
data class VitalsMeasurements(
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("blood_pressure_systolic") val bloodPressureSystolic: Int? = null,
    @SerialName("blood_pressure_diastolic") val bloodPressureDiastolic: Int? = null,
    @SerialName("fasting_glucose_mg_dl") val fastingGlucoseMgDl: Double? = null,
    @SerialName("temperature_celsius") val temperatureCelsius: Double? = null,
)

@Serializable
data class VitalsLogRequest(
    val vitalType: String, // VitalType.name — backend takes a raw String, not an enum type
    val measurements: VitalsMeasurements,
    val source: String? = null, // MANUAL | BLUETOOTH_DEVICE | OCR_PARSED
)

// No VitalsLogResponse class exists backend-side — POST/GET both return the raw VitalsLog document.
@Serializable
data class VitalsLogResponse(
    val id: String,
    val patientId: String,
    val pregnancyProfileId: String,
    val gestationalWeek: Int,
    val trimester: Int,
    val vitalType: String,
    val measurements: VitalsMeasurements,
    val source: String,
    val deviceName: String? = null,
    val isWithinNormalRange: Boolean,
    val alertTriggered: Boolean,
    val loggedAt: String,
    val createdAt: String,
)

// ─── Kick Counter (verified against KickCounterController/KickSessionResponse.java) ────
// POST /sessions (start, no body), PUT /sessions/{id}/kick, PUT /sessions/{id}/end (not POST).
// No GET history endpoint exists — repository is unused/unexposed backend-side.
@Serializable
data class KickCounterSessionResponse(
    val sessionId: String, // not "id"
    val patientId: String,
    val gestationalWeek: Int,
    val sessionStart: String,
    val sessionEnd: String? = null,
    val totalKicks: Int,
    val durationTo10KicksMinutes: Int? = null, // Int, not Double
    val isConcerning: Boolean,
) {
    // "active" isn't a real field — server signals an open session via null sessionEnd.
    val isActive: Boolean get() = sessionEnd == null
}

// ─── Contraction Timer (verified against ContractionController/ContractionRequest/
// ContractionSessionResponse.java) ──────────────────────────────────────────────────
// POST /sessions (start, no body), PUT /sessions/{id}/contraction, PUT /sessions/{id}/end.
// Client only ever sends durationSeconds (+ optional free-text intensity) — the server
// computes/timestamps startTime and intervalFromPreviousSeconds itself.
@Serializable
data class ContractionRequest(
    val durationSeconds: Int,
    val intensity: String? = null, // free string server-side, NOT a validated enum
)

// contractions[] is untyped Map<String,Object> backend-side (built manually, not a DTO):
// keys are startTime/durationSeconds/intensity always, intervalFromPreviousSeconds only
// after the first entry. No endTime key is ever populated despite the DB design doc.
@Serializable
data class ContractionSessionResponse(
    val sessionId: String, // not "id"
    val patientId: String,
    val gestationalWeek: Int,
    val sessionStart: String,
    val sessionEnd: String? = null,
    val contractions: List<Map<String, kotlinx.serialization.json.JsonElement>>,
    val totalContractions: Int,
    val averageIntervalSeconds: Int, // non-nullable Int, not Double
    val averageDurationSeconds: Int, // non-nullable Int, not Double
    val isLaborPattern: Boolean,
    val alertTriggered: Boolean, // isLaborPattern && gestationalWeek < 37
)

// ─── Summary Card (verified against SummaryCardController/SummaryCardResponse.java —
// structurally different from the F0 stub: no pregnancyProfileId/lastKickSession, has
// patientName/abhaAddress/bloodGroup/pendingMilestones instead) ───────────────────────
@Serializable
data class SummaryCardResponse(
    val patientId: String,
    val patientName: String,
    val abhaAddress: String? = null,
    val gestationalWeek: Int,
    val trimester: Int,
    val eddDate: String,
    val bloodGroup: String? = null,
    val highRiskFlags: List<String>,
    val latestVitals: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
    val recentSymptoms: List<Map<String, kotlinx.serialization.json.JsonElement>> = emptyList(),
    val pendingMilestones: List<Map<String, kotlinx.serialization.json.JsonElement>> = emptyList(),
)
