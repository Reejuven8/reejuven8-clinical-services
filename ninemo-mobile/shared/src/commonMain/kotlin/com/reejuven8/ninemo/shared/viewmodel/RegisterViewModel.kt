package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.BiologicalSex
import com.reejuven8.ninemo.shared.model.RegisterRequest
import com.reejuven8.ninemo.shared.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * P2 Register. Backend RegisterRequest needs firstName/lastName/dateOfBirth/biologicalSex —
 * fields the design mockup (option 3a) doesn't show (mockup only has a name + phone field).
 * fullName is kept as one input to match the mockup and split on first space; DOB + sex are
 * added as required extra fields since the server rejects the request without them.
 */
class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _dateOfBirth = MutableStateFlow("") // yyyy-MM-dd
    val dateOfBirth: StateFlow<String> = _dateOfBirth.asStateFlow()

    private val _biologicalSex = MutableStateFlow<BiologicalSex?>(null)
    val biologicalSex: StateFlow<BiologicalSex?> = _biologicalSex.asStateFlow()

    private val _termsAccepted = MutableStateFlow(false)
    val termsAccepted: StateFlow<Boolean> = _termsAccepted.asStateFlow()

    private val _registerState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val registerState: StateFlow<UiState<Unit>> = _registerState.asStateFlow()

    fun setFullName(value: String) { _fullName.value = value }
    fun setPhoneNumber(value: String) { _phoneNumber.value = value }
    fun setDateOfBirth(value: String) { _dateOfBirth.value = value }
    fun setBiologicalSex(value: BiologicalSex) { _biologicalSex.value = value }
    fun setTermsAccepted(value: Boolean) { _termsAccepted.value = value }

    val canSubmit: Boolean
        get() = _fullName.value.trim().contains(' ') &&
            _phoneNumber.value.isNotBlank() &&
            _dateOfBirth.value.isNotBlank() &&
            _biologicalSex.value != null &&
            _termsAccepted.value

    fun register() {
        val sex = _biologicalSex.value ?: return
        val nameParts = _fullName.value.trim().split(Regex("\\s+"), limit = 2)
        if (nameParts.size < 2 || !canSubmit) {
            _registerState.value = UiState.Error(IllegalStateException("Enter your full name (first and last)"))
            return
        }
        viewModelScope.launch {
            _registerState.value = UiState.Loading
            val request = RegisterRequest(
                firstName = nameParts[0],
                lastName = nameParts[1],
                phoneNumber = _phoneNumber.value,
                dateOfBirth = _dateOfBirth.value,
                biologicalSex = sex,
            )
            authRepository.register(request).fold(
                onSuccess = { _registerState.value = UiState.Success(Unit) },
                onFailure = { _registerState.value = UiState.Error(it) },
            )
        }
    }
}
