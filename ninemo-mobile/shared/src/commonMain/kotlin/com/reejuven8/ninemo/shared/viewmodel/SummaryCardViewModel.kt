package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.SummaryCardResponse
import com.reejuven8.ninemo.shared.repository.SummaryCardRepository
import com.reejuven8.ninemo.shared.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** P16 Summary Card — patient's own flash card to hand to a doctor in person. */
class SummaryCardViewModel(
    private val repository: SummaryCardRepository,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<SummaryCardResponse>>(UiState.Loading)
    val state: StateFlow<UiState<SummaryCardResponse>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val patientId = session.userId.first()
            if (patientId == null) {
                _state.value = UiState.Error(IllegalStateException("Not authenticated"))
                return@launch
            }
            repository.get(patientId).fold(
                onSuccess = { _state.value = UiState.Success(it) },
                onFailure = { _state.value = UiState.Error(it) },
            )
        }
    }
}
