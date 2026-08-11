package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.session.SessionStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-scoped session gate + active-profile ids. Drives auth routing and mode. */
class SessionViewModel(private val session: SessionStore) : ViewModel() {

    val isAuthenticated: StateFlow<Boolean> =
        session.isAuthenticated.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val activePregnancyId: StateFlow<String?> = session.activePregnancyId
    val activeChildId: StateFlow<String?> = session.activeChildId
    val activeChildName: StateFlow<String?> = session.activeChildName

    val hasCompletedOnboarding: StateFlow<Boolean> =
        session.hasCompletedOnboarding.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val userId: StateFlow<String?> =
        session.userId.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** child mode once a child profile is active (post P23 transition). */
    val isChildMode: StateFlow<Boolean> =
        session.activeChildId
            .let { flow ->
                kotlinx.coroutines.flow.MutableStateFlow(flow.value != null).also { out ->
                    viewModelScope.launch { flow.collect { out.value = it != null } }
                }
            }

    fun setActiveChild(id: String?, name: String? = null) =
        viewModelScope.launch { session.setActiveChild(id, name) }
    fun setActivePregnancy(id: String?) { session.activePregnancyId.value = id }

    fun logout() = viewModelScope.launch { session.clear() }
}
