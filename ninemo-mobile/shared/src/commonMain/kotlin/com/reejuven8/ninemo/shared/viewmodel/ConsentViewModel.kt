package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.ConsentResponse
import com.reejuven8.ninemo.shared.repository.ConsentRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * P9 — Consent Manager. No doctor search/lookup endpoint exists backend-side — grant flow
 * takes the doctor's raw NineMo ID directly, matching the design mockup's manual-entry flow.
 * consentStatus never auto-flips to EXPIRED server-side, so [displayStatus] compares
 * expiresAt itself for GRANTED-but-past-expiry consents (rendering already-provided data).
 */
class ConsentViewModel(private val repository: ConsentRepository) : ViewModel() {

    private val _consents = MutableStateFlow<UiState<List<ConsentResponse>>>(UiState.Loading)
    val consents: StateFlow<UiState<List<ConsentResponse>>> = _consents.asStateFlow()

    private val _grantState = MutableStateFlow<UiState<ConsentResponse>>(UiState.Empty)
    val grantState: StateFlow<UiState<ConsentResponse>> = _grantState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _consents.value = UiState.Loading
            repository.list().fold(
                onSuccess = { _consents.value = if (it.isEmpty()) UiState.Empty else UiState.Success(it) },
                onFailure = { _consents.value = UiState.Error(it) },
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    fun grant(doctorId: String, durationDays: Int) {
        if (doctorId.isBlank()) return
        val expiresAt = (Clock.System.now() + durationDays.days).toString()
        viewModelScope.launch {
            _grantState.value = UiState.Loading
            repository.grant(doctorId, expiresAt).fold(
                onSuccess = {
                    _grantState.value = UiState.Success(it)
                    load()
                },
                onFailure = { _grantState.value = UiState.Error(it) },
            )
        }
    }

    fun revoke(consentId: String) {
        viewModelScope.launch {
            repository.revoke(consentId).onSuccess { load() }
        }
    }

    fun resetGrantState() {
        _grantState.value = UiState.Empty
    }
}

fun displayStatus(consent: ConsentResponse, nowIso: String): String {
    if (consent.consentStatus == "GRANTED" && consent.expiresAt < nowIso) return "EXPIRED"
    return consent.consentStatus
}
