package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.GrowthInputRequest
import com.reejuven8.ninemo.shared.model.GrowthMeasurementResponse
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class GrowthRepository(private val client: HttpClient) {
    suspend fun history(childId: String): Result<List<GrowthMeasurementResponse>> = runCatching {
        client.get(apiUrl(ApiRoutes.growth(childId)))
            .body<ApiResponse<List<GrowthMeasurementResponse>>>().data ?: emptyList()
    }

    suspend fun add(childId: String, request: GrowthInputRequest): Result<GrowthMeasurementResponse> = runCatching {
        client.post(apiUrl(ApiRoutes.growth(childId))) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<ApiResponse<GrowthMeasurementResponse>>().data ?: error("Missing measurement in response")
    }
}
