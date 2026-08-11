package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.TimelineResponse
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class TimelineRepository(private val client: HttpClient) {
    suspend fun current(): Result<TimelineResponse> = runCatching {
        client.get(apiUrl(ApiRoutes.TIMELINE_CURRENT))
            .body<ApiResponse<TimelineResponse>>().data ?: error("Missing timeline in response")
    }

    suspend fun week(week: Int): Result<TimelineResponse> = runCatching {
        client.get(apiUrl(ApiRoutes.timelineWeek(week)))
            .body<ApiResponse<TimelineResponse>>().data ?: error("Missing timeline in response")
    }
}
