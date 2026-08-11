package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.GrowthMeasurementResponse
import com.reejuven8.ninemo.shared.model.VaccinationRecordResponse
import com.reejuven8.ninemo.shared.model.VaccinationStatus
import com.reejuven8.ninemo.shared.repository.GrowthRepository
import com.reejuven8.ninemo.shared.repository.MilestoneRepository
import com.reejuven8.ninemo.shared.repository.VaccinationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChildDashboard(
    val latestGrowth: GrowthMeasurementResponse?,
    val nextVaccination: VaccinationRecordResponse?,
    val anyOverdue: Boolean,
    val anyGrowthAlert: Boolean,
)

/** P6 — Child Dashboard. Aggregates the three child data sources into a single home summary. */
class ChildDashboardViewModel(
    private val growthRepository: GrowthRepository,
    private val vaccinationRepository: VaccinationRepository,
    private val milestoneRepository: MilestoneRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ChildDashboard>>(UiState.Loading)
    val state: StateFlow<UiState<ChildDashboard>> = _state.asStateFlow()

    fun load(childId: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val growth = growthRepository.history(childId).getOrDefault(emptyList())
            val schedule = vaccinationRepository.schedule(childId).getOrDefault(emptyList())

            val latestGrowth = growth.maxByOrNull { it.measurementDate }
            val nextVaccination = schedule
                .filter { it.status != VaccinationStatus.COMPLETED }
                .minByOrNull { it.scheduledDate }

            _state.value = UiState.Success(
                ChildDashboard(
                    latestGrowth = latestGrowth,
                    nextVaccination = nextVaccination,
                    anyOverdue = schedule.any { it.status == VaccinationStatus.OVERDUE },
                    anyGrowthAlert = latestGrowth?.alertFlags?.isNotEmpty() == true,
                ),
            )
        }
    }
}
