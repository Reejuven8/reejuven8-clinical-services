package com.reejuven8.ninemo.android.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reejuven8.ninemo.android.ui.theme.Berry
import kotlinx.coroutines.delay

private val SplashTop = Color(0xFFFDF7FB)
private val SplashBottom = Color(0xFFFFD8EA)
private val Wordmark = Color(0xFF3E0A2A)
private val Tagline = Color(0xFF6D3452)
private val Restoring = Color(0xFF837179)

/** P0 — decides routing (Login vs Onboarding vs Main shell handled by the nav gate above us). */
@Composable
fun SplashScreen(
    isAuthenticated: Boolean,
    hasCompletedOnboarding: Boolean,
    onNavigateToLogin: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
) {
    LaunchedEffect(isAuthenticated, hasCompletedOnboarding) {
        delay(400) // minimum splash time so the brand mark isn't a single-frame flash
        when {
            !isAuthenticated -> onNavigateToLogin()
            !hasCompletedOnboarding -> onNavigateToOnboarding()
            // authenticated + onboarded: top-level NineMoNavHost gate swaps us for MainShell.
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SplashTop, SplashBottom), endY = 1300f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(88.dp).background(Berry, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(46.dp))
            }
            Text(
                "NineMo",
                fontFamily = FontFamily.Serif,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = Wordmark,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                "Nine months, and every one after",
                style = MaterialTheme.typography.bodyMedium,
                color = Tagline,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(Modifier.padding(top = 44.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(Berry.copy(alpha = 1f - i * 0.375f), RoundedCornerShape(50)),
                    )
                }
            }
            Text(
                "Restoring your session…",
                style = MaterialTheme.typography.labelSmall,
                color = Restoring,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
