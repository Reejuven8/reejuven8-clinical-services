package com.reejuven8.ninemo.shared.session

import com.reejuven8.ninemo.shared.model.UserRole
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class JwtAccessClaims(val userId: String, val role: UserRole)

/**
 * identity-abha-service's TokenResponse carries only accessToken/refreshToken/tokenType/expiresIn —
 * userId and role are JWT claims (sub, role), not response fields. Decode-only, no signature
 * verification (the server is the trust boundary; the client just needs the claims for routing).
 */
@OptIn(ExperimentalEncodingApi::class)
fun decodeJwtClaims(accessToken: String): JwtAccessClaims {
    val payloadSegment = accessToken.split(".").getOrNull(1)
        ?: error("Malformed JWT: missing payload segment")
    val payloadJson = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
        .decode(payloadSegment)
        .decodeToString()
    val claims = Json.parseToJsonElement(payloadJson).jsonObject
    val userId = claims["sub"]?.jsonPrimitive?.content
        ?: error("JWT missing 'sub' claim")
    val role = claims["role"]?.jsonPrimitive?.content?.let(UserRole::valueOf)
        ?: error("JWT missing 'role' claim")
    return JwtAccessClaims(userId, role)
}
