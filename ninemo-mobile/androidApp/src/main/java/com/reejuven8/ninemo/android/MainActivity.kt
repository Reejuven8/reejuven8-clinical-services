package com.reejuven8.ninemo.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.reejuven8.ninemo.android.navigation.NineMoNavHost
import com.reejuven8.ninemo.android.ui.theme.NineMoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NineMoTheme {
                NineMoNavHost()
            }
        }
    }
}
