package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.HealthRecordResponse
import com.reejuven8.ninemo.shared.repository.FilesRepository
import com.reejuven8.ninemo.shared.repository.HealthRecordsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * P7 — Health Locker (Records + Upload tabs; Consents tab is [ConsentViewModel]).
 * No category-filter query param exists server-side — filter chips work over the
 * currently-loaded page's `category` values client-side (rendering, not new logic).
 */
class HealthLockerViewModel(
    private val recordsRepository: HealthRecordsRepository,
    private val filesRepository: FilesRepository,
) : ViewModel() {

    private val _records = MutableStateFlow<UiState<List<HealthRecordResponse>>>(UiState.Loading)
    val records: StateFlow<UiState<List<HealthRecordResponse>>> = _records.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null) // null = "All"
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _uploadState = MutableStateFlow<UiState<String>>(UiState.Empty) // holds latest parse status string
    val uploadState: StateFlow<UiState<String>> = _uploadState.asStateFlow()

    init {
        loadRecords()
    }

    fun loadRecords() {
        viewModelScope.launch {
            _records.value = UiState.Loading
            recordsRepository.list().fold(
                onSuccess = { page ->
                    _records.value = if (page.content.isEmpty()) UiState.Empty else UiState.Success(page.content)
                },
                onFailure = { _records.value = UiState.Error(it) },
            )
        }
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun categoriesFrom(records: List<HealthRecordResponse>): List<String> =
        records.mapNotNull { it.category }.distinct().sorted()

    fun upload(fileName: String, contentType: String, bytes: ByteArray) {
        viewModelScope.launch {
            _uploadState.value = UiState.Loading
            filesRepository.upload(fileName, contentType, bytes).fold(
                onSuccess = { response ->
                    _uploadState.value = UiState.Success(response.status)
                    filesRepository.parseStatusEvents(response.s3Key).collect { status ->
                        _uploadState.value = UiState.Success(status)
                        if (status == "PARSED") loadRecords()
                    }
                },
                onFailure = { _uploadState.value = UiState.Error(it) },
            )
        }
    }

    fun dismissUpload() {
        _uploadState.value = UiState.Empty
    }
}
