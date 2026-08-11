package com.reejuven8.ninemo.android.ui.screens.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.NineMoButton
import com.reejuven8.ninemo.android.ui.theme.Berry
import com.reejuven8.ninemo.android.ui.theme.SeverityCritical
import com.reejuven8.ninemo.android.ui.theme.SeverityCriticalBg
import com.reejuven8.ninemo.shared.model.VitalType
import com.reejuven8.ninemo.shared.model.VitalsLogResponse
import com.reejuven8.ninemo.shared.viewmodel.UiState
import com.reejuven8.ninemo.shared.viewmodel.VitalsViewModel
import org.koin.compose.viewmodel.koinViewModel

/** P12 — Vitals. alertTriggered/isWithinNormalRange are server-computed; UI only renders them. */
@Composable
fun VitalsScreen(initialTab: VitalType, onBack: () -> Unit) {
    val vm: VitalsViewModel = koinViewModel()
    val activeTab by vm.activeTab.collectAsStateWithLifecycle()
    val weightHistory by vm.weightHistory.collectAsStateWithLifecycle()
    val bpHistory by vm.bpHistory.collectAsStateWithLifecycle()
    val submitState by vm.submitState.collectAsStateWithLifecycle()

    LaunchedEffect(initialTab) { vm.setActiveTab(initialTab) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Vitals", style = MaterialTheme.typography.headlineSmall)
        }

        TabRow(selectedTabIndex = if (activeTab == VitalType.WEIGHT) 0 else 1) {
            Tab(selected = activeTab == VitalType.WEIGHT, onClick = { vm.setActiveTab(VitalType.WEIGHT) }, text = { Text("Weight") })
            Tab(selected = activeTab == VitalType.BLOOD_PRESSURE, onClick = { vm.setActiveTab(VitalType.BLOOD_PRESSURE) }, text = { Text("Blood Pressure") })
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            when (activeTab) {
                VitalType.WEIGHT -> WeightTab(weightHistory, submitState, onLog = vm::logWeight)
                VitalType.BLOOD_PRESSURE -> BloodPressureTab(bpHistory, submitState, onLog = vm::logBloodPressure)
                else -> Unit
            }
        }
    }
}

@Composable
private fun WeightTab(
    history: UiState<List<VitalsLogResponse>>,
    submitState: UiState<VitalsLogResponse>,
    onLog: (Double) -> Unit,
) {
    var weight by remember { mutableStateOf("") }

    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
        )
        NineMoButton(
            text = "Log",
            onClick = { weight.toDoubleOrNull()?.let(onLog) },
            loading = submitState is UiState.Loading,
            enabled = weight.toDoubleOrNull() != null,
            modifier = Modifier.weight(1f),
        )
    }

    TrendSection(
        title = "Recent weight",
        history = history,
        formatEntry = { "${it.measurements.weightKg ?: "—"} kg" },
    )
}

@Composable
private fun BloodPressureTab(
    history: UiState<List<VitalsLogResponse>>,
    submitState: UiState<VitalsLogResponse>,
    onLog: (Int, Int) -> Unit,
) {
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }

    val latestAlert = (history as? UiState.Success)?.data?.firstOrNull()?.takeIf { it.alertTriggered }
    if (latestAlert != null) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(SeverityCriticalBg, RoundedCornerShape(18.dp))
                .padding(16.dp),
        ) {
            Column {
                Text(
                    "High reading — ${latestAlert.measurements.bloodPressureSystolic}/${latestAlert.measurements.bloodPressureDiastolic}",
                    style = MaterialTheme.typography.titleSmall,
                    color = SeverityCritical,
                )
                Text(
                    "Your care engine flagged today's log. Rest 10 minutes and re-measure; call your doctor if it stays high.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = systolic,
            onValueChange = { systolic = it },
            label = { Text("Systolic") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
        )
        OutlinedTextField(
            value = diastolic,
            onValueChange = { diastolic = it },
            label = { Text("Diastolic") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
        )
    }
    NineMoButton(
        text = "Log",
        onClick = {
            val s = systolic.toIntOrNull()
            val d = diastolic.toIntOrNull()
            if (s != null && d != null) onLog(s, d)
        },
        loading = submitState is UiState.Loading,
        enabled = systolic.toIntOrNull() != null && diastolic.toIntOrNull() != null,
        modifier = Modifier.padding(top = 12.dp),
    )

    if (history is UiState.Success && history.data.size >= 2) {
        BpTrendChart(history.data.take(6).reversed())
    }

    TrendSection(
        title = "Recent readings",
        history = history,
        formatEntry = { "${it.measurements.bloodPressureSystolic ?: "—"} / ${it.measurements.bloodPressureDiastolic ?: "—"} mmHg" },
    )
}

@Composable
private fun TrendSection(
    title: String,
    history: UiState<List<VitalsLogResponse>>,
    formatEntry: (VitalsLogResponse) -> String,
) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
    )
    when (history) {
        is UiState.Success -> history.data.forEach { entry ->
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
                    Modifier.size(10.dp).background(
                        if (entry.alertTriggered) SeverityCritical else Color(0xFF7BA888),
                        CircleShape,
                    ),
                )
                Text(formatEntry(entry), style = MaterialTheme.typography.bodyMedium)
            }
        }
        is UiState.Empty -> Text("No readings logged yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        is UiState.Error -> Text("Couldn't load history.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        is UiState.Loading -> {}
    }
}

@Composable
private fun BpTrendChart(readings: List<VitalsLogResponse>) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(
            "LAST ${readings.size} READINGS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Canvas(Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp)) {
            val systolics = readings.mapNotNull { it.measurements.bloodPressureSystolic?.toFloat() }
            val diastolics = readings.mapNotNull { it.measurements.bloodPressureDiastolic?.toFloat() }
            if (systolics.size < 2) return@Canvas
            val allValues = systolics + diastolics
            val minY = (allValues.minOrNull() ?: 0f) - 5f
            val maxY = (allValues.maxOrNull() ?: 1f) + 5f
            val range = (maxY - minY).coerceAtLeast(1f)
            val stepX = size.width / (systolics.size - 1).coerceAtLeast(1)

            fun points(values: List<Float>) = values.mapIndexed { i, v ->
                Offset(i * stepX, size.height - ((v - minY) / range) * size.height)
            }

            val systolicPoints = points(systolics)
            for (i in 0 until systolicPoints.size - 1) {
                drawLine(Berry, systolicPoints[i], systolicPoints[i + 1], strokeWidth = 5f)
            }
            if (diastolics.size == systolics.size) {
                val diastolicPoints = points(diastolics)
                for (i in 0 until diastolicPoints.size - 1) {
                    drawLine(Berry.copy(alpha = 0.45f), diastolicPoints[i], diastolicPoints[i + 1], strokeWidth = 5f)
                }
            }
        }
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("— Systolic", style = MaterialTheme.typography.labelSmall, color = Berry)
            Text("— Diastolic", style = MaterialTheme.typography.labelSmall, color = Berry.copy(alpha = 0.45f))
        }
    }
}
