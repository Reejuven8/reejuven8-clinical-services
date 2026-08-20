package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.ConsentGrantRequest
import com.reejuven8.ninemo.shared.model.ConsentResponse
import com.reejuven8.ninemo.shared.model.DoctorSummaryResponse
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ConsentRepository(private val client: HttpClient) {
    suspend fun grant(doctorId: String, expiresAt: String): Result<ConsentResponse> = runCatching {
        client.post(apiUrl(ApiRoutes.CONSENT_GRANT)) {
            contentType(ContentType.Application.Json)
            setBody(ConsentGrantRequest(doctorId, expiresAt))
        }.body<ApiResponse<ConsentResponse>>().data ?: error("Missing consent in response")
    }

    suspend fun searchDoctorByPhone(phoneNumber: String): Result<DoctorSummaryResponse> = runCatching {
        client.get(apiUrl(ApiRoutes.DOCTOR_SEARCH)) {
            parameter("phoneNumber", phoneNumber)
        }.body<ApiResponse<DoctorSummaryResponse>>().data ?: error("Doctor not found")
    }

    suspend fun list(): Result<List<ConsentResponse>> = runCatching {
        client.get(apiUrl(ApiRoutes.CONSENT_LIST))
            .body<ApiResponse<List<ConsentResponse>>>().data ?: emptyList()
    }

    suspend fun revoke(consentId: String): Result<Unit> = runCatching {
        client.post(apiUrl(ApiRoutes.consentRevoke(consentId)))
        Unit
    }
}
