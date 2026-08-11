package com.reejuven8.ninemo.shared.session

import com.reejuven8.ninemo.shared.model.TokenResponse
import com.reejuven8.ninemo.shared.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

data class StoredTokens(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val role: UserRole,
)

/**
 * Session state — tokens + active-profile ids (was RN authSlice + uiSlice).
 * Tokens are stored encrypted per platform (Keystore-wrapped DataStore on Android,
 * Keychain on iOS). Never plain storage, never logged. Cross_Platform_Strategy.md §5.6.
 */
interface SessionStore {
    suspend fun save(tokens: TokenResponse)
    suspend fun tokens(): StoredTokens?
    suspend fun clear()

    val isAuthenticated: Flow<Boolean>
    val userId: Flow<String?>
    val role: Flow<UserRole?>

    val activeChildId: MutableStateFlow<String?>
    val activePregnancyId: MutableStateFlow<String?>

    // Child identity survives process death via encrypted storage — there is NO backend
    // endpoint to list/rehydrate children (NM-B-169), so the transition response is the
    // only source of childId. childName is local-only (server returns null after transition).
    val activeChildName: MutableStateFlow<String?>
    suspend fun setActiveChild(childId: String?, childName: String?)

    // Local-only until NM-B-167 ships a GET pregnancy-profile endpoint to check against.
    // Tracks whether the P4 wizard has been completed/dismissed for the current session.
    val hasCompletedOnboarding: Flow<Boolean>
    suspend fun setOnboardingComplete(complete: Boolean)
}
