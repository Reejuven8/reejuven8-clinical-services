package com.reejuven8.ninemo.android.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.ErrorView
import com.reejuven8.ninemo.android.ui.components.LoadingSpinner
import com.reejuven8.ninemo.android.ui.theme.Berry
import com.reejuven8.ninemo.shared.model.SummaryCardResponse
import com.reejuven8.ninemo.shared.viewmodel.SummaryCardViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import kotlinx.serialization.json.JsonPrimitive
import org.koin.compose.viewmodel.koinViewModel

private val CardDark = Color(0xFF3E0A2A)

/** P16 — Summary Card. Patient's own flash card to hand to a doctor in person; all values server-supplied. */
@Composable
fun SummaryCardScreen(onBack: () -> Unit) {
    val vm: SummaryCardViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(CardDark)) {
        Row(Modifier.padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Doctor's summary", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }

        when (val s = state) {
            is UiState.Loading -> LoadingSpinner()
            is UiState.Error -> ErrorView(s.throwable, onRetry = vm::load)
            is UiState.Empty -> {}
            is UiState.Success -> SummaryCardContent(s.data)
        }
    }
}

@Composable
private fun SummaryCardContent(card: SummaryCardResponse) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(22.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(card.patientName, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                    Text(
                        card.bloodGroup?.let { "$it" } ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("W${card.gestationalWeek}", style = MaterialTheme.typography.headlineMedium, color = Berry, fontFamily = FontFamily.Serif)
                    Text("EDD ${card.eddDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider()

            val vitalsFlat = card.latestVitals.mapValues { (_, v) -> (v as? JsonPrimitive)?.content }
            SummaryGrid(
                weightLabel = vitalsFlat["weight_kg"]?.let { "$it kg" } ?: "—",
                bpLabel = listOfNotNull(vitalsFlat["blood_pressure_systolic"], vitalsFlat["blood_pressure_diastolic"])
                    .takeIf { it.size == 2 }?.joinToString(" / ") ?: "—",
                riskFlags = card.highRiskFlags,
            )

            HorizontalDivider()

            Text(
                "RECENT ABNORMAL FLAGS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (card.recentSymptoms.isEmpty()) {
                Text("None logged.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            } else {
                card.recentSymptoms.forEach { symptom ->
                    val severity = (symptom["severityFlag"] as? JsonPrimitive)?.content ?: "—"
                    Text("$severity flag logged", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Values as returned by NineMo servers",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun HorizontalDivider() {
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxWidth().height(1.dp).padding(vertical = 16.dp).background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun SummaryGrid(weightLabel: String, bpLabel: String, riskFlags: List<String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        GridCell("LATEST WEIGHT", weightLabel, Modifier.weight(1f))
        GridCell("LAST BP", bpLabel, Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        GridCell("RISK FLAGS", riskFlags.firstOrNull() ?: "None", Modifier.weight(1f))
        GridCell("", "", Modifier.weight(1f))
    }
}

@Composable
private fun GridCell(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        if (label.isNotEmpty()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
