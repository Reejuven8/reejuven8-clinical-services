package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.CreatePregnancyProfileRequest
import com.reejuven8.ninemo.shared.model.DateBasis
import com.reejuven8.ninemo.shared.model.PregnancyProfileResponse
import com.reejuven8.ninemo.shared.repository.PregnancyProfileRepository
import com.reejuven8.ninemo.shared.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** P4 Onboarding — 3-step wizard (dates, body metrics, risk flags). Never computes EDD/BMI locally. */
class OnboardingViewModel(
    private val repository: PregnancyProfileRepository,
    private val session: SessionStore,
) : ViewModel() {

    private val _step = MutableStateFlow(1)
    val step: StateFlow<Int> = _step.asStateFlow()

    private val _dateBasis = MutableStateFlow(DateBasis())
    val dateBasis: StateFlow<DateBasis> = _dateBasis.asStateFlow()

    private val _ageYears = MutableStateFlow<Int?>(null)
    private val _heightCm = MutableStateFlow<Double?>(null)
    private val _prePregnancyWeightKg = MutableStateFlow<Double?>(null)
    private val _bloodGroup = MutableStateFlow("")

    private val _riskFlags = MutableStateFlow<Set<String>>(emptySet())
    val riskFlags: StateFlow<Set<String>> = _riskFlags.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<PregnancyProfileResponse>>(UiState.Empty)
    val submitState: StateFlow<UiState<PregnancyProfileResponse>> = _submitState.asStateFlow()

    // NM-B-167 is live now, so a failure here is a real failure (validation, network, or an
    // already-active profile) — no longer the "endpoint doesn't exist yet" case. Kept so the
    // UI copy that reads it still compiles; it stays false.
    private val _backendPending = MutableStateFlow(false)
    val backendPending: StateFlow<Boolean> = _backendPending.asStateFlow()

    fun setLmpDate(date: String) { _dateBasis.value = DateBasis(lmpDate = date) }
    fun setUltrasoundDate(date: String, weeks: Int?) {
        _dateBasis.value = DateBasis(ultrasoundDate = date, ultrasoundWeeks = weeks)
    }
    fun setIvfTransferDate(date: String) { _dateBasis.value = DateBasis(ivfTransferDate = date) }

    fun setAgeYears(value: Int) { _ageYears.value = value }
    fun setHeightCm(value: Double) { _heightCm.value = value }
    fun setPrePregnancyWeightKg(value: Double) { _prePregnancyWeightKg.value = value }
    fun setBloodGroup(value: String) { _bloodGroup.value = value }

    fun toggleRiskFlag(flag: String) {
        _riskFlags.value = if (flag in _riskFlags.value) _riskFlags.value - flag else _riskFlags.value + flag
    }

    fun next() { _step.value = (_step.value + 1).coerceAtMost(3) }
    fun back() { _step.value = (_step.value - 1).coerceAtLeast(1) }

    fun submit() {
        // ageYears is collected for the wizard's own display only — pregnancy_profiles has no
        // such column, so it is not sent.
        _ageYears.value ?: return
        val height = _heightCm.value ?: return
        val weight = _prePregnancyWeightKg.value ?: return
        val basis = _dateBasis.value
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            // Flatten the step-1 UI state onto the wire DTO. Exactly one dating basis is sent;
            // the server infers eddCalculationMethod from it and rejects anything ambiguous.
            val request = CreatePregnancyProfileRequest(
                lmpDate = basis.lmpDate,
                ultrasoundDate = basis.ultrasoundDate,
                ultrasoundGestationalAgeWeeks = basis.ultrasoundWeeks,
                ivfTransferDate = basis.ivfTransferDate,
                heightCm = height,
                prePregnancyWeightKg = weight,
                bloodGroup = _bloodGroup.value,
                highRiskFlags = _riskFlags.value.toList(),
            )
            repository.create(request).fold(
                onSuccess = { profile ->
                    session.activePregnancyId.value = profile.id
                    session.setOnboardingComplete(true)
                    _submitState.value = UiState.Success(profile)
                },
                onFailure = { err ->
                    // The endpoint exists now, so do NOT mark onboarding complete on failure —
                    // that would strand the user without a profile every clinical screen needs.
                    _submitState.value = UiState.Error(err)
                },
            )
        }
    }
}
