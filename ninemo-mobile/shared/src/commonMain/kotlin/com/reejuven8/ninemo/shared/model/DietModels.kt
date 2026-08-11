package com.reejuven8.ninemo.shared.model

import kotlinx.serialization.Serializable

// ─── Diet "Is It Safe?" (verified against DietController/DietLookupResponse.java) ─────
// GET /diet/search?q= -> ApiResponse<List<DietLookupResponse>>, no auth header, plain
// case-insensitive ILIKE substring match (not fuzzy/trigram), capped at 20 results.
// No id/trimesterTags/categories on the wire — those exist on the JPA entity only and
// are never mapped into the response.
@Serializable
data class DietFoodSafetyResponse(
    val ingredientName: String,
    val ingredientNameHindi: String? = null,
    val safetyRating: SafetyRating,
    val medicalReasoning: String, // not "description"
    val safeQuantity: String? = null,
)
