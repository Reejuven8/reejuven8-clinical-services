package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.CreatePregnancyProfileRequest
import com.reejuven8.ninemo.shared.model.PregnancyProfileResponse
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * NM-B-167 is live: POST /ninemo/profiles/pregnancy creates the profile (201) and
 * GET returns the active one (404 when onboarding has not been done).
 *
 * The GET is what lets onboarding status survive a reinstall — before it existed the app
 * only knew from a local encrypted flag.
 */
class PregnancyProfileRepository(private val client: HttpClient) {

    suspend fun create(request: CreatePregnancyProfileRequest): Result<PregnancyProfileResponse> = runCatching {
        client.post(apiUrl(ApiRoutes.PREGNANCY_PROFILE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<ApiResponse<PregnancyProfileResponse>>().data ?: error("Missing profile in response")
    }

    /** Fails with a 404 when the user has no active pregnancy — treat that as "onboarding pending". */
    suspend fun getActive(): Result<PregnancyProfileResponse> = runCatching {
        client.get(apiUrl(ApiRoutes.PREGNANCY_PROFILE))
            .body<ApiResponse<PregnancyProfileResponse>>().data ?: error("Missing profile in response")
    }
}
