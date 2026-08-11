package com.reejuven8.ninemo.android.ui.screens.child

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.ErrorView
import com.reejuven8.ninemo.android.ui.components.LoadingSpinner
import com.reejuven8.ninemo.android.ui.theme.Berry
import com.reejuven8.ninemo.android.ui.theme.SeverityCritical
import com.reejuven8.ninemo.android.ui.theme.SeverityCriticalBg
import com.reejuven8.ninemo.shared.model.VaccinationRecordResponse
import com.reejuven8.ninemo.shared.viewmodel.UiState
import com.reejuven8.ninemo.shared.viewmodel.VaccinationViewModel
import org.koin.compose.viewmodel.koinViewModel

/** P18 — Vaccination Tracker. IAP schedule + OVERDUE status server-computed. */
@Composable
fun VaccinationScreen(childId: String, onBack: () -> Unit) {
    val vm: VaccinationViewModel = koinViewModel()
    val scheduleState by vm.schedule.collectAsStateWithLifecycle()
    var tabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(childId) { vm.load(childId) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Vaccinations", style = MaterialTheme.typography.headlineSmall)
        }
        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Upcoming") })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Completed") })
        }

        when (val state = scheduleState) {
            is UiState.Loading -> LoadingSpinner()
            is UiState.Error -> ErrorView(state.throwable, onRetry = { vm.load(childId) })
            is UiState.Empty -> Text("No schedule yet.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(20.dp))
            is UiState.Success -> {
                val records = if (tabIndex == 0) vm.upcoming(state.data) else vm.completed(state.data)
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                    if (records.isEmpty()) {
                        Text(
                            if (tabIndex == 0) "Nothing due — all caught up." else "No completed doses yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    records.sortedBy { it.scheduledDate }.forEach { record ->
                        VaccineCard(record, showMark = tabIndex == 0, onMark = { vm.markCompleted(record.id) })
                    }
                    if (tabIndex == 0) {
                        Text(
                            "Schedule follows the IAP chart, generated when your baby's profile was created. Dates come from your care engine.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaccineCard(record: VaccinationRecordResponse, showMark: Boolean, onMark: () -> Unit) {
    val overdue = record.isOverdue
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .background(if (overdue) SeverityCriticalBg else MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .then(if (overdue) Modifier.border(1.5.dp, SeverityCritical, RoundedCornerShape(20.dp)) else Modifier)
            .padding(16.dp, 16.dp),
    ) {
        if (overdue) {
            Text("OVERDUE", style = MaterialTheme.typography.labelSmall, color = SeverityCritical)
        } else {
            Text(
                if (record.administeredDate != null) "GIVEN ${record.administeredDate}" else "DUE ${record.scheduledDate}",
                style = MaterialTheme.typography.labelSmall,
                color = Berry,
            )
        }
        Text(
            "${record.vaccineName} · dose ${record.doseNumber}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (overdue) {
            Text("Was due ${record.scheduledDate}", style = MaterialTheme.typography.bodySmall, color = SeverityCritical, modifier = Modifier.padding(top = 2.dp))
        }
        if (showMark) {
            Text(
                "Mark completed",
                style = MaterialTheme.typography.labelLarge,
                color = if (overdue) SeverityCritical else Berry,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .border(1.5.dp, if (overdue) SeverityCritical else Berry, RoundedCornerShape(100.dp))
                    .clickable(onClick = onMark)
                    .padding(horizontal = 20.dp, vertical = 9.dp),
            )
        }
    }
}
