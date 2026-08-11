package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.SymptomItem
import com.reejuven8.ninemo.shared.model.SymptomLogHistoryEntry
import com.reejuven8.ninemo.shared.model.SymptomLogRequest
import com.reejuven8.ninemo.shared.model.SymptomLogResponse
import com.reejuven8.ninemo.shared.model.VitalsAtLog
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class SymptomRepository(private val client: HttpClient) {
    suspend fun log(symptoms: List<SymptomItem>, vitalsAtLog: VitalsAtLog?): Result<SymptomLogResponse> = runCatching {
        client.post(apiUrl(ApiRoutes.SYMPTOMS)) {
            contentType(ContentType.Application.Json)
            setBody(SymptomLogRequest(symptoms, vitalsAtLog))
        }.body<ApiResponse<SymptomLogResponse>>().data ?: error("Missing symptom log in response")
    }

    suspend fun history(): Result<List<SymptomLogHistoryEntry>> = runCatching {
        client.get(apiUrl(ApiRoutes.SYMPTOMS))
            .body<ApiResponse<List<SymptomLogHistoryEntry>>>().data ?: emptyList()
    }
}
