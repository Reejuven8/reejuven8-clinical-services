package com.reejuven8.ninemo.android

import android.app.Application
import com.reejuven8.ninemo.shared.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class NineMoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@NineMoApplication)
        }
    }
}
