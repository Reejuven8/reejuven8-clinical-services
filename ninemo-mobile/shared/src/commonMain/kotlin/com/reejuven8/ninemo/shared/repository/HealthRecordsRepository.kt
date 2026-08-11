package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.HealthRecordResponse
import com.reejuven8.ninemo.shared.model.SpringPage
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class HealthRecordsRepository(private val client: HttpClient) {
    suspend fun list(page: Int = 0, size: Int = 20): Result<SpringPage<HealthRecordResponse>> = runCatching {
        client.get(apiUrl(ApiRoutes.RECORDS)) {
            parameter("page", page)
            parameter("size", size)
        }.body<ApiResponse<SpringPage<HealthRecordResponse>>>().data ?: SpringPage()
    }

    suspend fun get(id: String): Result<HealthRecordResponse> = runCatching {
        client.get(apiUrl(ApiRoutes.record(id)))
            .body<ApiResponse<HealthRecordResponse>>().data ?: error("Missing record in response")
    }
}
