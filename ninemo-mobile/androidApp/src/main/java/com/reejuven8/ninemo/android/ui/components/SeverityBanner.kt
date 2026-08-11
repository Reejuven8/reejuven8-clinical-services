package com.reejuven8.ninemo.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.reejuven8.ninemo.android.ui.theme.SeverityCritical
import com.reejuven8.ninemo.android.ui.theme.SeverityCriticalBg
import com.reejuven8.ninemo.android.ui.theme.SeverityNormal
import com.reejuven8.ninemo.android.ui.theme.SeverityWarning
import com.reejuven8.ninemo.android.ui.theme.SeverityWarningBg
import com.reejuven8.ninemo.shared.model.SeverityFlag

/**
 * Severity is always encoded with icon + text, never color alone (a11y rule).
 * Identical semantics on symptoms, vitals, kicks, contractions, growth.
 */
@Composable
fun SeverityBanner(
    flag: SeverityFlag,
    title: String,
    lines: List<String>,
    modifier: Modifier = Modifier,
) {
    val (fg, bg, icon) = when (flag) {
        SeverityFlag.NORMAL -> Triple(SeverityNormal, SeverityNormal.copy(alpha = 0.10f), Icons.Default.CheckCircle)
        SeverityFlag.WARNING -> Triple(SeverityWarning, SeverityWarningBg, Icons.Default.Warning)
        SeverityFlag.CRITICAL -> Triple(SeverityCritical, SeverityCriticalBg, Icons.Default.Error)
    }
    SeverityBannerContent(fg, bg, icon, title, lines, modifier)
}

@Composable
private fun SeverityBannerContent(
    fg: Color,
    bg: Color,
    icon: ImageVector,
    title: String,
    lines: List<String>,
    modifier: Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = title, tint = fg)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, color = fg, style = MaterialTheme.typography.titleMedium)
            lines.forEach {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
