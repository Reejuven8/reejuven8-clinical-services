package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.SummaryCardResponse
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

// SummaryCardController takes patientId as a path variable (not X-User-Id-resolved) —
// the mobile app always passes the current user's own id (this is a self-view flash card).
class SummaryCardRepository(private val client: HttpClient) {
    suspend fun get(patientId: String): Result<SummaryCardResponse> = runCatching {
        client.get(apiUrl(ApiRoutes.summaryCard(patientId)))
            .body<ApiResponse<SummaryCardResponse>>().data ?: error("Missing summary card in response")
    }
}
