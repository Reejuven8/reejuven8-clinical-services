package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.VaccinationRecordResponse
import com.reejuven8.ninemo.shared.model.VaccinationStatus
import com.reejuven8.ninemo.shared.repository.VaccinationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** P18 — Vaccination Tracker. Schedule (IAP) and OVERDUE status are server-computed. */
class VaccinationViewModel(private val repository: VaccinationRepository) : ViewModel() {

    private var childId: String? = null

    private val _schedule = MutableStateFlow<UiState<List<VaccinationRecordResponse>>>(UiState.Loading)
    val schedule: StateFlow<UiState<List<VaccinationRecordResponse>>> = _schedule.asStateFlow()

    fun load(childId: String) {
        this.childId = childId
        loadSchedule()
    }

    private fun loadSchedule() {
        val id = childId ?: return
        viewModelScope.launch {
            _schedule.value = UiState.Loading
            repository.schedule(id).fold(
                onSuccess = { _schedule.value = if (it.isEmpty()) UiState.Empty else UiState.Success(it) },
                onFailure = { _schedule.value = UiState.Error(it) },
            )
        }
    }

    fun markCompleted(vaccinationId: String) {
        viewModelScope.launch {
            repository.markCompleted(vaccinationId).onSuccess { loadSchedule() }
        }
    }

    // Client-side split for the Upcoming/Completed tabs — the server returns one flat schedule.
    fun upcoming(records: List<VaccinationRecordResponse>): List<VaccinationRecordResponse> =
        records.filter { it.status != VaccinationStatus.COMPLETED }

    fun completed(records: List<VaccinationRecordResponse>): List<VaccinationRecordResponse> =
        records.filter { it.status == VaccinationStatus.COMPLETED }
}
