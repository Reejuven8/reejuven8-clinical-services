package com.reejuven8.ninemo.android.ui.screens.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.reejuven8.ninemo.shared.model.TimelineJsonObject
import com.reejuven8.ninemo.shared.model.TimelineResponse
import com.reejuven8.ninemo.shared.viewmodel.TimelineViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import kotlinx.serialization.json.JsonPrimitive
import org.koin.compose.viewmodel.koinViewModel

/** P5 — Home/Timeline. Never computes gestational week/EDD itself; renders server output. */
@Composable
fun TimelineScreen(
    onLogSymptom: () -> Unit,
    onLogVitals: () -> Unit,
    onSummaryCard: () -> Unit,
    onBabyArrived: () -> Unit,
) {
    val vm: TimelineViewModel = koinViewModel()
    val timelineState by vm.timelineState.collectAsStateWithLifecycle()
    val banner by vm.banner.collectAsStateWithLifecycle()

    when (val state = timelineState) {
        is UiState.Loading -> LoadingSpinner()
        is UiState.Error -> ErrorView(state.throwable, onRetry = vm::loadCurrentWeek)
        is UiState.Empty -> {}
        is UiState.Success -> TimelineContent(
            timeline = state.data,
            bannerText = banner?.let { entry ->
                "${entry.severityFlag} — ${entry.symptoms.joinToString { it.name.replace('_', ' ') }}"
            },
            onWeekSelected = vm::loadWeek,
            onLogSymptom = onLogSymptom,
            onLogVitals = onLogVitals,
            onSummaryCard = onSummaryCard,
            onBabyArrived = onBabyArrived,
        )
    }
}

@Composable
private fun TimelineContent(
    timeline: TimelineResponse,
    bannerText: String?,
    onWeekSelected: (Int) -> Unit,
    onLogSymptom: () -> Unit,
    onLogVitals: () -> Unit,
    onSummaryCard: () -> Unit,
    onBabyArrived: () -> Unit,
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("This Week", "Milestones", "Diet & Wellness")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            "Week ${timeline.gestationalWeek} of 40",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 20.dp, top = 14.dp),
        )

        WeekPager(currentWeek = timeline.gestationalWeek, onWeekSelected = onWeekSelected)

        if (bannerText != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Recent symptom log", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                    Text(bannerText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        TabRow(selectedTabIndex = tabIndex, modifier = Modifier.padding(top = 12.dp)) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = tabIndex == i, onClick = { tabIndex = i }, text = { Text(title) })
            }
        }

        Box(Modifier.padding(20.dp)) {
            when (tabIndex) {
                0 -> ThisWeekTab(timeline.babyDevelopment, timeline.maternalChanges)
                1 -> MilestonesTab(timeline.scheduledMilestones)
                else -> DietTab(timeline.dietTips, timeline.yogaRoutine)
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("Log symptom", onLogSymptom, Modifier.weight(1f))
            QuickAction("Log vitals", onLogVitals, Modifier.weight(1f))
            QuickAction("Summary card", onSummaryCard, Modifier.weight(1f))
        }

        Text(
            "Baby has arrived? Switch to Child mode",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .clickable(onClick = onBabyArrived)
                .padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun RowScope.QuickAction(label: String, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun WeekPager(currentWeek: Int, onWeekSelected: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ((currentWeek - 2).coerceAtLeast(1)..(currentWeek + 2).coerceAtMost(42)).forEach { week ->
            val selected = week == currentWeek
            Column(
                Modifier
                    .width(44.dp)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onWeekSelected(week) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "W",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "$week",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun TimelineJsonObject.stringOrNull(key: String): String? = (this[key] as? JsonPrimitive)?.content

@Composable
private fun ThisWeekTab(babyDevelopment: TimelineJsonObject, maternalChanges: List<String>) {
    Column {
        val summary = babyDevelopment.stringOrNull("summary")
        val sizeComparison = babyDevelopment.stringOrNull("size_comparison")
        Text(
            "Development this week",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            sizeComparison ?: summary ?: "Content for this week is being prepared.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (maternalChanges.isNotEmpty()) {
            Text(
                "You may notice",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 20.dp),
            )
            maternalChanges.forEach {
                Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun MilestonesTab(milestones: List<TimelineJsonObject>) {
    if (milestones.isEmpty()) {
        Text("No milestones scheduled right now.", style = MaterialTheme.typography.bodyMedium)
        return
    }
    Column {
        milestones.forEach { milestone ->
            val name = milestone.stringOrNull("name") ?: "Upcoming milestone"
            val description = milestone.stringOrNull("description")
            Column(Modifier.padding(bottom = 12.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DietTab(dietTips: List<TimelineJsonObject>, yogaRoutine: TimelineJsonObject?) {
    Column {
        if (dietTips.isEmpty()) {
            Text("Diet tips for this week are being prepared.", style = MaterialTheme.typography.bodyMedium)
        } else {
            dietTips.forEach { tip ->
                Text("• ${tip.stringOrNull("tip") ?: ""}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 6.dp))
            }
        }
        val yogaTitle = yogaRoutine?.stringOrNull("title")
        if (yogaTitle != null) {
            Text(
                "Yoga: $yogaTitle",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
