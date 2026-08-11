package com.reejuven8.ninemo.shared.viewmodel

/** Loading | Success | Error | Empty — every data screen renders exactly one. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val throwable: Throwable) : UiState<Nothing>
}

inline fun <T> runToUiState(block: () -> T): UiState<T> =
    runCatching(block).fold(
        onSuccess = { UiState.Success(it) },
        onFailure = { UiState.Error(it) },
    )
