package com.reejuven8.ninemo.shared.di

import com.reejuven8.ninemo.shared.network.platformHttpEngine
import com.reejuven8.ninemo.shared.session.IosSessionStore
import com.reejuven8.ninemo.shared.session.SessionStore
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<HttpClientEngine> { platformHttpEngine() }
    single<SessionStore> { IosSessionStore() }
}
