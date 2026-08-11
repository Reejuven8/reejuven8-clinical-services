package com.reejuven8.ninemo.android.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.NineMoButton
import com.reejuven8.ninemo.android.ui.components.SeverityBanner
import com.reejuven8.ninemo.shared.model.SeverityFlag
import com.reejuven8.ninemo.shared.viewmodel.CommonSymptoms
import com.reejuven8.ninemo.shared.viewmodel.SymptomLogViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import org.koin.compose.viewmodel.koinViewModel

/** P11 — Symptom Log. Severity verdict always comes from the server, never computed here. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomLogScreen(onBack: () -> Unit) {
    val vm: SymptomLogViewModel = koinViewModel()
    val selected by vm.selectedSymptoms.collectAsStateWithLifecycle()
    val severity by vm.severity.collectAsStateWithLifecycle()
    val attachVitals by vm.attachVitals.collectAsStateWithLifecycle()
    val submitState by vm.submitState.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text("Symptom log", style = MaterialTheme.typography.headlineSmall)

        val result = submitState
        if (result is UiState.Success) {
            SeverityBanner(
                flag = result.data.severityFlag,
                title = severityTitle(result.data.severityFlag),
                lines = if (result.data.triggeredRules.isNotEmpty()) {
                    listOf("This assessment comes from your care engine based on today's entry.")
                } else {
                    listOf("Logged — no concerning pattern detected.")
                },
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        Text(
            "What are you feeling?",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CommonSymptoms.forEach { symptom ->
                FilterChip(
                    selected = symptom in selected,
                    onClick = { vm.toggleSymptom(symptom) },
                    label = { Text(symptom.replace('_', ' ').replaceFirstChar(Char::uppercase)) },
                )
            }
        }

        Text(
            "How severe?",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 22.dp, bottom = 6.dp),
        )
        Slider(value = severity, onValueChange = vm::setSeverity)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mild", style = MaterialTheme.typography.labelSmall)
            Text("Moderate", style = MaterialTheme.typography.labelSmall)
            Text("Severe", style = MaterialTheme.typography.labelSmall)
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Attach today's vitals", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = attachVitals, onCheckedChange = vm::setAttachVitals)
        }

        NineMoButton(
            text = "Submit for assessment",
            onClick = vm::submit,
            loading = submitState is UiState.Loading,
            enabled = selected.isNotEmpty(),
            modifier = Modifier.padding(top = 20.dp),
        )

        Text(
            "History",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 28.dp, bottom = 10.dp),
        )
        when (val historyState = history) {
            is UiState.Success -> historyState.data.forEach { entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(10.dp).background(severityDotColor(entry.severityFlag), CircleShape),
                    )
                    Column {
                        Text(entry.symptoms.joinToString { it.name.replace('_', ' ') }, style = MaterialTheme.typography.bodyMedium)
                        Text("${entry.severityFlag}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            is UiState.Empty -> Text("No symptom logs yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            is UiState.Error -> Text("Couldn't load history.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            is UiState.Loading -> {}
        }
    }
}

private fun severityTitle(flag: SeverityFlag): String = when (flag) {
    SeverityFlag.CRITICAL -> "CRITICAL — contact your doctor now"
    SeverityFlag.WARNING -> "WARNING — worth a check"
    SeverityFlag.NORMAL -> "Logged"
}

private fun severityDotColor(flag: SeverityFlag) = when (flag) {
    SeverityFlag.CRITICAL -> androidx.compose.ui.graphics.Color(0xFFBA1A1A)
    SeverityFlag.WARNING -> androidx.compose.ui.graphics.Color(0xFFE0A100)
    SeverityFlag.NORMAL -> androidx.compose.ui.graphics.Color(0xFF7BA888)
}
