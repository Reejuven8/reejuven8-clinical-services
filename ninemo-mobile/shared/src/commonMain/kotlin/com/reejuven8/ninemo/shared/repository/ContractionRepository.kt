package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.ContractionRequest
import com.reejuven8.ninemo.shared.model.ContractionSessionResponse
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ContractionRepository(private val client: HttpClient) {
    suspend fun startSession(): Result<ContractionSessionResponse> = runCatching {
        client.post(apiUrl(ApiRoutes.CONTRACTION_SESSIONS))
            .body<ApiResponse<ContractionSessionResponse>>().data ?: error("Missing session in response")
    }

    suspend fun addContraction(sessionId: String, durationSeconds: Int, intensity: String?): Result<ContractionSessionResponse> =
        runCatching {
            client.put(apiUrl(ApiRoutes.contractionAdd(sessionId))) {
                contentType(ContentType.Application.Json)
                setBody(ContractionRequest(durationSeconds, intensity))
            }.body<ApiResponse<ContractionSessionResponse>>().data ?: error("Missing session in response")
        }

    suspend fun endSession(sessionId: String): Result<ContractionSessionResponse> = runCatching {
        client.put(apiUrl(ApiRoutes.contractionEnd(sessionId)))
            .body<ApiResponse<ContractionSessionResponse>>().data ?: error("Missing session in response")
    }
}
