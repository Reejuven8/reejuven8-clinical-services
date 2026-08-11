package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ChatMessageResponse
import com.reejuven8.ninemo.shared.model.SendMessageRequest
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.PlatformConfig
import com.reejuven8.ninemo.shared.network.communityWebSocketClient
import com.reejuven8.ninemo.shared.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.conversions.kxserialization.StompSessionWithKxSerialization
import org.hildan.krossbow.stomp.conversions.kxserialization.convertAndSend
import org.hildan.krossbow.stomp.conversions.kxserialization.json.withJsonConversions
import org.hildan.krossbow.stomp.conversions.kxserialization.subscribe

/**
 * P20 live chat over STOMP. Connects straight to community-service (:8086) via SockJS's
 * raw-websocket transport (`/ws/connect/websocket`) — the gateway is bypassed; the JWT
 * rides the STOMP CONNECT frame and the service authenticates it (StompAuthChannelInterceptor).
 *
 * One instance per open channel screen: connect() -> incoming()/send() -> disconnect().
 */
class ChatSocketClient(private val session: SessionStore) {

    private val stompClient = StompClient(communityWebSocketClient())
    private var stompSession: StompSessionWithKxSerialization? = null
    private val teardownScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun connect() {
        if (stompSession != null) return
        val token = session.tokens()?.accessToken ?: error("Not authenticated")
        val raw = stompClient.connect(
            url = "${PlatformConfig.wsBaseUrl}/ws/connect/websocket",
            customStompConnectHeaders = mapOf("Authorization" to "Bearer $token"),
        )
        stompSession = raw.withJsonConversions()
    }

    /** Broadcast stream for one channel. Suspends to register the STOMP subscription. */
    suspend fun incoming(clubId: String, channelId: String): Flow<ChatMessageResponse> =
        requireSession().subscribe(
            ApiRoutes.chatTopic(clubId, channelId),
            ChatMessageResponse.serializer(),
        )

    suspend fun send(clubId: String, channelId: String, request: SendMessageRequest) {
        requireSession().convertAndSend(
            ApiRoutes.chatSend(clubId, channelId),
            request,
            SendMessageRequest.serializer(),
        )
    }

    suspend fun disconnect() {
        stompSession?.disconnect()
        stompSession = null
    }

    /** Fire-and-forget teardown for ViewModel.onCleared() (can't suspend there). */
    fun close() {
        teardownScope.launch { runCatching { disconnect() } }
    }

    private fun requireSession(): StompSessionWithKxSerialization =
        stompSession ?: error("Chat socket not connected — call connect() first")
}
