package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.ChatMessageResponse
import com.reejuven8.ninemo.shared.model.SendMessageRequest
import com.reejuven8.ninemo.shared.repository.ChatSocketClient
import com.reejuven8.ninemo.shared.repository.CommunityRepository
import com.reejuven8.ninemo.shared.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * P20 — one channel's live chat. REST history + STOMP live stream merged into one
 * oldest-first list (chat scrolls to the bottom). Instance is per-channel-screen:
 * start() connects, onCleared() tears the socket down.
 *
 * NOTE: the sender's club alias is not retrievable from the GET /clubs DTO (backend gap —
 * ClubResponse omits members). It is seeded from the route (the alias used at join time)
 * and stays user-editable; defaults to "Member" when unknown.
 */
class ChatViewModel(
    private val repository: CommunityRepository,
    private val socket: ChatSocketClient,
    private val session: SessionStore,
) : ViewModel() {

    private lateinit var clubId: String
    private lateinit var channelId: String
    private var started = false

    private val _messages = MutableStateFlow<UiState<List<ChatMessageResponse>>>(UiState.Loading)
    val messages: StateFlow<UiState<List<ChatMessageResponse>>> = _messages.asStateFlow()

    private val _alias = MutableStateFlow("Member")
    val alias: StateFlow<String> = _alias.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private var myUserId: String? = null
    private val ordered = mutableListOf<ChatMessageResponse>() // oldest-first, deduped by id

    fun start(clubId: String, channelId: String, alias: String?) {
        if (started) return
        started = true
        this.clubId = clubId
        this.channelId = channelId
        if (!alias.isNullOrBlank()) _alias.value = alias

        viewModelScope.launch {
            myUserId = session.tokens()?.userId
            // History first (server returns newest-first → reverse to oldest-first).
            repository.history(clubId, channelId).fold(
                onSuccess = { history ->
                    ordered.clear()
                    ordered.addAll(history.reversed())
                    publish()
                },
                onFailure = { _messages.value = UiState.Error(it) },
            )
            // Then go live.
            runCatching {
                socket.connect()
                _connected.value = true
                socket.incoming(clubId, channelId).collect { upsert(it) }
            }.onFailure { _connected.value = false }
        }
    }

    fun setAlias(value: String) { _alias.value = value }

    fun isMine(message: ChatMessageResponse): Boolean = message.senderId == myUserId

    fun send(body: String) {
        val text = body.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                socket.send(clubId, channelId, SendMessageRequest(senderAlias = _alias.value, messageBody = text))
            }
            // The server echoes the saved message back over the topic subscription — no
            // optimistic insert needed; upsert() dedupes if it ever races.
        }
    }

    fun delete(message: ChatMessageResponse) {
        viewModelScope.launch {
            repository.deleteMessage(clubId, channelId, message.id).onSuccess {
                ordered.removeAll { it.id == message.id }
                publish()
            }
        }
    }

    private fun upsert(message: ChatMessageResponse) {
        val idx = ordered.indexOfFirst { it.id == message.id }
        if (idx >= 0) ordered[idx] = message else ordered.add(message)
        publish()
    }

    private fun publish() {
        _messages.value = if (ordered.isEmpty()) UiState.Empty else UiState.Success(ordered.toList())
    }

    override fun onCleared() {
        socket.close()
    }
}
