package com.reejuven8.ninemo.shared.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun platformHttpEngine(): HttpClientEngine = Darwin.create()

// iOS simulator reaches host loopback via localhost.
// TODO(F8): drive from build setting (dev vs prod https://api.reejuven8.com).
actual object PlatformConfig {
    actual val baseUrl: String = "http://localhost:8080/api/v1"
    actual val wsBaseUrl: String = "ws://localhost:8086"
}
