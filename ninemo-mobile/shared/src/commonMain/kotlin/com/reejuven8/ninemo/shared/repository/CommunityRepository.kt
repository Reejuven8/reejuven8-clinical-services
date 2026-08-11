package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.Channel
import com.reejuven8.ninemo.shared.model.ChatMessageResponse
import com.reejuven8.ninemo.shared.model.ClubResponse
import com.reejuven8.ninemo.shared.model.JoinClubRequest
import com.reejuven8.ninemo.shared.model.SpringPage
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * P20 Community — Due Date Clubs + chat history (REST). Live chat is [ChatSocketClient].
 * X-User-Id is injected by the gateway from the JWT, so no manual header here.
 */
class CommunityRepository(private val client: HttpClient) {

    suspend fun listClubs(): Result<List<ClubResponse>> = runCatching {
        client.get(apiUrl(ApiRoutes.CLUBS))
            .body<ApiResponse<List<ClubResponse>>>().data ?: emptyList()
    }

    suspend fun getClub(clubId: String): Result<ClubResponse> = runCatching {
        client.get(apiUrl("${ApiRoutes.CLUBS}/$clubId"))
            .body<ApiResponse<ClubResponse>>().data ?: error("Club not found")
    }

    suspend fun getChannels(clubId: String): Result<List<Channel>> = runCatching {
        client.get(apiUrl("${ApiRoutes.CLUBS}/$clubId/channels"))
            .body<ApiResponse<List<Channel>>>().data ?: emptyList()
    }

    suspend fun join(dueDateMonth: String, alias: String): Result<ClubResponse> = runCatching {
        client.post(apiUrl(ApiRoutes.CLUBS_JOIN)) {
            contentType(ContentType.Application.Json)
            setBody(JoinClubRequest(dueDateMonth, alias))
        }.body<ApiResponse<ClubResponse>>().data ?: error("Join failed")
    }

    /** Paginated history, newest-first (server order). */
    suspend fun history(
        clubId: String,
        channelId: String,
        page: Int = 0,
        size: Int = 50,
    ): Result<List<ChatMessageResponse>> = runCatching {
        client.get(apiUrl(ApiRoutes.clubMessages(clubId, channelId))) {
            parameter("page", page)
            parameter("size", size)
        }.body<ApiResponse<SpringPage<ChatMessageResponse>>>().data?.content ?: emptyList()
    }

    suspend fun deleteMessage(clubId: String, channelId: String, messageId: String): Result<Unit> = runCatching {
        client.delete(apiUrl("${ApiRoutes.clubMessages(clubId, channelId)}/$messageId"))
        Unit
    }
}
