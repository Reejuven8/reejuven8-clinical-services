package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.KickCounterSessionResponse
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.put

// No GET history endpoint exists backend-side — repository method is unused/unexposed.
class KickCounterRepository(private val client: HttpClient) {
    suspend fun startSession(): Result<KickCounterSessionResponse> = runCatching {
        client.post(apiUrl(ApiRoutes.KICK_SESSIONS))
            .body<ApiResponse<KickCounterSessionResponse>>().data ?: error("Missing session in response")
    }

    suspend fun addKick(sessionId: String): Result<KickCounterSessionResponse> = runCatching {
        client.put(apiUrl(ApiRoutes.kickAdd(sessionId)))
            .body<ApiResponse<KickCounterSessionResponse>>().data ?: error("Missing session in response")
    }

    suspend fun endSession(sessionId: String): Result<KickCounterSessionResponse> = runCatching {
        client.put(apiUrl(ApiRoutes.kickEnd(sessionId)))
            .body<ApiResponse<KickCounterSessionResponse>>().data ?: error("Missing session in response")
    }
}
