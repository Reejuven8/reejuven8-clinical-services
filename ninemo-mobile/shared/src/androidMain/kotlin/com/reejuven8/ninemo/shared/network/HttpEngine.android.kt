package com.reejuven8.ninemo.shared.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun platformHttpEngine(): HttpClientEngine = OkHttp.create()

// Emulator reaches host loopback via 10.0.2.2; a USB physical device uses 127.0.0.1
// paired with `adb reverse tcp:8080 tcp:8080` + `adb reverse tcp:8086 tcp:8086`.
// TODO(F8): drive from BuildConfig flavor (dev vs prod https://api.reejuven8.com).
actual object PlatformConfig {
    actual val baseUrl: String = "http://127.0.0.1:8080/api/v1"
    actual val wsBaseUrl: String = "ws://127.0.0.1:8086"
}
