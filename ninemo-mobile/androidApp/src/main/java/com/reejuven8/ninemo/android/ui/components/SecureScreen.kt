package com.reejuven8.ninemo.android.ui.components

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Sets FLAG_SECURE (blocks screenshots/screen-recording/recents-thumbnail) for as long as
 * this composable is on screen — used on screens rendering real medical documents/files
 * (Health Locker, Document Detail). Cleared on dispose so it doesn't leak onto unrelated
 * screens sharing the same single-Activity window.
 */
@Composable
fun SecureScreen() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? android.app.Activity)?.window
        window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
