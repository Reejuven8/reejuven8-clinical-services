package com.reejuven8.ninemo.android.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.NineMoButton
import com.reejuven8.ninemo.android.ui.theme.Berry
import com.reejuven8.ninemo.shared.viewmodel.AbhaLinkViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import org.koin.compose.viewmodel.koinViewModel

/** P3 — connects the government health ID. Skippable; QR scan is a Phase-2 backend stub. */
@Composable
fun AbhaLinkScreen(maskedPhone: String, onDone: () -> Unit, onSkip: () -> Unit) {
    val vm: AbhaLinkViewModel = koinViewModel()
    val step by vm.step.collectAsStateWithLifecycle()
    val otpState by vm.otpState.collectAsStateWithLifecycle()
    val verifyState by vm.verifyState.collectAsStateWithLifecycle()
    val addressState by vm.addressState.collectAsStateWithLifecycle()

    var aadhaarOrMobile by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    LaunchedEffect(addressState) {
        if (addressState is UiState.Success) onDone()
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSkip) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            TextButton(onClick = onSkip) { Text("Skip for now") }
        }

        Box(
            Modifier.padding(top = 8.dp).size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Berry)
        }
        Text("Link your ABHA", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
        Text(
            "Connect your government health ID so lab reports and scans sync into your Locker automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            StepRow(number = 1, done = true, title = "Mobile number verified", subtitle = maskedPhone)

            StepRow(number = 2, done = step > 2, active = step == 2, title = "Enter Aadhaar OTP", subtitle = "Sent to your Aadhaar-linked mobile") {
                if (step == 2) {
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = otp,
                            onValueChange = { otp = it },
                            label = { Text("OTP") },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                        )
                        NineMoButton(
                            text = if (aadhaarOrMobile.isBlank() || otpState is UiState.Success) "Verify" else "Generate",
                            onClick = {
                                if (otpState is UiState.Success) vm.verifyOtp(otp) else vm.generateOtp(aadhaarOrMobile)
                            },
                            loading = otpState is UiState.Loading || verifyState is UiState.Loading,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (otpState !is UiState.Success) {
                        OutlinedTextField(
                            value = aadhaarOrMobile,
                            onValueChange = { aadhaarOrMobile = it },
                            label = { Text("Aadhaar-linked mobile") },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            shape = MaterialTheme.shapes.medium,
                        )
                    }
                    if (verifyState is UiState.Error) {
                        Text(
                            (verifyState as UiState.Error).throwable.message ?: "Verification failed",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            StepRow(number = 3, done = false, active = step == 3, title = "Choose your ABHA address", subtitle = "e.g. name@abdm") {
                if (step == 3) {
                    Column(Modifier.padding(top = 10.dp)) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("ABHA address") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        )
                        if (addressState is UiState.Error) {
                            Text(
                                (addressState as UiState.Error).throwable.message ?: "Couldn't save your ABHA address",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        NineMoButton(
                            text = "Finish linking",
                            onClick = { vm.setAddress(address) },
                            loading = addressState is UiState.Loading,
                            enabled = address.isNotBlank(),
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Scan ABHA QR instead", style = MaterialTheme.typography.bodyMedium)
                Text("Coming soon", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StepRow(
    number: Int,
    done: Boolean,
    active: Boolean = false,
    title: String,
    subtitle: String,
    content: (@Composable () -> Unit)? = null,
) {
    Row {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(
                        when {
                            done -> Color(0xFF2E6B4F)
                            active -> Berry
                            else -> Color.Transparent
                        },
                        RoundedCornerShape(50),
                    )
                    .then(if (!done && !active) Modifier.border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50)) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    done -> Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    else -> Text(
                        "$number",
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (done || active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content?.invoke()
        }
    }
}
