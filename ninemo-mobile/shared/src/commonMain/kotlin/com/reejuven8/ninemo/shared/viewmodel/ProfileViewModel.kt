package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.UserRole
import com.reejuven8.ninemo.shared.session.SessionStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * P22 — Profile & Settings. Identity + role come from the session (no dedicated profile
 * endpoint yet); actions route to ABHA link / consent manager and logout.
 */
class ProfileViewModel(private val session: SessionStore) : ViewModel() {

    val userId: StateFlow<String?> =
        session.userId.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val role: StateFlow<UserRole?> =
        session.role.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun logout() = viewModelScope.launch { session.clear() }
}
