package com.reejuven8.ninemo.android.navigation

import kotlinx.serialization.Serializable

/** Type-safe destinations (replaces RN routes.ts string constants). */
sealed interface Routes {
    // Auth + onboarding (F1)
    @Serializable data object Splash : Routes
    @Serializable data object Login : Routes
    @Serializable data object Register : Routes
    @Serializable data object AbhaLink : Routes
    @Serializable data object Onboarding : Routes

    // Bottom-tab roots
    @Serializable data object Home : Routes       // Timeline (pregnancy) / Child Dashboard (child)
    @Serializable data object Locker : Routes
    @Serializable data object Tools : Routes
    @Serializable data object Community : Routes
    @Serializable data object Profile : Routes

    // Tool destinations (F2–F5)
    @Serializable data object SymptomLog : Routes
    @Serializable data object VitalsWeight : Routes
    @Serializable data object VitalsBP : Routes
    @Serializable data object KickCounter : Routes
    @Serializable data object ContractionTimer : Routes
    @Serializable data object Diet : Routes
    @Serializable data class SummaryCard(val patientId: String) : Routes
    @Serializable data class GrowthChart(val childId: String) : Routes
    @Serializable data class Vaccination(val childId: String) : Routes
    @Serializable data class Milestones(val childId: String) : Routes

    // Locker/community detail (F4/F6)
    @Serializable data class DocumentDetail(val recordId: String) : Routes
    @Serializable data object Consent : Routes
    @Serializable data class ChannelChat(
        val clubId: String,
        val channelId: String,
        val channelName: String? = null,
        val alias: String? = null,
    ) : Routes
    @Serializable data object ContentFeed : Routes
    @Serializable data object ModeTransition : Routes
}
