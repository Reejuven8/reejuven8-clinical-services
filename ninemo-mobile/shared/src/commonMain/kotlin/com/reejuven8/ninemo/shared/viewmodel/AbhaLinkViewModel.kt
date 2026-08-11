package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.repository.AbhaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * P3 ABHA Link — mobile is already verified by login, so this wizard starts at step 2
 * (Aadhaar OTP) per the design mockup (2c). Skippable at any step.
 */
class AbhaLinkViewModel(private val abhaRepository: AbhaRepository) : ViewModel() {

    private val _step = MutableStateFlow(2)
    val step: StateFlow<Int> = _step.asStateFlow()

    private val _txnId = MutableStateFlow<String?>(null)

    private val _otpState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val otpState: StateFlow<UiState<Unit>> = _otpState.asStateFlow()

    private val _verifyState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val verifyState: StateFlow<UiState<Unit>> = _verifyState.asStateFlow()

    private val _addressState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val addressState: StateFlow<UiState<Unit>> = _addressState.asStateFlow()

    fun generateOtp(aadhaarOrMobile: String) {
        viewModelScope.launch {
            _otpState.value = UiState.Loading
            abhaRepository.generateOtp(aadhaarOrMobile).fold(
                onSuccess = {
                    _txnId.value = it.txnId
                    _otpState.value = UiState.Success(Unit)
                },
                onFailure = { _otpState.value = UiState.Error(it) },
            )
        }
    }

    fun verifyOtp(otp: String) {
        val txnId = _txnId.value ?: return
        viewModelScope.launch {
            _verifyState.value = UiState.Loading
            abhaRepository.verifyOtp(txnId, otp).fold(
                onSuccess = {
                    _txnId.value = it.txnId // server issues a new txnId for the address step
                    _verifyState.value = UiState.Success(Unit)
                    _step.value = 3
                },
                onFailure = { _verifyState.value = UiState.Error(it) },
            )
        }
    }

    fun setAddress(preferredAddress: String) {
        val txnId = _txnId.value ?: return
        viewModelScope.launch {
            _addressState.value = UiState.Loading
            abhaRepository.setAddress(txnId, preferredAddress).fold(
                onSuccess = { _addressState.value = UiState.Success(Unit) },
                onFailure = { _addressState.value = UiState.Error(it) },
            )
        }
    }
}
