package com.reejuven8.ninemo.shared.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.reejuven8.ninemo.shared.model.TokenResponse
import com.reejuven8.ninemo.shared.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Tokens in Keystore-backed EncryptedSharedPreferences — real encryption at rest,
 * never plain storage, never logged (security rule). Values are AES-GCM encrypted
 * with a master key held in the Android Keystore.
 */
class AndroidSessionStore(context: Context) : SessionStore {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "ninemo_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _isAuthenticated = MutableStateFlow(prefs.getString(KEY_ACCESS, null) != null)
    private val _userId = MutableStateFlow(prefs.getString(KEY_USER_ID, null))
    private val _role = MutableStateFlow(prefs.getString(KEY_ROLE, null)?.let(UserRole::valueOf))
    private val _hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDED, false))

    override val isAuthenticated: Flow<Boolean> = _isAuthenticated.asStateFlow()
    override val userId: Flow<String?> = _userId.asStateFlow()
    override val role: Flow<UserRole?> = _role.asStateFlow()
    override val hasCompletedOnboarding: Flow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    override val activeChildId = MutableStateFlow(prefs.getString(KEY_CHILD_ID, null))
    override val activeChildName = MutableStateFlow(prefs.getString(KEY_CHILD_NAME, null))
    override val activePregnancyId = MutableStateFlow<String?>(null)

    override suspend fun setActiveChild(childId: String?, childName: String?) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_CHILD_ID, childId)
            .putString(KEY_CHILD_NAME, childName)
            .apply()
        activeChildId.value = childId
        activeChildName.value = childName
    }

    override suspend fun save(tokens: TokenResponse) = withContext(Dispatchers.IO) {
        val claims = decodeJwtClaims(tokens.accessToken)
        prefs.edit()
            .putString(KEY_ACCESS, tokens.accessToken)
            .putString(KEY_REFRESH, tokens.refreshToken)
            .putString(KEY_USER_ID, claims.userId)
            .putString(KEY_ROLE, claims.role.name)
            .apply()
        _userId.value = claims.userId
        _role.value = claims.role
        _isAuthenticated.value = true
    }

    override suspend fun tokens(): StoredTokens? = withContext(Dispatchers.IO) {
        val access = prefs.getString(KEY_ACCESS, null) ?: return@withContext null
        val refresh = prefs.getString(KEY_REFRESH, null) ?: return@withContext null
        val uid = prefs.getString(KEY_USER_ID, null) ?: return@withContext null
        val role = prefs.getString(KEY_ROLE, null)?.let(UserRole::valueOf) ?: return@withContext null
        StoredTokens(access, refresh, uid, role)
    }

    override suspend fun setOnboardingComplete(complete: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_ONBOARDED, complete).apply()
        _hasCompletedOnboarding.value = complete
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
        _userId.value = null
        _role.value = null
        _isAuthenticated.value = false
        _hasCompletedOnboarding.value = false
        activeChildId.value = null
        activeChildName.value = null
        activePregnancyId.value = null
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_ROLE = "role"
        const val KEY_ONBOARDED = "onboarding_complete"
        const val KEY_CHILD_ID = "active_child_id"
        const val KEY_CHILD_NAME = "active_child_name"
    }
}
