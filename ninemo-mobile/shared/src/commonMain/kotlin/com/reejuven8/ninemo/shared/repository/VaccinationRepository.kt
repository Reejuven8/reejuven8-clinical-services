package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.VaccinationRecordResponse
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put

class VaccinationRepository(private val client: HttpClient) {
    // Idempotent-generate: first call persists the full IAP schedule, later calls return it.
    suspend fun schedule(childId: String): Result<List<VaccinationRecordResponse>> = runCatching {
        client.get(apiUrl(ApiRoutes.vaccinationSchedule(childId)))
            .body<ApiResponse<List<VaccinationRecordResponse>>>().data ?: emptyList()
    }

    // PUT, query params (both optional server-side — date defaults to today).
    suspend fun markCompleted(
        vaccinationId: String,
        administeredDate: String? = null,
        administeredBy: String? = null,
    ): Result<VaccinationRecordResponse> = runCatching {
        client.put(apiUrl(ApiRoutes.vaccinationMarkCompleted(vaccinationId))) {
            administeredDate?.let { parameter("administeredDate", it) }
            administeredBy?.let { parameter("administeredBy", it) }
        }.body<ApiResponse<VaccinationRecordResponse>>().data ?: error("Missing record in response")
    }
}
