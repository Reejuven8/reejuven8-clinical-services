package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.ContractionSessionResponse
import com.reejuven8.ninemo.shared.repository.ContractionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * P14 — Contraction Timer. Client only times duration locally (a stopwatch is inherently
 * client-side UX) and sends durationSeconds to the server — isLaborPattern/alertTriggered/
 * averages/intervalFromPreviousSeconds are all server-computed, never derived here.
 */
class ContractionViewModel(private val repository: ContractionRepository) : ViewModel() {

    private val _session = MutableStateFlow<UiState<ContractionSessionResponse>>(UiState.Empty)
    val session: StateFlow<UiState<ContractionSessionResponse>> = _session.asStateFlow()

    private val _timerRunning = MutableStateFlow(false)
    val timerRunning: StateFlow<Boolean> = _timerRunning.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _selectedIntensity = MutableStateFlow<String?>(null)
    val selectedIntensity: StateFlow<String?> = _selectedIntensity.asStateFlow()

    private var tickJob: Job? = null

    fun startSession() {
        viewModelScope.launch {
            _session.value = UiState.Loading
            repository.startSession().fold(
                onSuccess = { _session.value = UiState.Success(it) },
                onFailure = { _session.value = UiState.Error(it) },
            )
        }
    }

    fun setIntensity(value: String) { _selectedIntensity.value = value }

    fun startContraction() {
        if (_timerRunning.value) return
        _elapsedSeconds.value = 0
        _timerRunning.value = true
        tickJob = viewModelScope.launch {
            while (_timerRunning.value) {
                delay(1_000)
                _elapsedSeconds.value += 1
            }
        }
    }

    fun stopContraction() {
        if (!_timerRunning.value) return
        _timerRunning.value = false
        tickJob?.cancel()
        val current = (_session.value as? UiState.Success)?.data ?: return
        val duration = _elapsedSeconds.value
        viewModelScope.launch {
            repository.addContraction(current.sessionId, duration, _selectedIntensity.value)
                .onSuccess { _session.value = UiState.Success(it) }
            _selectedIntensity.value = null
        }
    }

    fun endSession() {
        val current = (_session.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.endSession(current.sessionId).onSuccess { _session.value = UiState.Success(it) }
        }
    }
}
