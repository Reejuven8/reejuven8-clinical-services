package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Backend enforces no minimum resend interval (flat 5-min OTP TTL, no cooldown field) —
// this is a client-side UX guard only, not a server contract.
private const val RESEND_COOLDOWN_SECONDS = 30

/** P1 Login: phone entry -> OTP send -> 6-digit verify. */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _otpSendState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val otpSendState: StateFlow<UiState<Unit>> = _otpSendState.asStateFlow()

    private val _loginState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val loginState: StateFlow<UiState<Unit>> = _loginState.asStateFlow()

    private val _resendCooldown = MutableStateFlow(0)
    val resendCooldown: StateFlow<Int> = _resendCooldown.asStateFlow()

    private var cooldownJob: Job? = null

    fun setPhoneNumber(value: String) {
        _phoneNumber.value = value
    }

    fun sendOtp() {
        val phone = _phoneNumber.value
        if (phone.isBlank()) return
        viewModelScope.launch {
            _otpSendState.value = UiState.Loading
            authRepository.sendOtp(phone).fold(
                onSuccess = {
                    _otpSendState.value = UiState.Success(Unit)
                    startCooldown()
                },
                onFailure = { _otpSendState.value = UiState.Error(it) },
            )
        }
    }

    fun login(otp: String) {
        if (otp.isBlank()) return
        viewModelScope.launch {
            _loginState.value = UiState.Loading
            authRepository.login(_phoneNumber.value, otp).fold(
                onSuccess = { _loginState.value = UiState.Success(Unit) },
                onFailure = { _loginState.value = UiState.Error(it) },
            )
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (remaining in RESEND_COOLDOWN_SECONDS downTo 0) {
                _resendCooldown.value = remaining
                delay(1_000)
            }
        }
    }
}
