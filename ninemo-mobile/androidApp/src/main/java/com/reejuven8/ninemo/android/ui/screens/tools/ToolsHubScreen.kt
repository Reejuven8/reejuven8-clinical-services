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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class ToolItem(val title: String, val subtitle: String, val icon: ImageVector, val onClick: () -> Unit)

/** P10 — Tools Hub. Pure launcher grid; mode-aware badges land alongside F5 child mode. */
@Composable
fun ToolsHubScreen(
    currentWeek: Int?,
    onSymptomLog: () -> Unit,
    onVitalsWeight: () -> Unit,
    onKickCounter: () -> Unit,
    onContractionTimer: () -> Unit,
    onDiet: () -> Unit,
    onSummaryCard: () -> Unit,
) {
    val tools = listOf(
        ToolItem("Symptom log", "Daily check-in", Icons.Filled.Favorite, onSymptomLog),
        ToolItem("Vitals", "Weight · BP", Icons.Outlined.MonitorHeart, onVitalsWeight),
        ToolItem("Kick counter", "Time to 10 kicks", Icons.Outlined.Timer, onKickCounter),
        ToolItem("Contractions", "Timer & pattern", Icons.Outlined.Bloodtype, onContractionTimer),
        ToolItem("Is it safe?", "Diet search", Icons.Outlined.Search, onDiet),
        ToolItem("Summary card", "Show your doctor", Icons.Outlined.Description, onSummaryCard),
    )

    Column(Modifier.fillMaxSize()) {
        Text("Tools", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 20.dp, top = 14.dp))
        Text(
            if (currentWeek != null) "Everything for week $currentWeek" else "Everything you need",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, top = 2.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(tools) { tool ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                        .clickable(onClick = tool.onClick)
                        .padding(18.dp, 16.dp),
                ) {
                    Icon(tool.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(tool.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
                    Text(tool.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                "After delivery, this hub switches to Child mode: growth charts, vaccinations and milestones.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
