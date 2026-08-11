package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.DevelopmentalMilestoneResponse
import com.reejuven8.ninemo.shared.repository.MilestoneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// WHO check-in months the backend has milestone data for (DevelopmentalMilestoneService).
val MilestoneMonths = listOf(2, 4, 6, 9, 12, 18, 24, 36, 48, 60)

/** P19 — Milestone Checklist. Server owns delay-risk assessment (via alertFlags); client renders. */
class MilestoneViewModel(private val repository: MilestoneRepository) : ViewModel() {

    private var childId: String? = null

    private val _selectedMonth = MutableStateFlow(2)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _checkIn = MutableStateFlow<UiState<DevelopmentalMilestoneResponse>>(UiState.Loading)
    val checkIn: StateFlow<UiState<DevelopmentalMilestoneResponse>> = _checkIn.asStateFlow()

    fun load(childId: String) {
        this.childId = childId
        loadMonth(_selectedMonth.value)
    }

    fun selectMonth(month: Int) {
        _selectedMonth.value = month
        loadMonth(month)
    }

    private fun loadMonth(month: Int) {
        val id = childId ?: return
        viewModelScope.launch {
            _checkIn.value = UiState.Loading
            repository.forMonth(id, month).fold(
                onSuccess = { _checkIn.value = UiState.Success(it) },
                onFailure = { _checkIn.value = UiState.Error(it) },
            )
        }
    }

    fun toggle(milestoneName: String, achieved: Boolean) {
        val current = (_checkIn.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.achieve(current.id, milestoneName, achieved).onSuccess {
                _checkIn.value = UiState.Success(it)
            }
        }
    }
}
