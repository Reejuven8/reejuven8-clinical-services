package com.reejuven8.ninemo.android.ui.screens.locker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.ErrorView
import com.reejuven8.ninemo.android.ui.components.LoadingSpinner
import com.reejuven8.ninemo.android.ui.components.NineMoButton
import com.reejuven8.ninemo.shared.model.ConsentResponse
import com.reejuven8.ninemo.shared.viewmodel.ConsentViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import com.reejuven8.ninemo.shared.viewmodel.displayStatus
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.koin.compose.viewmodel.koinViewModel

private val GrantedBg = Color(0xFFD3EEDD)
private val GrantedFg = Color(0xFF173F2A)
private val ExpiredBg = Color(0xFFFFDEAD)
private val ExpiredFg = Color(0xFF5C3700)
private val RevokedBg = Color(0xFFFFDAD6)
private val RevokedFg = Color(0xFF93000A)

private val DurationOptions = listOf(30, 90, 180, 365)

/** P9 — Consent Manager. Doctors only see patient data with an active, non-expired consent. */
@OptIn(ExperimentalTime::class)
@Composable
fun ConsentManagerScreen(onBack: () -> Unit) {
    val vm: ConsentViewModel = koinViewModel()
    val consentsState by vm.consents.collectAsStateWithLifecycle()
    val grantState by vm.grantState.collectAsStateWithLifecycle()
    val nowIso = remember { Clock.System.now().toString() }

    var showGrantForm by remember { mutableStateOf(false) }

    LaunchedEffect(grantState) {
        if (grantState is UiState.Success) showGrantForm = false
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Consent manager", style = MaterialTheme.typography.headlineSmall)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                .padding(14.dp),
        ) {
            Text(
                "Only doctors you approve can see your records. You can revoke access anytime — it takes effect immediately.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Text(
            "Who has access",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
        )

        when (val state = consentsState) {
            is UiState.Loading -> LoadingSpinner()
            is UiState.Error -> ErrorView(state.throwable, onRetry = vm::load)
            is UiState.Empty -> Text("No one has access yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            is UiState.Success -> state.data.forEach { consent ->
                ConsentCard(consent, nowIso, onRevoke = { vm.revoke(consent.id) })
            }
        }

        if (showGrantForm) {
            GrantForm(
                grantState = grantState,
                onSubmit = { doctorId, days -> vm.grant(doctorId, days) },
                onCancel = { showGrantForm = false; vm.resetGrantState() },
            )
        } else {
            NineMoButton(
                text = "Grant access to a doctor",
                onClick = { showGrantForm = true },
                modifier = Modifier.padding(top = 18.dp),
            )
            Text(
                "You'll need the doctor's NineMo ID and choose a duration.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun ConsentCard(consent: ConsentResponse, nowIso: String, onRevoke: () -> Unit) {
    val status = displayStatus(consent, nowIso)
    val (bg, fg) = when (status) {
        "GRANTED" -> GrantedBg to GrantedFg
        "EXPIRED" -> ExpiredBg to ExpiredFg
        else -> RevokedBg to RevokedFg
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(16.dp, 18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Doctor ${consent.doctorId.take(8)}…", style = MaterialTheme.typography.bodyLarge)
            Text(
                status,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                modifier = Modifier.background(bg, RoundedCornerShape(100.dp)).padding(10.dp, 4.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Granted ${consent.grantedAt.take(10)} · expires ${consent.expiresAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (status == "GRANTED") {
                Text(
                    "Revoke",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable(onClick = onRevoke),
                )
            }
        }
    }
}

@Composable
private fun GrantForm(
    grantState: UiState<ConsentResponse>,
    onSubmit: (String, Int) -> Unit,
    onCancel: () -> Unit,
) {
    var doctorId by remember { mutableStateOf("") }
    var duration by remember { mutableIntStateOf(90) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        OutlinedTextField(
            value = doctorId,
            onValueChange = { doctorId = it },
            label = { Text("Doctor's NineMo ID") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DurationOptions.forEach { days ->
                FilterChip(selected = duration == days, onClick = { duration = days }, label = { Text("${days}d") })
            }
        }
        if (grantState is UiState.Error) {
            Text(
                grantState.throwable.message ?: "Couldn't grant access.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NineMoButton(
                text = "Grant",
                onClick = { onSubmit(doctorId, duration) },
                loading = grantState is UiState.Loading,
                enabled = doctorId.isNotBlank(),
                modifier = Modifier.weight(1f),
            )
            NineMoButton(text = "Cancel", onClick = onCancel, modifier = Modifier.weight(1f))
        }
    }
}
