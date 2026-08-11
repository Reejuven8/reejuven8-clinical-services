package com.reejuven8.ninemo.shared.network

import org.hildan.krossbow.websocket.WebSocketClient

/**
 * Platform WebSocket transport for the STOMP chat client.
 * Android: OkHttp. iOS: Ktor/Darwin (wired in the iOS phase).
 * Points at community-service directly (wsBaseUrl :8086), bypassing the gateway —
 * the STOMP CONNECT frame carries the JWT and the service validates it itself.
 */
expect fun communityWebSocketClient(): WebSocketClient
