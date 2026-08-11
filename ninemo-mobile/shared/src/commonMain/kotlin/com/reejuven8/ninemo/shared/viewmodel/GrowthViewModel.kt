package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.GrowthInputRequest
import com.reejuven8.ninemo.shared.model.GrowthMeasurementResponse
import com.reejuven8.ninemo.shared.repository.GrowthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class GrowthMetric(val label: String, val zScoreKey: String) {
    WEIGHT("Weight", "weight_for_age"),
    HEIGHT("Height", "height_for_age"),
    HEAD("Head", "head_circumference_for_age"),
}

/** P17 — Growth Chart. All Z-scores/percentiles/alerts server-computed (WHO LMS); client renders only. */
class GrowthViewModel(private val repository: GrowthRepository) : ViewModel() {

    private var childId: String? = null

    private val _activeMetric = MutableStateFlow(GrowthMetric.WEIGHT)
    val activeMetric: StateFlow<GrowthMetric> = _activeMetric.asStateFlow()

    private val _history = MutableStateFlow<UiState<List<GrowthMeasurementResponse>>>(UiState.Loading)
    val history: StateFlow<UiState<List<GrowthMeasurementResponse>>> = _history.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<GrowthMeasurementResponse>>(UiState.Empty)
    val submitState: StateFlow<UiState<GrowthMeasurementResponse>> = _submitState.asStateFlow()

    fun load(childId: String) {
        this.childId = childId
        loadHistory()
    }

    fun setMetric(metric: GrowthMetric) { _activeMetric.value = metric }

    private fun loadHistory() {
        val id = childId ?: return
        viewModelScope.launch {
            _history.value = UiState.Loading
            repository.history(id).fold(
                onSuccess = { _history.value = if (it.isEmpty()) UiState.Empty else UiState.Success(it) },
                onFailure = { _history.value = UiState.Error(it) },
            )
        }
    }

    fun add(ageInMonths: Int, heightCm: Double, weightKg: Double, headCircumferenceCm: Double?) {
        val id = childId ?: return
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            repository.add(id, GrowthInputRequest(ageInMonths, heightCm, weightKg, headCircumferenceCm)).fold(
                onSuccess = {
                    _submitState.value = UiState.Success(it)
                    loadHistory()
                },
                onFailure = { _submitState.value = UiState.Error(it) },
            )
        }
    }
}
