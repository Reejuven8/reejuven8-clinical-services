package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.SymptomItem
import com.reejuven8.ninemo.shared.model.SymptomLogHistoryEntry
import com.reejuven8.ninemo.shared.model.SymptomLogResponse
import com.reejuven8.ninemo.shared.model.VitalsAtLog
import com.reejuven8.ninemo.shared.repository.SymptomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Common symptoms surfaced as quick-select chips — "More…" lets free text extend this list. */
val CommonSymptoms = listOf(
    "nausea", "spotting", "cramping", "swelling_hands", "headache", "dizziness",
    "back_pain", "reduced_fetal_movement", "regular_contractions", "vaginal_discharge",
)

/** P11 Symptom Log — server owns the severity verdict; client never computes triage itself. */
class SymptomLogViewModel(private val repository: SymptomRepository) : ViewModel() {

    private val _selectedSymptoms = MutableStateFlow<Set<String>>(emptySet())
    val selectedSymptoms: StateFlow<Set<String>> = _selectedSymptoms.asStateFlow()

    private val _severity = MutableStateFlow(0.5f) // 0=mild, 0.5=moderate, 1=severe
    val severity: StateFlow<Float> = _severity.asStateFlow()

    private val _attachVitals = MutableStateFlow(false)
    val attachVitals: StateFlow<Boolean> = _attachVitals.asStateFlow()

    private val _vitalsAtLog = MutableStateFlow(VitalsAtLog())
    val vitalsAtLog: StateFlow<VitalsAtLog> = _vitalsAtLog.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<SymptomLogResponse>>(UiState.Empty)
    val submitState: StateFlow<UiState<SymptomLogResponse>> = _submitState.asStateFlow()

    private val _history = MutableStateFlow<UiState<List<SymptomLogHistoryEntry>>>(UiState.Loading)
    val history: StateFlow<UiState<List<SymptomLogHistoryEntry>>> = _history.asStateFlow()

    init {
        loadHistory()
    }

    fun toggleSymptom(name: String) {
        _selectedSymptoms.value = if (name in _selectedSymptoms.value) {
            _selectedSymptoms.value - name
        } else {
            _selectedSymptoms.value + name
        }
    }

    fun setSeverity(value: Float) { _severity.value = value }
    fun setAttachVitals(value: Boolean) { _attachVitals.value = value }
    fun setVitalsAtLog(value: VitalsAtLog) { _vitalsAtLog.value = value }

    fun submit() {
        if (_selectedSymptoms.value.isEmpty()) return
        val severityLabel = when {
            _severity.value < 0.34f -> "mild"
            _severity.value < 0.67f -> "moderate"
            else -> "severe"
        }
        val symptoms = _selectedSymptoms.value.map { SymptomItem(name = it, severity = severityLabel) }
        val vitals = if (_attachVitals.value) _vitalsAtLog.value else null

        viewModelScope.launch {
            _submitState.value = UiState.Loading
            repository.log(symptoms, vitals).fold(
                onSuccess = {
                    _submitState.value = UiState.Success(it)
                    loadHistory()
                },
                onFailure = { _submitState.value = UiState.Error(it) },
            )
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            repository.history().fold(
                onSuccess = { _history.value = if (it.isEmpty()) UiState.Empty else UiState.Success(it) },
                onFailure = { _history.value = UiState.Error(it) },
            )
        }
    }
}
