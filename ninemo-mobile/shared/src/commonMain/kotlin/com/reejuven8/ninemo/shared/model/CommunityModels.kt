package com.reejuven8.ninemo.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Community (verified against ninemo-community-service DTOs + entities) ─────────
// GOTCHA: backend Lombok boolean getters serialize is-prefix-stripped by Jackson
// (isDefault -> "default", isMember -> "member"). No global PropertyNamingStrategy set,
// so @SerialName maps the wire name. Defaults guard the paths where the key is absent
// (e.g. join returns the raw DueDateClub entity, which has no "member" field at all).
@Serializable
data class Channel(
    val channelId: String,
    val name: String,
    val description: String? = null,
    @SerialName("default") val isDefault: Boolean = false,
)

@Serializable
data class ClubMember(
    val userId: String,
    val alias: String,
    val joinedAt: String? = null,
)

// Two backend shapes deserialize into this one model:
//   • GET /clubs, GET /clubs/{id}  -> ClubResponse DTO (has "member", no members list)
//   • POST /clubs/join            -> raw DueDateClub entity (has members list, no "member")
// Both members + isMember are optional so either shape maps cleanly.
@Serializable
data class ClubResponse(
    val id: String,
    val clubName: String,
    val dueDateMonth: String,
    val memberCount: Int = 0,
    @SerialName("member") val isMember: Boolean = false,
    val callerAlias: String? = null,
    val members: List<ClubMember> = emptyList(),
    val channels: List<Channel> = emptyList(),
)

@Serializable
data class JoinClubRequest(
    val dueDateMonth: String,
    val alias: String,
)

// senderId intentionally absent — backend derives sender identity from the authenticated
// STOMP principal (JWT on CONNECT), never from the payload. Sending it would be ignored.
@Serializable
data class SendMessageRequest(
    val senderAlias: String,
    val messageBody: String,
    val messageType: String = "TEXT",
    val replyToMessageId: String? = null,
    val imageUrl: String? = null,
)

// Mirrors ChatMessageResponse DTO exactly — note: no isDeleted field on the response
// (it lives on the entity only; soft-deleted messages are filtered out server-side).
@Serializable
data class ChatMessageResponse(
    val id: String,
    val clubId: String,
    val channelId: String,
    val senderId: String,
    val senderAlias: String,
    val messageType: String,
    val messageBody: String,
    val replyToMessageId: String? = null,
    val imageUrl: String? = null,
    val sentAt: String,
)

// ─── Content ──────────────────────────────────────────────────────────────────
// GET /content returns Spring Page<ContentArticle> (use SpringPage<ContentArticle>).
// by-category / by-week return a plain List<ContentArticle>.
@Serializable
data class ContentArticle(
    val id: String,
    val title: String,
    val body: String,
    val summary: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val gestationalWeeks: List<Int> = emptyList(),
    val author: String? = null,
    val imageUrl: String? = null,
    val publishedAt: String? = null,
)
