package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.ChildProfileResponse
import com.reejuven8.ninemo.shared.model.ChildProfileUpdateRequest
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * NM-B-169. The transition now takes an optional body (baby details are persisted rather
 * than dropped), and children are listable, so `childId` no longer has to be cached
 * locally forever to stay reachable.
 */
class ModeTransitionRepository(private val client: HttpClient) {

    suspend fun transition(
        pregnancyProfileId: String,
        details: ChildProfileUpdateRequest? = null,
    ): Result<ChildProfileResponse> = runCatching {
        client.post(apiUrl(ApiRoutes.modeTransition(pregnancyProfileId))) {
            if (details != null) {
                contentType(ContentType.Application.Json)
                setBody(details)
            }
        }.body<ApiResponse<ChildProfileResponse>>().data ?: error("Missing child profile in response")
    }

    suspend fun listChildren(): Result<List<ChildProfileResponse>> = runCatching {
        client.get(apiUrl(ApiRoutes.CHILDREN))
            .body<ApiResponse<List<ChildProfileResponse>>>().data ?: emptyList()
    }

    suspend fun getChild(childId: String): Result<ChildProfileResponse> = runCatching {
        client.get(apiUrl(ApiRoutes.child(childId)))
            .body<ApiResponse<ChildProfileResponse>>().data ?: error("Missing child profile in response")
    }

    suspend fun updateChild(
        childId: String,
        request: ChildProfileUpdateRequest,
    ): Result<ChildProfileResponse> = runCatching {
        client.patch(apiUrl(ApiRoutes.child(childId))) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<ApiResponse<ChildProfileResponse>>().data ?: error("Missing child profile in response")
    }
}
