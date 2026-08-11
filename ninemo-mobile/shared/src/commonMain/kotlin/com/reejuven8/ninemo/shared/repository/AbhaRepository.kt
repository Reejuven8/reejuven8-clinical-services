package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.AbhaAddressRequest
import com.reejuven8.ninemo.shared.model.AbhaOtpGenerateRequest
import com.reejuven8.ninemo.shared.model.AbhaOtpGenerateResponse
import com.reejuven8.ninemo.shared.model.AbhaOtpVerifyRequest
import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import com.reejuven8.ninemo.shared.session.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AbhaRepository(
    private val client: HttpClient,
    private val session: SessionStore,
) {
    suspend fun generateOtp(mobileNumber: String): Result<AbhaOtpGenerateResponse> = runCatching {
        client.post(apiUrl(ApiRoutes.ABHA_OTP_GENERATE)) {
            contentType(ContentType.Application.Json)
            setBody(AbhaOtpGenerateRequest(mobileNumber))
        }.body<ApiResponse<AbhaOtpGenerateResponse>>().data ?: error("Missing txnId in response")
    }

    suspend fun verifyOtp(txnId: String, encryptedOtp: String): Result<AbhaOtpGenerateResponse> = runCatching {
        client.post(apiUrl(ApiRoutes.ABHA_OTP_VERIFY)) {
            contentType(ContentType.Application.Json)
            setBody(AbhaOtpVerifyRequest(txnId, encryptedOtp))
        }.body<ApiResponse<AbhaOtpGenerateResponse>>().data ?: error("Missing txnId in response")
    }

    // Requires X-User-Id (not the bearer token) — AbhaController reads the patient's raw UUID.
    suspend fun setAddress(txnId: String, preferredAddress: String): Result<Unit> = runCatching {
        val userId = session.tokens()?.userId ?: error("Not authenticated")
        client.post(apiUrl(ApiRoutes.ABHA_ADDRESS)) {
            header("X-User-Id", userId)
            contentType(ContentType.Application.Json)
            setBody(AbhaAddressRequest(txnId, preferredAddress))
        }
        Unit
    }
}
