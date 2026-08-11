package com.reejuven8.ninemo.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun NineMoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) NineMoDarkColors else NineMoLightColors,
        typography = NineMoTypography,
        shapes = NineMoShapes,
        content = content,
    )
}
