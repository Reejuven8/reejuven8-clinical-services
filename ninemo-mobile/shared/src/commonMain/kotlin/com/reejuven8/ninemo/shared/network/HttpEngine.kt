package com.reejuven8.ninemo.shared.network

import io.ktor.client.engine.HttpClientEngine

/** Platform-provided Ktor engine (OkHttp on Android, Darwin on iOS). */
expect fun platformHttpEngine(): HttpClientEngine

/** Per-platform gateway base URL (dev emulator loopback differs by platform). */
expect object PlatformConfig {
    val baseUrl: String
    val wsBaseUrl: String
}
