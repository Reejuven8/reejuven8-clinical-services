package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.DietFoodSafetyResponse
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class DietRepository(private val client: HttpClient) {
    suspend fun search(query: String): Result<List<DietFoodSafetyResponse>> = runCatching {
        client.get(apiUrl(ApiRoutes.DIET_SEARCH)) {
            parameter("q", query)
        }.body<ApiResponse<List<DietFoodSafetyResponse>>>().data ?: emptyList()
    }
}
