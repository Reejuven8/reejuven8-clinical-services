package com.reejuven8.ninemo.android.ui.screens.locker

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.ErrorView
import com.reejuven8.ninemo.android.ui.components.LoadingSpinner
import com.reejuven8.ninemo.android.ui.components.NineMoButton
import com.reejuven8.ninemo.android.ui.components.SecureScreen
import com.reejuven8.ninemo.android.ui.theme.Berry
import com.reejuven8.ninemo.android.ui.theme.SeverityNormal
import com.reejuven8.ninemo.android.ui.theme.SeverityWarning
import com.reejuven8.ninemo.android.ui.theme.SeverityWarningBg
import com.reejuven8.ninemo.shared.model.HealthRecordResponse
import com.reejuven8.ninemo.shared.viewmodel.HealthLockerViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import java.io.ByteArrayOutputStream

/** P7 — Health Locker. Records/Upload tabs here; Trends/Consents lead off to their own destinations. */
@Composable
fun HealthLockerScreen(onOpenRecord: (String) -> Unit, onOpenConsents: () -> Unit) {
    SecureScreen()
    val vm: HealthLockerViewModel = koinViewModel()
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Records", "Upload", "Trends", "Consents")

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp, 14.dp, 20.dp, 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Health Locker", style = MaterialTheme.typography.headlineSmall)
            Row(
                Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(100.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Berry, modifier = Modifier.size(13.dp))
                Text("Screen secured", style = MaterialTheme.typography.labelSmall, color = Berry, modifier = Modifier.padding(start = 5.dp))
            }
        }

        TabRow(selectedTabIndex = tabIndex, modifier = Modifier.padding(top = 10.dp)) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = tabIndex == i,
                    onClick = { if (title == "Consents") onOpenConsents() else tabIndex = i },
                    text = { Text(title) },
                )
            }
        }

        when (tabIndex) {
            0 -> RecordsTab(vm, onOpenRecord)
            1 -> UploadTab(vm)
            else -> Box(Modifier.fillMaxSize().padding(20.dp)) {
                Text(
                    "Trends are coming soon — track weight and BP trends from Tools > Vitals for now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecordsTab(vm: HealthLockerViewModel, onOpenRecord: (String) -> Unit) {
    val recordsState by vm.records.collectAsStateWithLifecycle()
    val selectedCategory by vm.selectedCategory.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        when (val state = recordsState) {
            is UiState.Loading -> LoadingSpinner()
            is UiState.Error -> ErrorView(state.throwable, onRetry = vm::loadRecords)
            is UiState.Empty -> Text(
                "No records yet — upload a document to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
            is UiState.Success -> {
                val categories = vm.categoriesFrom(state.data)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChipItem("All", selectedCategory == null) { vm.setCategory(null) }
                    categories.forEach { cat -> FilterChipItem(cat, selectedCategory == cat) { vm.setCategory(cat) } }
                }
                val filtered = state.data.filter { selectedCategory == null || it.category == selectedCategory }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    filtered.forEach { record -> RecordCard(record, onClick = { onOpenRecord(record.id) }) }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(if (selected) Berry else MaterialTheme.colorScheme.surface, RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
private fun RecordCard(record: HealthRecordResponse, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Description, contentDescription = null, tint = Berry)
        }
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(record.resourceType, style = MaterialTheme.typography.bodyLarge)
            Text(
                listOfNotNull(record.category, record.source).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UploadTab(vm: HealthLockerViewModel) {
    val uploadState by vm.uploadState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val resolver = context.contentResolver
            val contentType = resolver.getType(uri) ?: "application/octet-stream"
            val fileName = uri.lastPathSegment ?: "upload"
            val bytes = resolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArrayOutputStream()
                input.copyTo(buffer)
                buffer.toByteArray()
            } ?: return@launch
            vm.upload(fileName, contentType, bytes)
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            "Upload a lab report, prescription, or scan. Parsing runs automatically — you'll see it move from PROCESSING to PARSED.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NineMoButton(
            text = "Choose a file",
            onClick = { launcher.launch("*/*") },
            loading = uploadState is UiState.Loading,
            modifier = Modifier.padding(top = 16.dp),
        )

        when (val state = uploadState) {
            is UiState.Success -> {
                val (bg, fg) = if (state.data == "PARSED") SeverityNormal.copy(alpha = 0.12f) to SeverityNormal else SeverityWarningBg to SeverityWarning
                Text(
                    state.data,
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .background(bg, RoundedCornerShape(100.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
            is UiState.Error -> Text(
                state.throwable.message ?: "Upload failed.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            else -> Unit
        }
    }
}
