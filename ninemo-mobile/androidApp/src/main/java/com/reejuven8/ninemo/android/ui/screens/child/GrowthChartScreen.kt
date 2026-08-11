package com.reejuven8.ninemo.android.ui.screens.child

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.ErrorView
import com.reejuven8.ninemo.android.ui.components.LoadingSpinner
import com.reejuven8.ninemo.android.ui.components.NineMoButton
import com.reejuven8.ninemo.android.ui.components.SeverityBanner
import com.reejuven8.ninemo.android.ui.theme.Berry
import com.reejuven8.ninemo.shared.model.GrowthMeasurementResponse
import com.reejuven8.ninemo.shared.model.SeverityFlag
import com.reejuven8.ninemo.shared.viewmodel.GrowthMetric
import com.reejuven8.ninemo.shared.viewmodel.GrowthViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import org.koin.compose.viewmodel.koinViewModel

/** P17 — Growth Chart. Z-scores/percentiles/alerts are WHO-computed server-side; UI renders only. */
@Composable
fun GrowthChartScreen(childId: String, onBack: () -> Unit) {
    val vm: GrowthViewModel = koinViewModel()
    val metric by vm.activeMetric.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(childId) { vm.load(childId) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(4.dp, 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Growth", style = MaterialTheme.typography.headlineSmall)
        }

        TabRow(selectedTabIndex = metric.ordinal) {
            GrowthMetric.entries.forEach { m ->
                Tab(selected = metric == m, onClick = { vm.setMetric(m) }, text = { Text(m.label) })
            }
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            when (val state = history) {
                is UiState.Loading -> LoadingSpinner()
                is UiState.Error -> ErrorView(state.throwable, onRetry = { vm.load(childId) })
                is UiState.Empty -> Text("No measurements yet — add the first one.", style = MaterialTheme.typography.bodyMedium)
                is UiState.Success -> GrowthContent(state.data, metric)
            }
            NineMoButton(text = "Add measurement", onClick = { showAdd = true }, modifier = Modifier.padding(top = 20.dp))
        }
    }

    if (showAdd) {
        AddMeasurementDialog(
            onDismiss = { showAdd = false },
            onSubmit = { age, height, weight, head ->
                vm.add(age, height, weight, head)
                showAdd = false
            },
        )
    }
}

@Composable
private fun GrowthContent(history: List<GrowthMeasurementResponse>, metric: GrowthMetric) {
    val ordered = history.sortedBy { it.ageInMonths }
    val values = ordered.map {
        when (metric) {
            GrowthMetric.WEIGHT -> it.weightKg
            GrowthMetric.HEIGHT -> it.heightCm
            GrowthMetric.HEAD -> it.headCircumferenceCm ?: 0.0
        }.toFloat()
    }
    val latest = ordered.lastOrNull()

    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp)).padding(16.dp),
    ) {
        Text(
            "${metric.label.uppercase()}-FOR-AGE",
            style = MaterialTheme.typography.labelSmall,
            color = Berry,
        )
        // The child's own trajectory. Backend returns no WHO reference-curve data, so the
        // percentile band lines from the mockup aren't drawable here — percentile per point
        // is shown numerically instead.
        if (values.size >= 2) {
            Canvas(Modifier.fillMaxWidth().height(160.dp).padding(top = 10.dp)) {
                val minV = values.min() - 1f
                val maxV = values.max() + 1f
                val range = (maxV - minV).coerceAtLeast(1f)
                val stepX = size.width / (values.size - 1)
                val points = values.mapIndexed { i, v -> Offset(i * stepX, size.height - ((v - minV) / range) * size.height) }
                for (i in 0 until points.size - 1) drawLine(Berry, points[i], points[i + 1], strokeWidth = 5f)
            }
        } else {
            Text("Add more measurements to see the trend.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }

    latest?.let { m ->
        val pct = m.percentiles[metric.zScoreKey]
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Latest", latestValueLabel(m, metric), Modifier.weight(1f))
            StatCard("Percentile", pct?.let { "P$it" } ?: "—", Modifier.weight(1f))
        }
        if (m.alertFlags.isNotEmpty()) {
            SeverityBanner(
                flag = SeverityFlag.WARNING,
                title = "Growth flag from your care engine",
                lines = m.alertFlags,
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            SeverityBanner(
                flag = SeverityFlag.NORMAL,
                title = "On track",
                lines = listOf("No percentile-line crossings flagged."),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

private fun latestValueLabel(m: GrowthMeasurementResponse, metric: GrowthMetric): String = when (metric) {
    GrowthMetric.WEIGHT -> "${m.weightKg} kg"
    GrowthMetric.HEIGHT -> "${m.heightCm} cm"
    GrowthMetric.HEAD -> m.headCircumferenceCm?.let { "$it cm" } ?: "—"
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(14.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun AddMeasurementDialog(onDismiss: () -> Unit, onSubmit: (Int, Double, Double, Double?) -> Unit) {
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var head by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add measurement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(age, { age = it }, "Age (months)")
                NumberField(height, { height = it }, "Height (cm)")
                NumberField(weight, { weight = it }, "Weight (kg)")
                NumberField(head, { head = it }, "Head circumference (cm, optional)")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val a = age.toIntOrNull()
                    val h = height.toDoubleOrNull()
                    val w = weight.toDoubleOrNull()
                    if (a != null && h != null && w != null) onSubmit(a, h, w, head.toDoubleOrNull())
                },
                enabled = age.toIntOrNull() != null && height.toDoubleOrNull() != null && weight.toDoubleOrNull() != null,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}
