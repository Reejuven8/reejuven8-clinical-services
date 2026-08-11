package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.DevelopmentalMilestoneResponse
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put

class MilestoneRepository(private val client: HttpClient) {
    suspend fun forChild(childId: String): Result<List<DevelopmentalMilestoneResponse>> = runCatching {
        client.get(apiUrl(ApiRoutes.milestones(childId)))
            .body<ApiResponse<List<DevelopmentalMilestoneResponse>>>().data ?: emptyList()
    }

    // get-or-create for a given month check-in.
    suspend fun forMonth(childId: String, month: Int): Result<DevelopmentalMilestoneResponse> = runCatching {
        client.get(apiUrl(ApiRoutes.milestonesMonth(childId, month)))
            .body<ApiResponse<DevelopmentalMilestoneResponse>>().data ?: error("Missing check-in in response")
    }

    // PUT, query params: milestoneName is the EXACT milestone text (no short key), achieved required.
    suspend fun achieve(documentId: String, milestoneName: String, achieved: Boolean): Result<DevelopmentalMilestoneResponse> =
        runCatching {
            client.put(apiUrl(ApiRoutes.milestoneAchieve(documentId))) {
                parameter("milestoneName", milestoneName)
                parameter("achieved", achieved)
            }.body<ApiResponse<DevelopmentalMilestoneResponse>>().data ?: error("Missing check-in in response")
        }
}
