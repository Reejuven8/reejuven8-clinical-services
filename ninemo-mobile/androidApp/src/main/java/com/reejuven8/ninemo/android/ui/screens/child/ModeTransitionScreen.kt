package com.reejuven8.ninemo.android.ui.screens.child

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.NineMoButton
import com.reejuven8.ninemo.android.ui.theme.Berry
import com.reejuven8.ninemo.shared.viewmodel.ModeTransitionViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import org.koin.compose.viewmodel.koinViewModel

/**
 * P23 — "Baby is here!". Flips to child mode. Baby name is captured locally only (backend
 * transition takes no body — see NM-B-169). On success the shell swaps to child mode via the
 * session's activeChildId, so this screen just pops.
 */
@Composable
fun ModeTransitionScreen(onCancel: () -> Unit, onSwitched: () -> Unit) {
    val vm: ModeTransitionViewModel = koinViewModel()
    val pregnancyMissing by vm.pregnancyMissing.collectAsStateWithLifecycle()
    val transitionState by vm.transitionState.collectAsStateWithLifecycle()
    var babyName by remember { mutableStateOf("") }

    LaunchedEffect(transitionState) {
        if (transitionState is UiState.Success) onSwitched()
    }

    Box(Modifier.fillMaxSize().background(Color(0x73211A1E)), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .verticalScroll(rememberScrollState())
                .padding(24.dp, 16.dp, 24.dp, 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(36.dp, 4.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)))
            Box(
                Modifier.padding(top = 20.dp).size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ChildCare, contentDescription = null, tint = Berry, modifier = Modifier.size(32.dp))
            }
            Text("Baby is here!", style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Serif, modifier = Modifier.padding(top = 14.dp))
            Text(
                "Congratulations! A few details to switch NineMo to Child mode.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )

            OutlinedTextField(
                value = babyName,
                onValueChange = { babyName = it },
                label = { Text("Baby's name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                shape = MaterialTheme.shapes.medium,
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Text("WHAT CHANGES", style = MaterialTheme.typography.labelSmall, color = Berry)
                listOf(
                    "Pregnancy timeline is locked (still viewable)",
                    "Home becomes your baby's dashboard",
                    "Vaccination schedule is created automatically",
                ).forEach {
                    Text("•  $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                }
            }

            if (pregnancyMissing) {
                Text(
                    "This becomes available once your pregnancy profile is set up (backend NM-B-167).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
            if (transitionState is UiState.Error) {
                Text(
                    (transitionState as UiState.Error).throwable.message ?: "Couldn't switch modes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NineMoButton(text = "Not yet", onClick = onCancel, modifier = Modifier.weight(1f))
                NineMoButton(
                    text = "Switch to Child mode",
                    onClick = { vm.switchToChildMode(babyName) },
                    loading = transitionState is UiState.Loading,
                    enabled = !pregnancyMissing,
                    modifier = Modifier.weight(1.4f),
                )
            }
        }
    }
}
