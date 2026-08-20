package com.reejuven8.ninemo.shared.network

/**
 * Endpoint path constants. All traffic goes through api-gateway :8080 → /api/v1/{endpoint}.
 * Base URL is supplied per-platform (dev: 10.0.2.2 Android / localhost iOS sim).
 * Source of truth: docs/Android_Native_Migration_Analysis.md §2.1.
 */
object ApiRoutes {
    const val IDENTITY = "identity"
    const val HEALTH = "health"
    const val NINEMO = "ninemo"

    // Auth
    const val AUTH = "$IDENTITY/auth"
    const val OTP_SEND = "$AUTH/otp/send"
    const val LOGIN = "$AUTH/login"
    const val REGISTER = "$AUTH/register"
    const val REFRESH = "$AUTH/refresh"
    const val LOGOUT = "$AUTH/logout"

    // ABHA
    const val ABHA = "$IDENTITY/abha"
    const val ABHA_OTP_GENERATE = "$ABHA/enroll/otp/generate"
    const val ABHA_OTP_VERIFY = "$ABHA/enroll/otp/verify"
    const val ABHA_ADDRESS = "$ABHA/enroll/address"

    // Consent
    const val CONSENT = "$IDENTITY/consent"
    const val CONSENT_GRANT = "$CONSENT/grant"
    const val CONSENT_LIST = "$CONSENT/list"
    fun consentRevoke(id: String) = "$CONSENT/revoke/$id"

    // Doctors
    const val DOCTOR_SEARCH = "$IDENTITY/doctors/search" // ?phoneNumber=

    // Health records / files
    const val RECORDS = "$HEALTH/records" // ?page=&size= -> ApiResponse<SpringPage<HealthRecordResponse>>
    fun record(id: String) = "$HEALTH/records/$id"
    fun recordsByPatient(patientId: String) = "$HEALTH/records/patient/$patientId" // ?resourceType= -> plain List, no paging
    const val FILE_UPLOAD = "$HEALTH/files/upload"
    // Both are query-param based server-side (s3Key), not path segments — a real bug fixed
    // here (the prior path-based form would 404 against the actual backend, see IS-025).
    const val FILE_DOWNLOAD = "$HEALTH/files/download"
    const val FILE_EVENTS = "$HEALTH/files/events" // SSE, event name "parse-progress", bare string data

    // Timeline
    const val TIMELINE_CURRENT = "$NINEMO/timeline/current"
    fun timelineWeek(week: Int) = "$NINEMO/timeline/week/$week"

    // Symptoms / vitals
    const val SYMPTOMS = "$NINEMO/symptoms"
    const val VITALS = "$NINEMO/vitals"
    fun vitalsByType(type: String) = "$NINEMO/vitals/$type"

    // Kick counter
    const val KICK_SESSIONS = "$NINEMO/kick-counter/sessions"
    fun kickAdd(id: String) = "$NINEMO/kick-counter/sessions/$id/kick"
    fun kickEnd(id: String) = "$NINEMO/kick-counter/sessions/$id/end"

    // Contractions
    const val CONTRACTION_SESSIONS = "$NINEMO/contractions/sessions"
    fun contractionAdd(id: String) = "$NINEMO/contractions/sessions/$id/contraction"
    fun contractionEnd(id: String) = "$NINEMO/contractions/sessions/$id/end"

    // Summary card
    fun summaryCard(patientId: String) = "$NINEMO/summary-card/$patientId"

    // Growth / vaccination / milestones
    fun growth(childId: String) = "$NINEMO/growth/children/$childId/measurements"
    fun vaccinationSchedule(childId: String) = "$NINEMO/vaccinations/children/$childId/schedule"
    fun vaccinationMarkCompleted(recordId: String) = "$NINEMO/vaccinations/$recordId/mark-completed"
    fun milestones(childId: String) = "$NINEMO/milestones/children/$childId"
    fun milestonesMonth(childId: String, month: Int) = "$NINEMO/milestones/children/$childId/month/$month"
    fun milestoneAchieve(docId: String) = "$NINEMO/milestones/$docId/achieve"

    // Diet
    const val DIET_SEARCH = "$NINEMO/diet/search"

    // Pregnancy profile (NM-B-167) — POST creates, GET returns the active one
    const val PREGNANCY_PROFILE = "$NINEMO/profiles/pregnancy"

    // Child profiles (NM-B-169) — list/get/patch; childId is now recoverable after reinstall
    const val CHILDREN = "$NINEMO/children"
    fun child(childId: String) = "$NINEMO/children/$childId"

    fun modeTransition(pregnancyProfileId: String) =
        "$NINEMO/mode/transition-to-postnatal/$pregnancyProfileId"

    // Community
    const val CLUBS_JOIN = "$NINEMO/community/clubs/join"
    const val CLUBS = "$NINEMO/community/clubs"
    fun clubMessages(clubId: String, channelId: String) =
        "$NINEMO/community/clubs/$clubId/channels/$channelId/messages"
    fun deleteMessage(messageId: String) = "$NINEMO/community/messages/$messageId"
    const val CONTENT = "$NINEMO/community/content"

    // STOMP (community-service)
    const val WS_CONNECT = "/ws/connect"
    fun chatSend(clubId: String, channelId: String) = "/app/chat.send/$clubId/$channelId"
    fun chatTopic(clubId: String, channelId: String) = "/topic/club.$clubId.$channelId"

    // Notifications
    const val DEVICES = "notifications/devices"
}
