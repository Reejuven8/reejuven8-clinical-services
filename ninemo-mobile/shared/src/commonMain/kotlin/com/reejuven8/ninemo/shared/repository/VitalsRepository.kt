package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.VitalType
import com.reejuven8.ninemo.shared.model.VitalsLogRequest
import com.reejuven8.ninemo.shared.model.VitalsLogResponse
import com.reejuven8.ninemo.shared.model.VitalsMeasurements
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class VitalsRepository(private val client: HttpClient) {
    suspend fun log(vitalType: VitalType, measurements: VitalsMeasurements, source: String? = "MANUAL"): Result<VitalsLogResponse> =
        runCatching {
            client.post(apiUrl(ApiRoutes.VITALS)) {
                contentType(ContentType.Application.Json)
                setBody(VitalsLogRequest(vitalType.name, measurements, source))
            }.body<ApiResponse<VitalsLogResponse>>().data ?: error("Missing vitals log in response")
        }

    suspend fun history(vitalType: VitalType): Result<List<VitalsLogResponse>> = runCatching {
        client.get(apiUrl(ApiRoutes.vitalsByType(vitalType.name)))
            .body<ApiResponse<List<VitalsLogResponse>>>().data ?: emptyList()
    }
}
