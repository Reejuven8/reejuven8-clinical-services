package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.ChildProfileResponse
import com.reejuven8.ninemo.shared.repository.ModeTransitionRepository
import com.reejuven8.ninemo.shared.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * P23 — "Baby is here!" Flips the app to child mode.
 *
 * Backend reality: transition-to-postnatal takes NO request body and returns a ChildProfile
 * with childName=null. So the baby's name entered here is stored LOCALLY only (in SessionStore)
 * for display — there is no backend endpoint to persist it, nor to update the child later
 * (NM-B-169). The whole flow is also gated on having an active pregnancyProfileId, which today
 * only exists once NM-B-167 (pregnancy profile creation) ships — until then [pregnancyMissing]
 * is true and the screen explains why the action is unavailable.
 */
class ModeTransitionViewModel(
    private val repository: ModeTransitionRepository,
    private val session: SessionStore,
) : ViewModel() {

    private val _pregnancyMissing = MutableStateFlow(false)
    val pregnancyMissing: StateFlow<Boolean> = _pregnancyMissing.asStateFlow()

    private val _transitionState = MutableStateFlow<UiState<ChildProfileResponse>>(UiState.Empty)
    val transitionState: StateFlow<UiState<ChildProfileResponse>> = _transitionState.asStateFlow()

    init {
        viewModelScope.launch {
            _pregnancyMissing.value = session.activePregnancyId.first() == null
        }
    }

    fun switchToChildMode(babyName: String) {
        viewModelScope.launch {
            val pregnancyId = session.activePregnancyId.first()
            if (pregnancyId == null) {
                _pregnancyMissing.value = true
                return@launch
            }
            _transitionState.value = UiState.Loading
            repository.transition(pregnancyId).fold(
                onSuccess = { child ->
                    // Server returns childName=null — keep the locally-entered name for display.
                    session.setActiveChild(child.id, babyName.ifBlank { null })
                    _transitionState.value = UiState.Success(child)
                },
                onFailure = { _transitionState.value = UiState.Error(it) },
            )
        }
    }
}
