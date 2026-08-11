package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.LoginRequest
import com.reejuven8.ninemo.shared.model.OtpSendRequest
import com.reejuven8.ninemo.shared.model.RegisterRequest
import com.reejuven8.ninemo.shared.model.TokenResponse
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import com.reejuven8.ninemo.shared.session.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.authProviders
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthRepository(
    private val client: HttpClient,
    private val session: SessionStore,
) {
    // The Ktor Bearer Auth plugin caches the result of loadTokens on first use. The pre-login
    // requests (otp/send, login) cache a null token, so without invalidating that cache after
    // saving fresh tokens, EVERY authenticated request goes out with no Authorization header
    // → gateway 401s the whole app. Clearing forces loadTokens to re-read the saved tokens.
    private fun refreshAuthToken() {
        client.authProviders
            .filterIsInstance<BearerAuthProvider>()
            .firstOrNull()
            ?.clearToken()
    }
    suspend fun sendOtp(phoneNumber: String): Result<Unit> = runCatching {
        client.post(apiUrl(ApiRoutes.OTP_SEND)) {
            contentType(ContentType.Application.Json)
            setBody(OtpSendRequest(phoneNumber))
        }
        Unit
    }

    suspend fun login(phoneNumber: String, otp: String): Result<Unit> = runCatching {
        val tokens = client.post(apiUrl(ApiRoutes.LOGIN)) {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(phoneNumber, otp))
        }.body<ApiResponse<TokenResponse>>().data ?: error("Login response missing token data")
        session.save(tokens)
        refreshAuthToken()
    }

    suspend fun register(request: RegisterRequest): Result<Unit> = runCatching {
        val tokens = client.post(apiUrl(ApiRoutes.REGISTER)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<ApiResponse<TokenResponse>>().data ?: error("Register response missing token data")
        session.save(tokens)
        refreshAuthToken()
    }

    suspend fun logout(): Result<Unit> {
        val result = runCatching { client.post(apiUrl(ApiRoutes.LOGOUT)) }
        session.clear()
        return result.map { }
    }
}
