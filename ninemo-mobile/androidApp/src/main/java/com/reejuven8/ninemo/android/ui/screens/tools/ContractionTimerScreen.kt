package com.reejuven8.ninemo.android.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.NineMoButton
import com.reejuven8.ninemo.android.ui.theme.Berry
import com.reejuven8.ninemo.shared.model.ContractionSessionResponse
import com.reejuven8.ninemo.shared.viewmodel.ContractionViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import kotlinx.serialization.json.JsonPrimitive
import org.koin.compose.viewmodel.koinViewModel

private val IntensityOptions = listOf("mild", "moderate", "strong")

/** P14 — Contraction Timer. isLaborPattern/alertTriggered (labor + premature) are server-computed. */
@Composable
fun ContractionTimerScreen(onBack: () -> Unit) {
    val vm: ContractionViewModel = koinViewModel()
    val sessionState by vm.session.collectAsStateWithLifecycle()
    val timerRunning by vm.timerRunning.collectAsStateWithLifecycle()
    val elapsedSeconds by vm.elapsedSeconds.collectAsStateWithLifecycle()
    val selectedIntensity by vm.selectedIntensity.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        Text("Contraction timer", style = MaterialTheme.typography.headlineSmall)

        when (val state = sessionState) {
            is UiState.Empty -> Column(Modifier.padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Time each contraction — your care engine watches the pattern for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NineMoButton(text = "Start session", onClick = vm::startSession, modifier = Modifier.padding(top = 20.dp))
            }
            is UiState.Loading -> {}
            is UiState.Error -> Text(
                state.throwable.message ?: "Couldn't start a session — check you have an active pregnancy profile.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 20.dp),
            )
            is UiState.Success -> RunningSession(
                session = state.data,
                timerRunning = timerRunning,
                elapsedSeconds = elapsedSeconds,
                selectedIntensity = selectedIntensity,
                onSetIntensity = vm::setIntensity,
                onStart = vm::startContraction,
                onStop = vm::stopContraction,
                onEnd = vm::endSession,
            )
        }
    }
}

@Composable
private fun RunningSession(
    session: ContractionSessionResponse,
    timerRunning: Boolean,
    elapsedSeconds: Int,
    selectedIntensity: String?,
    onSetIntensity: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onEnd: () -> Unit,
) {
    if (session.isLaborPattern) {
        val alertLabel = if (session.alertTriggered) "PREMATURE LABOR PATTERN — before week 37" else "LABOR PATTERN DETECTED"
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
                .border(1.5.dp, Berry, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Text(alertLabel, style = MaterialTheme.typography.labelLarge, color = Berry)
            Text(
                "Contractions are ~5 min apart lasting ~1 min. Time to call your hospital.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }

    Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier
                .size(150.dp)
                .border(6.dp, Berry, CircleShape)
                .clickable(onClick = if (timerRunning) onStop else onStart),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("contraction", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "%d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
                fontFamily = FontFamily.Serif,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (timerRunning) "TAP TO STOP" else "TAP TO START",
                style = MaterialTheme.typography.labelMedium,
                color = Berry,
            )
        }

        if (timerRunning) {
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntensityOptions.forEach { option ->
                    FilterChip(
                        selected = selectedIntensity == option,
                        onClick = { onSetIntensity(option) },
                        label = { Text(option.replaceFirstChar(Char::uppercase)) },
                    )
                }
            }
        } else {
            Text(
                "End session",
                style = MaterialTheme.typography.labelLarge,
                color = Berry,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .border(1.5.dp, Berry, RoundedCornerShape(100.dp))
                    .clickable(onClick = onEnd)
                    .padding(horizontal = 24.dp, vertical = 9.dp),
            )
        }
    }

    Text(
        "This session",
        style = MaterialTheme.typography.labelLarge,
        color = Berry,
        modifier = Modifier.padding(top = 26.dp, bottom = 10.dp),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp)),
    ) {
        session.contractions.reversed().forEach { entry ->
            val duration = (entry["durationSeconds"] as? JsonPrimitive)?.content
            val intensity = (entry["intensity"] as? JsonPrimitive)?.content
            val interval = (entry["intervalFromPreviousSeconds"] as? JsonPrimitive)?.content
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${duration ?: "—"} s", style = MaterialTheme.typography.bodyMedium)
                Text(intensity?.replaceFirstChar(Char::uppercase) ?: "—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(interval?.let { "${it}s since last" } ?: "first", style = MaterialTheme.typography.bodySmall, color = Berry)
            }
        }
    }

    Text(
        "Before week 37, a regular pattern shows a premature-labor alert instead.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 14.dp),
    )
}
