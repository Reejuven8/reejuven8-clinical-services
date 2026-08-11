package com.reejuven8.ninemo.android.ui.screens.child

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.ErrorView
import com.reejuven8.ninemo.android.ui.components.LoadingSpinner
import com.reejuven8.ninemo.android.ui.theme.Berry
import com.reejuven8.ninemo.android.ui.theme.SeverityNormal
import com.reejuven8.ninemo.android.ui.theme.SeverityWarning
import com.reejuven8.ninemo.shared.viewmodel.ChildDashboard
import com.reejuven8.ninemo.shared.viewmodel.ChildDashboardViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import org.koin.compose.viewmodel.koinViewModel

/** P6 — Child Dashboard (child mode Home). All metrics server-computed; this is a summary surface. */
@Composable
fun ChildDashboardScreen(
    childId: String,
    childName: String?,
    onLogGrowth: () -> Unit,
    onMilestones: () -> Unit,
    onVaccines: () -> Unit,
) {
    val vm: ChildDashboardViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(childId) { vm.load(childId) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(20.dp, 16.dp, 20.dp, 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (childName?.firstOrNull() ?: 'B').uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Berry,
                    fontFamily = FontFamily.Serif,
                )
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(childName ?: "Your baby", style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Serif)
                Text("Child mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        when (val s = state) {
            is UiState.Loading -> LoadingSpinner()
            is UiState.Error -> ErrorView(s.throwable, onRetry = { vm.load(childId) })
            is UiState.Empty -> {}
            is UiState.Success -> DashboardContent(s.data, onVaccines)
        }

        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("Log growth", onLogGrowth)
            QuickAction("Milestones", onMilestones)
            QuickAction("Vaccines", onVaccines)
        }
    }
}

@Composable
private fun DashboardContent(data: ChildDashboard, onVaccines: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        data.nextVaccination?.let { vaccine ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
                    .clickable(onClick = onVaccines)
                    .padding(18.dp),
            ) {
                Text("NEXT VACCINATION", style = MaterialTheme.typography.labelSmall, color = Berry)
                Text(vaccine.vaccineName, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif, modifier = Modifier.padding(top = 6.dp))
                Text(
                    if (vaccine.isOverdue) "Overdue — was due ${vaccine.scheduledDate}" else "Due ${vaccine.scheduledDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (vaccine.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCell("Growth", Modifier.weight(1f)) {
                val pct = data.latestGrowth?.percentiles?.get("weight_for_age")
                Text(
                    pct?.let { "P$it" } ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Serif,
                )
                Text("weight-for-age", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SummaryCell("Alerts", Modifier.weight(1f)) {
                val clear = !data.anyOverdue && !data.anyGrowthAlert
                Text(
                    if (clear) "All clear" else "Needs review",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (clear) SeverityNormal else SeverityWarning,
                )
            }
        }
    }
}

@Composable
private fun SummaryCell(label: String, modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Berry)
        Box(Modifier.padding(top = 6.dp)) { Column { content() } }
    }
}

@Composable
private fun RowScope.QuickAction(label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
