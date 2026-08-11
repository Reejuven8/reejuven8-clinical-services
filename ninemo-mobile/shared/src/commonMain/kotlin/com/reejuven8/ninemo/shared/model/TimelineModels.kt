package com.reejuven8.ninemo.shared.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ─── Timeline (verified against TimelineController/TimelineResponse.java) ─────────────
// babyDevelopment/scheduledMilestones/dietTips/yogaRoutine are untyped Map<String,Object>
// backend-side — content is data-driven (ninemo_timeline_feed, CMS-style, no fixed Java
// class) and today mostly comes back as TimelineService's fallback stub (no seed data
// exists yet): babyDevelopment={"summary": "..."}, scheduledMilestones=[{"name","week"}],
// dietTips=[], yogaRoutine=null. Once seeded, real keys follow Database_Design.md §6.2
// (snake_case: size_comparison, weight_grams, key_developments, image_url, etc). Kept as
// JsonObject rather than a fixed data class since the backend gives no schema guarantee.
typealias TimelineJsonObject = Map<String, JsonElement>

@Serializable
data class TimelineResponse(
    val gestationalWeek: Int,
    val trimester: Int, // 1 | 2 | 3
    val babyDevelopment: TimelineJsonObject,
    val maternalChanges: List<String>,
    val scheduledMilestones: List<TimelineJsonObject>,
    val dietTips: List<TimelineJsonObject>,
    val yogaRoutine: TimelineJsonObject? = null,
)
