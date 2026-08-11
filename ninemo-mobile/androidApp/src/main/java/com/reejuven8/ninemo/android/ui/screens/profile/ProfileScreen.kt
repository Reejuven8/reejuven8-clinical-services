package com.reejuven8.ninemo.android.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.theme.Berry
import com.reejuven8.ninemo.android.ui.theme.SeverityCritical
import com.reejuven8.ninemo.shared.viewmodel.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

/** P22 — Profile & Settings. Identity from session; rows route to ABHA / consent; logout. */
@Composable
fun ProfileScreen(
    onAbhaLink: () -> Unit,
    onConsents: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val vm: ProfileViewModel = koinViewModel()
    val userId by vm.userId.collectAsStateWithLifecycle()
    val role by vm.role.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall)

        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Berry)
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(role?.name ?: "Patient", style = MaterialTheme.typography.titleMedium)
                Text(
                    "ID ${userId?.take(8) ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
            SettingRow(Icons.Filled.Link, "Link ABHA", onAbhaLink)
            SettingRow(Icons.Filled.HealthAndSafety, "Consents & sharing", onConsents)
            SettingRow(Icons.Filled.VerifiedUser, "Privacy", onConsents)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .background(SeverityCritical.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                .clickable { vm.logout(); onLoggedOut() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = SeverityCritical)
            Text(
                "Log out",
                style = MaterialTheme.typography.bodyLarge,
                color = SeverityCritical,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Berry)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 14.dp).weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
