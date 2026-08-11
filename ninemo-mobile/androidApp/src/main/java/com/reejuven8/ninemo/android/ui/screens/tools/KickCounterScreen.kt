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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.NineMoButton
import com.reejuven8.ninemo.android.ui.theme.Berry
import com.reejuven8.ninemo.android.ui.theme.SeverityWarning
import com.reejuven8.ninemo.android.ui.theme.SeverityWarningBg
import com.reejuven8.ninemo.shared.model.KickCounterSessionResponse
import com.reejuven8.ninemo.shared.viewmodel.KickCounterViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import org.koin.compose.viewmodel.koinViewModel

/** P13 — Kick Counter. UI-only stopwatch feel; isConcerning is entirely server-computed. */
@Composable
fun KickCounterScreen(onBack: () -> Unit) {
    val vm: KickCounterViewModel = koinViewModel()
    val sessionState by vm.session.collectAsStateWithLifecycle()
    val tapCount by vm.kickTapCount.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        Text("Kick counter", style = MaterialTheme.typography.headlineSmall)

        when (val state = sessionState) {
            is UiState.Empty -> Column(Modifier.padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Track how long it takes to feel 10 kicks — a good time is under 2 hours.",
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
            is UiState.Success -> RunningSession(state.data, tapCount, onTap = vm::tapKick, onEnd = vm::endSession)
        }
    }
}

@Composable
private fun RunningSession(session: KickCounterSessionResponse, tapCount: Int, onTap: () -> Unit, onEnd: () -> Unit) {
    val running = session.isActive

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (running) {
            Text(
                "SESSION RUNNING",
                style = MaterialTheme.typography.labelMedium,
                color = Berry,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(100.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
            )
        }

        Column(
            Modifier
                .padding(top = 28.dp)
                .size(230.dp)
                .background(Brush.radialGradient(listOf(Color(0xFFB85D92), Berry)), CircleShape)
                .then(if (running) Modifier.clickable(onClick = onTap) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                if (running) "Tap on every kick" else "Session ended",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
            Text(
                "${session.totalKicks}",
                fontFamily = FontFamily.Serif,
                fontSize = 64.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text("of 10 kicks", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
        }

        Row(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(10) { i ->
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .size(width = 16.dp, height = 8.dp)
                        .background(
                            if (i < session.totalKicks) Berry else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp),
                        ),
                )
            }
        }

        if (running) {
            Text(
                "End session early",
                style = MaterialTheme.typography.labelLarge,
                color = Berry,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .border(1.5.dp, Berry, RoundedCornerShape(100.dp))
                    .clickable(onClick = onEnd)
                    .padding(horizontal = 24.dp, vertical = 9.dp),
            )
        } else if (session.isConcerning) {
            Text(
                "Flagged slow by your care engine — worth a check with your doctor.",
                style = MaterialTheme.typography.bodySmall,
                color = SeverityWarning,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .background(SeverityWarningBg, RoundedCornerShape(12.dp))
                    .padding(12.dp),
            )
        }

        session.durationTo10KicksMinutes?.let {
            Text(
                "10 kicks in $it min",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}
