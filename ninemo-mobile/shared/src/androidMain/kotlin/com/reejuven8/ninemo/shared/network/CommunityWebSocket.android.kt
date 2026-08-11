package com.reejuven8.ninemo.shared.network

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient

actual fun communityWebSocketClient(): WebSocketClient = OkHttpWebSocketClient()
