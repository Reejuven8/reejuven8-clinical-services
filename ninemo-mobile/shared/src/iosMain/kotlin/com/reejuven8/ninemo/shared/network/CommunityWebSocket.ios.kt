package com.reejuven8.ninemo.shared.network

import org.hildan.krossbow.websocket.WebSocketClient

// TODO(F-iOS): wire krossbow-websocket-ktor (KtorWebSocketClient over Darwin + WebSockets
// plugin). Android is the only built/tested target this phase; iOS chat lands in the iOS pass.
actual fun communityWebSocketClient(): WebSocketClient =
    TODO("iOS community WebSocket transport not wired yet (F-iOS)")
