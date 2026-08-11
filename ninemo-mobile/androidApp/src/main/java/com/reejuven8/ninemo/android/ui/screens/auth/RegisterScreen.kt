package com.reejuven8.ninemo.android.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.NineMoButton
import com.reejuven8.ninemo.shared.model.BiologicalSex
import com.reejuven8.ninemo.shared.viewmodel.RegisterViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import org.koin.compose.viewmodel.koinViewModel

/** P2 — first-time account creation. Role is always PATIENT from this app. */
@Composable
fun RegisterScreen(onRegistered: () -> Unit, onGoToLogin: () -> Unit) {
    val vm: RegisterViewModel = koinViewModel()
    val fullName by vm.fullName.collectAsStateWithLifecycle()
    val phoneNumber by vm.phoneNumber.collectAsStateWithLifecycle()
    val dateOfBirth by vm.dateOfBirth.collectAsStateWithLifecycle()
    val biologicalSex by vm.biologicalSex.collectAsStateWithLifecycle()
    val termsAccepted by vm.termsAccepted.collectAsStateWithLifecycle()
    val registerState by vm.registerState.collectAsStateWithLifecycle()

    LaunchedEffect(registerState) {
        if (registerState is UiState.Success) onRegistered()
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        IconButton(onClick = onGoToLogin) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text("Create your account", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
        Text(
            "NineMo is for you — your account is always a patient account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        OutlinedTextField(
            value = fullName,
            onValueChange = vm::setFullName,
            label = { Text("Your name") },
            modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
            shape = MaterialTheme.shapes.medium,
            supportingText = { Text("First and last name") },
        )
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = vm::setPhoneNumber,
            label = { Text("Mobile number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            shape = MaterialTheme.shapes.medium,
        )
        OutlinedTextField(
            value = dateOfBirth,
            onValueChange = vm::setDateOfBirth,
            label = { Text("Date of birth") },
            placeholder = { Text("YYYY-MM-DD") },
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            shape = MaterialTheme.shapes.medium,
        )

        Text(
            "Biological sex",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BiologicalSex.entries.forEach { sex ->
                FilterChip(
                    selected = biologicalSex == sex,
                    onClick = { vm.setBiologicalSex(sex) },
                    label = { Text(sex.name.lowercase().replaceFirstChar(Char::uppercase)) },
                )
            }
        }

        Row(Modifier.padding(top = 18.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = termsAccepted, onCheckedChange = vm::setTermsAccepted)
            Text(
                "I agree to the terms and privacy policy — my health data stays private and is shared only with my consent.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.weight(1f))
        if (registerState is UiState.Error) {
            Text(
                (registerState as UiState.Error).throwable.message ?: "Couldn't create your account.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        NineMoButton(
            text = "Create account",
            onClick = vm::register,
            loading = registerState is UiState.Loading,
            enabled = vm.canSubmit,
        )
        TextButton(onClick = onGoToLogin, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text("Already have an account? Sign in")
        }
    }
}
