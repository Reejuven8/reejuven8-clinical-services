package com.reejuven8.ninemo.android.ui.screens.community

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.EmptyView
import com.reejuven8.ninemo.android.ui.components.ErrorView
import com.reejuven8.ninemo.android.ui.components.LoadingSpinner
import com.reejuven8.ninemo.shared.model.ChatMessageResponse
import com.reejuven8.ninemo.shared.viewmodel.ChatViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import org.koin.compose.viewmodel.koinViewModel

/** P20 — one channel's live chat (STOMP). History + live stream, send + delete-own. */
@Composable
fun ChannelChatScreen(
    clubId: String,
    channelId: String,
    channelName: String?,
    alias: String?,
    onBack: () -> Unit,
) {
    val vm: ChatViewModel = koinViewModel()
    LaunchedEffect(clubId, channelId) { vm.start(clubId, channelId, alias) }

    val messagesState by vm.messages.collectAsStateWithLifecycle()
    val connected by vm.connected.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text("# ${channelName ?: "Channel"}", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (connected) "Live" else "Connecting…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (val state = messagesState) {
                is UiState.Loading -> LoadingSpinner()
                is UiState.Error -> ErrorView(state.throwable)
                is UiState.Empty -> EmptyView("No messages yet — say hello 👋")
                is UiState.Success -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // ordered is oldest-first; reverse so newest sits at the bottom.
                    items(state.data.asReversed(), key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            mine = vm.isMine(message),
                            onDelete = { vm.delete(message) },
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
            )
            IconButton(
                onClick = { vm.send(draft); draft = "" },
                enabled = draft.isNotBlank(),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (draft.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(message: ChatMessageResponse, mine: Boolean, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .background(
                    if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(16.dp),
                )
                .combinedClickable(onClick = {}, onLongClick = { if (mine) onDelete() })
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (!mine) {
                Text(
                    message.senderAlias,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                message.messageBody,
                style = MaterialTheme.typography.bodyMedium,
                color = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                shortTime(message.sentAt),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.End,
                color = if (mine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** ISO-8601 -> HH:MM (display only; no clinical/temporal computation). */
private fun shortTime(iso: String): String {
    val t = iso.substringAfter('T', "")
    return if (t.length >= 5) t.substring(0, 5) else iso
}
