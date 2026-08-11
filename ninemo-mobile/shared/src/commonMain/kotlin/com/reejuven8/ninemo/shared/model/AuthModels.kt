package com.reejuven8.ninemo.shared.model

import kotlinx.serialization.Serializable

// ─── Auth (mirrors identity-abha-service AuthController DTOs exactly) ─────────
@Serializable
data class OtpSendRequest(val phoneNumber: String)

@Serializable
data class LoginRequest(
    val phoneNumber: String,
    val otp: String,
)

@Serializable
data class RegisterRequest(
    val firstName: String,
    val middleName: String? = null,
    val lastName: String,
    val phoneNumber: String,
    val role: UserRole = UserRole.PATIENT,
    val dateOfBirth: String, // ISO yyyy-MM-dd
    val biologicalSex: BiologicalSex,
)

// tokenType/expiresIn are server-computed (always "Bearer"/900s); no userId/role here —
// decode from the access token JWT (see session/JwtDecoder.kt).
@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
)

// ─── ABHA enrollment (identity-abha-service AbhaController DTOs exactly) ──────
@Serializable
data class AbhaOtpGenerateRequest(val mobileNumber: String)

@Serializable
data class AbhaOtpGenerateResponse(val txnId: String, val message: String)

@Serializable
data class AbhaOtpVerifyRequest(val txnId: String, val encryptedOtp: String)

@Serializable
data class AbhaAddressRequest(val txnId: String, val preferredAddress: String)
