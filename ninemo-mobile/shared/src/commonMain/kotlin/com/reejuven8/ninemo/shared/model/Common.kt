package com.reejuven8.ninemo.shared.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ─── Envelope (mirrors ApiResponse<T> / PagedResponse<T> in api.ts) ───────────
@Serializable
data class ApiResponse<T>(
    val status: String,
    val data: T? = null,
    val error: ErrorBody? = null,
    val metadata: Map<String, JsonElement>? = null,
)

@Serializable
data class ErrorBody(
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class PagedResponse<T>(
    val status: String,
    val data: PagedData<T>,
)

@Serializable
data class PagedData<T>(
    val content: List<T>,
    val pagination: Pagination,
)

@Serializable
data class Pagination(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

// ─── Shared enums (server-provided; client never computes these) ──────────────
@Serializable
enum class SeverityFlag { NORMAL, WARNING, CRITICAL }

// VaccinationStatus.java: PENDING/COMPLETED/SKIPPED/OVERDUE (SKIPPED defined but never set).
@Serializable
enum class VaccinationStatus { PENDING, COMPLETED, SKIPPED, OVERDUE }

// No DelayRisk enum exists server-side — developmental-delay risk is a string in alertFlags
// ("DEVELOPMENTAL_DELAY_RISK"), binary present/absent, not a 3-level enum. Removed.

// Server enum is FoodSafetyRating.java: exactly 3 values (no MODERATE, no UNSAFE).
@Serializable
enum class SafetyRating { SAFE, CAUTION, AVOID }

@Serializable
enum class UserRole { PATIENT, DOCTOR, ADMIN }

@Serializable
enum class BiologicalSex { MALE, FEMALE, OTHER }
