package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.KickCounterSessionResponse
import com.reejuven8.ninemo.shared.repository.KickCounterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** P13 — Kick Counter. isConcerning/durationTo10KicksMinutes are server-computed (WHO <10-in-120min rule). */
class KickCounterViewModel(private val repository: KickCounterRepository) : ViewModel() {

    private val _session = MutableStateFlow<UiState<KickCounterSessionResponse>>(UiState.Empty)
    val session: StateFlow<UiState<KickCounterSessionResponse>> = _session.asStateFlow()

    private val _kickTapCount = MutableStateFlow(0) // local tap counter — server is the source of truth for totalKicks
    val kickTapCount: StateFlow<Int> = _kickTapCount.asStateFlow()

    fun startSession() {
        viewModelScope.launch {
            _session.value = UiState.Loading
            _kickTapCount.value = 0
            repository.startSession().fold(
                onSuccess = { _session.value = UiState.Success(it) },
                onFailure = { _session.value = UiState.Error(it) },
            )
        }
    }

    fun tapKick() {
        val current = (_session.value as? UiState.Success)?.data ?: return
        _kickTapCount.value += 1
        viewModelScope.launch {
            repository.addKick(current.sessionId).onSuccess { _session.value = UiState.Success(it) }
        }
    }

    fun endSession() {
        val current = (_session.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.endSession(current.sessionId).onSuccess { _session.value = UiState.Success(it) }
        }
    }
}
