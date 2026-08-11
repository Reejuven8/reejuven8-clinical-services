package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.SeverityFlag
import com.reejuven8.ninemo.shared.model.SymptomLogHistoryEntry
import com.reejuven8.ninemo.shared.model.TimelineResponse
import com.reejuven8.ninemo.shared.repository.SymptomRepository
import com.reejuven8.ninemo.shared.repository.TimelineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** P5 Home/Timeline. Combines Timeline content with the most recent non-NORMAL symptom log for the banner. */
class TimelineViewModel(
    private val timelineRepository: TimelineRepository,
    private val symptomRepository: SymptomRepository,
) : ViewModel() {

    private val _timelineState = MutableStateFlow<UiState<TimelineResponse>>(UiState.Loading)
    val timelineState: StateFlow<UiState<TimelineResponse>> = _timelineState.asStateFlow()

    private val _banner = MutableStateFlow<SymptomLogHistoryEntry?>(null)
    val banner: StateFlow<SymptomLogHistoryEntry?> = _banner.asStateFlow()

    init {
        loadCurrentWeek()
        loadBanner()
    }

    fun loadCurrentWeek() {
        viewModelScope.launch {
            _timelineState.value = UiState.Loading
            timelineRepository.current().fold(
                onSuccess = { _timelineState.value = UiState.Success(it) },
                onFailure = { _timelineState.value = UiState.Error(it) },
            )
        }
    }

    fun loadWeek(week: Int) {
        viewModelScope.launch {
            _timelineState.value = UiState.Loading
            timelineRepository.week(week).fold(
                onSuccess = { _timelineState.value = UiState.Success(it) },
                onFailure = { _timelineState.value = UiState.Error(it) },
            )
        }
    }

    private fun loadBanner() {
        viewModelScope.launch {
            symptomRepository.history().onSuccess { entries ->
                _banner.value = entries
                    .filter { it.severityFlag != SeverityFlag.NORMAL }
                    .maxByOrNull { it.loggedAt }
            }
        }
    }
}
