package com.reejuven8.ninemo.android.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.NineMoButton
import com.reejuven8.ninemo.shared.viewmodel.AuthViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import org.koin.compose.viewmodel.koinViewModel

/** P1 — phone entry -> request OTP -> 6-digit verify. Never displays or logs the OTP elsewhere. */
@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onGoToRegister: () -> Unit) {
    val vm: AuthViewModel = koinViewModel()
    val phoneNumber by vm.phoneNumber.collectAsStateWithLifecycle()
    val otpSendState by vm.otpSendState.collectAsStateWithLifecycle()
    val loginState by vm.loginState.collectAsStateWithLifecycle()
    val resendCooldown by vm.resendCooldown.collectAsStateWithLifecycle()

    LaunchedEffect(loginState) {
        if (loginState is UiState.Success) onLoggedIn()
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        IconButton(onClick = { /* handled by system back on this graph's start */ }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        if (otpSendState !is UiState.Success) {
            Text("Sign in", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
            Text(
                "Enter your registered mobile number and we'll text you a 6-digit code.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = vm::setPhoneNumber,
                label = { Text("Mobile number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.weight(1f))
            if (otpSendState is UiState.Error) {
                Text(
                    (otpSendState as UiState.Error).throwable.message ?: "Couldn't send the code.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            NineMoButton(
                text = "Send code",
                onClick = vm::sendOtp,
                loading = otpSendState is UiState.Loading,
                enabled = phoneNumber.isNotBlank(),
            )
            TextButton(onClick = onGoToRegister, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text("New here? Create an account")
            }
        } else {
            OtpEntryStep(
                phoneNumber = phoneNumber,
                resendCooldown = resendCooldown,
                loginState = loginState,
                onVerify = vm::login,
                onResend = vm::sendOtp,
            )
        }
    }
}

@Composable
private fun ColumnScope.OtpEntryStep(
    phoneNumber: String,
    resendCooldown: Int,
    loginState: UiState<Unit>,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
) {
    var otp by remember { mutableStateOf("") }

    Text("Enter the code", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
    Text(
        "We sent a 6-digit code by SMS to $phoneNumber",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
    OutlinedTextField(
        value = otp,
        onValueChange = { if (it.length <= 6) otp = it.filter(Char::isDigit) },
        label = { Text("6-digit code") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
        shape = MaterialTheme.shapes.medium,
    )
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            if (resendCooldown > 0) "Resend code in 0:${resendCooldown.toString().padStart(2, '0')}" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onResend, enabled = resendCooldown == 0) { Text("Resend") }
    }
    Spacer(Modifier.weight(1f))
    if (loginState is UiState.Error) {
        Text(
            loginState.throwable.message ?: "Couldn't verify that code.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
    NineMoButton(
        text = "Verify & sign in",
        onClick = { onVerify(otp) },
        loading = loginState is UiState.Loading,
        enabled = otp.length == 6,
    )
    Text(
        "The code is verified on our servers and never stored on this phone.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )
}
