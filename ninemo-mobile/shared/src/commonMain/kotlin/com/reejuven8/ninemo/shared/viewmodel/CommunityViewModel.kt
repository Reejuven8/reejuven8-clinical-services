package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.ClubResponse
import com.reejuven8.ninemo.shared.repository.CommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** P20 — Due Date Clubs list + join. Chat lives in [ChatViewModel]. */
class CommunityViewModel(private val repository: CommunityRepository) : ViewModel() {

    private val _clubs = MutableStateFlow<UiState<List<ClubResponse>>>(UiState.Loading)
    val clubs: StateFlow<UiState<List<ClubResponse>>> = _clubs.asStateFlow()

    private val _joining = MutableStateFlow(false)
    val joining: StateFlow<Boolean> = _joining.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _clubs.value = UiState.Loading
            repository.listClubs().fold(
                onSuccess = { _clubs.value = if (it.isEmpty()) UiState.Empty else UiState.Success(it) },
                onFailure = { _clubs.value = UiState.Error(it) },
            )
        }
    }

    /** dueDateMonth is YYYY-MM. onJoined carries the club + the alias the user chose. */
    fun join(dueDateMonth: String, alias: String, onJoined: (ClubResponse, String) -> Unit) {
        viewModelScope.launch {
            _joining.value = true
            repository.join(dueDateMonth, alias).fold(
                onSuccess = { onJoined(it, alias); load() },
                onFailure = { _clubs.value = UiState.Error(it) },
            )
            _joining.value = false
        }
    }
}
