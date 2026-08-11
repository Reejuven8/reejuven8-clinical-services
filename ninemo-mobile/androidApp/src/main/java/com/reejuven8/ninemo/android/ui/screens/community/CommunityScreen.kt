package com.reejuven8.ninemo.android.ui.screens.community

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.EmptyView
import com.reejuven8.ninemo.android.ui.components.ErrorView
import com.reejuven8.ninemo.android.ui.components.LoadingSpinner
import com.reejuven8.ninemo.android.ui.components.NineMoTextField
import com.reejuven8.ninemo.android.ui.theme.Berry
import com.reejuven8.ninemo.shared.model.Channel
import com.reejuven8.ninemo.shared.model.ClubResponse
import com.reejuven8.ninemo.shared.viewmodel.CommunityViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import org.koin.compose.viewmodel.koinViewModel

/** P20 — Due Date Clubs. Tap a channel chip to open its live chat. */
@Composable
fun CommunityScreen(
    onOpenChannel: (clubId: String, channelId: String, channelName: String, alias: String?) -> Unit,
    onOpenContent: () -> Unit,
) {
    val vm: CommunityViewModel = koinViewModel()
    val clubsState by vm.clubs.collectAsStateWithLifecycle()
    val joining by vm.joining.collectAsStateWithLifecycle()
    var showJoin by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp, 14.dp, 20.dp, 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Community", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Articles",
                style = MaterialTheme.typography.labelLarge,
                color = Berry,
                modifier = Modifier.clickable(onClick = onOpenContent).padding(8.dp),
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp))
                .clickable { showJoin = true }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Berry)
            Text(
                "Join your due-date club",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 10.dp),
            )
        }

        when (val state = clubsState) {
            is UiState.Loading -> LoadingSpinner()
            is UiState.Error -> ErrorView(state.throwable, onRetry = vm::load)
            is UiState.Empty -> EmptyView("You haven't joined a club yet.")
            is UiState.Success -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            ) {
                state.data.forEach { club -> ClubCard(club, onOpenChannel) }
            }
        }
    }

    if (showJoin) {
        JoinClubDialog(
            joining = joining,
            onDismiss = { showJoin = false },
            onJoin = { month, alias ->
                vm.join(month, alias) { club, chosenAlias ->
                    showJoin = false
                    val channel = club.channels.firstOrNull { it.isDefault } ?: club.channels.firstOrNull()
                    if (channel != null) onOpenChannel(club.id, channel.channelId, channel.name, chosenAlias)
                }
            },
        )
    }
}

@Composable
private fun ClubCard(
    club: ClubResponse,
    onOpenChannel: (String, String, String, String?) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text(club.clubName, style = MaterialTheme.typography.titleMedium)
        Text(
            "${club.memberCount} members",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            club.channels.forEach { channel ->
                ChannelChip(channel) { onOpenChannel(club.id, channel.channelId, channel.name, null) }
            }
        }
    }
}

@Composable
private fun ChannelChip(channel: Channel, onClick: () -> Unit) {
    Text(
        "# ${channel.name}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun JoinClubDialog(
    joining: Boolean,
    onDismiss: () -> Unit,
    onJoin: (dueDateMonth: String, alias: String) -> Unit,
) {
    var month by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    val monthValid = Regex("""\d{4}-\d{2}""").matches(month)
    val canJoin = monthValid && alias.isNotBlank() && !joining

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join a club") },
        text = {
            Column {
                Text(
                    "Clubs are grouped by your due month. Pick a display alias — chats are anonymous.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NineMoTextField(
                    value = month,
                    onValueChange = { month = it },
                    label = "Due month (YYYY-MM)",
                    keyboardType = KeyboardType.Number,
                    isError = month.isNotEmpty() && !monthValid,
                    modifier = Modifier.padding(top = 12.dp),
                )
                NineMoTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = "Your alias",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onJoin(month, alias.trim()) }, enabled = canJoin) {
                Text(if (joining) "Joining…" else "Join")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
