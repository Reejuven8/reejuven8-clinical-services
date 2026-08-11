package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.VitalType
import com.reejuven8.ninemo.shared.model.VitalsLogResponse
import com.reejuven8.ninemo.shared.model.VitalsMeasurements
import com.reejuven8.ninemo.shared.repository.VitalsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** P12 Vitals — Weight + Blood Pressure tabs. alertTriggered/isWithinNormalRange are server-computed. */
class VitalsViewModel(private val repository: VitalsRepository) : ViewModel() {

    private val _activeTab = MutableStateFlow(VitalType.WEIGHT)
    val activeTab: StateFlow<VitalType> = _activeTab.asStateFlow()

    private val _weightHistory = MutableStateFlow<UiState<List<VitalsLogResponse>>>(UiState.Loading)
    val weightHistory: StateFlow<UiState<List<VitalsLogResponse>>> = _weightHistory.asStateFlow()

    private val _bpHistory = MutableStateFlow<UiState<List<VitalsLogResponse>>>(UiState.Loading)
    val bpHistory: StateFlow<UiState<List<VitalsLogResponse>>> = _bpHistory.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<VitalsLogResponse>>(UiState.Empty)
    val submitState: StateFlow<UiState<VitalsLogResponse>> = _submitState.asStateFlow()

    init {
        loadHistory(VitalType.WEIGHT)
        loadHistory(VitalType.BLOOD_PRESSURE)
    }

    fun setActiveTab(tab: VitalType) {
        _activeTab.value = tab
    }

    fun logWeight(weightKg: Double) = submit(VitalType.WEIGHT, VitalsMeasurements(weightKg = weightKg))

    fun logBloodPressure(systolic: Int, diastolic: Int) =
        submit(VitalType.BLOOD_PRESSURE, VitalsMeasurements(bloodPressureSystolic = systolic, bloodPressureDiastolic = diastolic))

    private fun submit(type: VitalType, measurements: VitalsMeasurements) {
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            repository.log(type, measurements).fold(
                onSuccess = {
                    _submitState.value = UiState.Success(it)
                    loadHistory(type)
                },
                onFailure = { _submitState.value = UiState.Error(it) },
            )
        }
    }

    private fun loadHistory(type: VitalType) {
        viewModelScope.launch {
            repository.history(type).fold(
                onSuccess = { entries ->
                    val state: UiState<List<VitalsLogResponse>> = if (entries.isEmpty()) UiState.Empty else UiState.Success(entries)
                    if (type == VitalType.WEIGHT) _weightHistory.value = state else _bpHistory.value = state
                },
                onFailure = { err ->
                    if (type == VitalType.WEIGHT) _weightHistory.value = UiState.Error(err) else _bpHistory.value = UiState.Error(err)
                },
            )
        }
    }
}
