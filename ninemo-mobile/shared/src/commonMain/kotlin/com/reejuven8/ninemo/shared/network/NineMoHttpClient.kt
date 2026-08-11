package com.reejuven8.ninemo.shared.network

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.TokenResponse
import com.reejuven8.ninemo.shared.session.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.call.body
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Full gateway URL for a relative ApiRoutes path. Repositories always build absolute URLs. */
fun apiUrl(path: String): String = "${PlatformConfig.baseUrl}/$path"

/**
 * Ktor client factory. The Auth plugin natively implements load→401→refresh→retry,
 * serialized internally (single-flight) so parallel 401s share one refresh.
 * Mirrors Cross_Platform_Strategy.md §5.1.
 */
fun nineMoHttpClient(
    engine: HttpClientEngine,
    session: SessionStore,
): HttpClient = HttpClient(engine) {
    expectSuccess = true

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            },
        )
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 15_000
    }

    install(Logging)

    install(Auth) {
        bearer {
            loadTokens {
                session.tokens()?.let { BearerTokens(it.accessToken, it.refreshToken) }
            }
            refreshTokens {
                val refresh = oldTokens?.refreshToken ?: return@refreshTokens null
                runCatching {
                    client.post(apiUrl(ApiRoutes.REFRESH)) {
                        header("Authorization", "Bearer $refresh")
                        markAsRefreshTokenRequest()
                        contentType(ContentType.Application.Json)
                    }.body<ApiResponse<TokenResponse>>().data
                }.getOrNull()
                    ?.also { session.save(it) }
                    ?.let { BearerTokens(it.accessToken, it.refreshToken) }
                    ?: run { session.clear(); null }
            }
        }
    }

    defaultRequest {
        url("${PlatformConfig.baseUrl}/")
    }
}
