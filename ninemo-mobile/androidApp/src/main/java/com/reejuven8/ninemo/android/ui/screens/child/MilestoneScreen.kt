package com.reejuven8.ninemo.android.ui.screens.child

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.ErrorView
import com.reejuven8.ninemo.android.ui.components.LoadingSpinner
import com.reejuven8.ninemo.android.ui.components.SeverityBanner
import com.reejuven8.ninemo.android.ui.theme.Berry
import com.reejuven8.ninemo.shared.model.DevelopmentalMilestoneResponse
import com.reejuven8.ninemo.shared.model.SeverityFlag
import com.reejuven8.ninemo.shared.viewmodel.MilestoneMonths
import com.reejuven8.ninemo.shared.viewmodel.MilestoneViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import org.koin.compose.viewmodel.koinViewModel

/** P19 — Milestone Checklist. Delay-risk assessment is server-side (via alertFlags); UI renders. */
@Composable
fun MilestoneScreen(childId: String, onBack: () -> Unit) {
    val vm: MilestoneViewModel = koinViewModel()
    val selectedMonth by vm.selectedMonth.collectAsStateWithLifecycle()
    val checkInState by vm.checkIn.collectAsStateWithLifecycle()

    LaunchedEffect(childId) { vm.load(childId) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Milestones", style = MaterialTheme.typography.headlineSmall)
        }

        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MilestoneMonths.forEach { month ->
                val selected = month == selectedMonth
                Text(
                    "$month mo",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(if (selected) Berry else MaterialTheme.colorScheme.surface, RoundedCornerShape(100.dp))
                        .clickable { vm.selectMonth(month) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        when (val state = checkInState) {
            is UiState.Loading -> LoadingSpinner()
            is UiState.Error -> ErrorView(state.throwable, onRetry = { vm.selectMonth(selectedMonth) })
            is UiState.Empty -> {}
            is UiState.Success -> CheckInContent(state.data, onToggle = vm::toggle)
        }
    }
}

@Composable
private fun CheckInContent(checkIn: DevelopmentalMilestoneResponse, onToggle: (String, Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        val total = checkIn.milestones.size
        val achieved = checkIn.achievedCount

        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)).padding(16.dp),
        ) {
            Text("$achieved / $total achieved", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else achieved.toFloat() / total },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                color = Berry,
            )
        }

        if (checkIn.hasDelayRisk) {
            SeverityBanner(
                flag = SeverityFlag.WARNING,
                title = "Worth discussing with your pediatrician",
                lines = listOf("Fewer than half of this month's milestones are marked — your care engine flagged this for review."),
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Column(Modifier.padding(top = 16.dp)) {
            checkIn.milestones.forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(item.name, !item.achieved) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = item.achieved, onCheckedChange = { onToggle(item.name, it) })
                    Text(item.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}
