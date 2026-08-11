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

/** P8 — Document Detail. Presigned URL is always fetched fresh (15-min server TTL, never cached). */
class DocumentDetailViewModel(
    private val recordsRepository: HealthRecordsRepository,
    private val filesRepository: FilesRepository,
) : ViewModel() {

    private val _record = MutableStateFlow<UiState<HealthRecordResponse>>(UiState.Loading)
    val record: StateFlow<UiState<HealthRecordResponse>> = _record.asStateFlow()

    private val _presignedUrl = MutableStateFlow<String?>(null)
    val presignedUrl: StateFlow<String?> = _presignedUrl.asStateFlow()

    fun load(recordId: String) {
        viewModelScope.launch {
            _record.value = UiState.Loading
            recordsRepository.get(recordId).fold(
                onSuccess = { record ->
                    _record.value = UiState.Success(record)
                    record.sourceFileS3Url?.let { s3Key ->
                        filesRepository.downloadUrl(s3Key).onSuccess { _presignedUrl.value = it }
                    }
                },
                onFailure = { _record.value = UiState.Error(it) },
            )
        }
    }
}
