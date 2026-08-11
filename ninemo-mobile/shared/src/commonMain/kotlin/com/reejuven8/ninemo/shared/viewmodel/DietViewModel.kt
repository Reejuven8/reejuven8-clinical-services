package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.DietFoodSafetyResponse
import com.reejuven8.ninemo.shared.repository.DietRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** P15 — Diet "Is It Safe?". safetyRating always server-supplied (SAFE/CAUTION/AVOID). */
class DietViewModel(private val repository: DietRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<UiState<List<DietFoodSafetyResponse>>>(UiState.Empty)
    val results: StateFlow<UiState<List<DietFoodSafetyResponse>>> = _results.asStateFlow()

    // No recent-searches endpoint exists backend-side — kept local to this session only.
    private val _recentSearches = MutableStateFlow<List<DietFoodSafetyResponse>>(emptyList())
    val recentSearches: StateFlow<List<DietFoodSafetyResponse>> = _recentSearches.asStateFlow()

    fun setQuery(value: String) {
        _query.value = value
    }

    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _results.value = UiState.Loading
            repository.search(q).fold(
                onSuccess = { list ->
                    _results.value = if (list.isEmpty()) UiState.Empty else UiState.Success(list)
                    list.firstOrNull()?.let { top ->
                        _recentSearches.value = (listOf(top) + _recentSearches.value).distinctBy { it.ingredientName }.take(10)
                    }
                },
                onFailure = { _results.value = UiState.Error(it) },
            )
        }
    }

    fun clearQuery() {
        _query.value = ""
        _results.value = UiState.Empty
    }
}
