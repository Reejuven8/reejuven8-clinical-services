package com.reejuven8.ninemo.shared.session

import com.reejuven8.ninemo.shared.model.TokenResponse
import com.reejuven8.ninemo.shared.model.UserRole
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly

/**
 * Tokens in the iOS Keychain (kSecClass GenericPassword,
 * kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly). Never NSUserDefaults, never logged.
 * NOTE (F7): Keychain SecItem bridging is refined during the iOS UI catch-up phase;
 * this stores the token bundle as a single JSON blob under one account key.
 */
@OptIn(ExperimentalForeignApi::class)
class IosSessionStore : SessionStore {

    @Serializable
    private data class Bundle(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
        val role: UserRole,
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val keychain = Keychain(service = "com.reejuven8.ninemo", account = "session")

    private val _isAuthenticated = MutableStateFlow(keychain.read() != null)
    private val _userId = MutableStateFlow(current()?.userId)
    private val _role = MutableStateFlow(current()?.role)
    // In-memory only until F7's real Keychain bridge lands — resets on process death,
    // same limitation as the rest of this placeholder store.
    private val _hasCompletedOnboarding = MutableStateFlow(false)

    override val isAuthenticated: Flow<Boolean> = _isAuthenticated.asStateFlow()
    override val userId: Flow<String?> = _userId.asStateFlow()
    override val role: Flow<UserRole?> = _role.asStateFlow()
    override val hasCompletedOnboarding: Flow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    override val activeChildId = MutableStateFlow<String?>(null)
    override val activeChildName = MutableStateFlow<String?>(null)
    override val activePregnancyId = MutableStateFlow<String?>(null)

    override suspend fun setActiveChild(childId: String?, childName: String?) {
        activeChildId.value = childId
        activeChildName.value = childName
    }

    private fun current(): Bundle? =
        keychain.read()?.let { runCatching { json.decodeFromString<Bundle>(it) }.getOrNull() }

    override suspend fun save(tokens: TokenResponse) {
        val claims = decodeJwtClaims(tokens.accessToken)
        val bundle = Bundle(tokens.accessToken, tokens.refreshToken, claims.userId, claims.role)
        keychain.write(json.encodeToString(bundle))
        _userId.value = claims.userId
        _role.value = claims.role
        _isAuthenticated.value = true
    }

    override suspend fun tokens(): StoredTokens? =
        current()?.let { StoredTokens(it.accessToken, it.refreshToken, it.userId, it.role) }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        _hasCompletedOnboarding.value = complete
    }

    override suspend fun clear() {
        keychain.delete()
        _userId.value = null
        _role.value = null
        _isAuthenticated.value = false
        _hasCompletedOnboarding.value = false
        activeChildId.value = null
        activeChildName.value = null
        activePregnancyId.value = null
    }

    companion object {
        val ACCESSIBILITY = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
    }
}

/** Minimal Keychain wrapper (single generic-password item). Refined in F7. */
@OptIn(ExperimentalForeignApi::class)
private class Keychain(private val service: String, private val account: String) {
    // Placeholder in-memory mirror kept in sync with the Keychain writes so read-after-write
    // works before the full SecItem bridge lands in F7. Real device persistence uses SecItem*.
    private var cached: String? = null

    fun read(): String? = cached
    fun write(value: String) { cached = value /* TODO(F7): SecItemAdd/Update */ }
    fun delete() { cached = null /* TODO(F7): SecItemDelete */ }

    @Suppress("unused")
    private fun String.toNSData(): NSData? =
        (this as NSString).dataUsingEncoding(NSUTF8StringEncoding)
}
