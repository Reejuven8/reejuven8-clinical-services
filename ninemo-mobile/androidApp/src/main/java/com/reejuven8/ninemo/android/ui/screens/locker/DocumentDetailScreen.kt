package com.reejuven8.ninemo.android.ui.screens.locker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.ErrorView
import com.reejuven8.ninemo.android.ui.components.LoadingSpinner
import com.reejuven8.ninemo.android.ui.components.NineMoButton
import com.reejuven8.ninemo.android.ui.components.SecureScreen
import com.reejuven8.ninemo.android.ui.theme.Berry
import com.reejuven8.ninemo.shared.model.HealthRecordResponse
import com.reejuven8.ninemo.shared.viewmodel.DocumentDetailViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import kotlinx.serialization.json.JsonPrimitive
import org.koin.compose.viewmodel.koinViewModel

/** P8 — Document Detail. Presigned URL always fresh (15-min server TTL, see FileUploadResponse). */
@Composable
fun DocumentDetailScreen(recordId: String, onBack: () -> Unit) {
    SecureScreen()
    val vm: DocumentDetailViewModel = koinViewModel()
    val recordState by vm.record.collectAsStateWithLifecycle()
    val presignedUrl by vm.presignedUrl.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(recordId) { vm.load(recordId) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(
                (recordState as? UiState.Success)?.data?.resourceType ?: "Document",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Row(
                Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(100.dp)).padding(10.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Berry)
                Text("Secured", style = MaterialTheme.typography.labelSmall, color = Berry)
            }
        }

        when (val state = recordState) {
            is UiState.Loading -> LoadingSpinner()
            is UiState.Error -> ErrorView(state.throwable, onRetry = { vm.load(recordId) })
            is UiState.Empty -> {}
            is UiState.Success -> DetailContent(state.data, presignedUrl, onViewFile = { presignedUrl?.let(uriHandler::openUri) })
        }
    }
}

@Composable
private fun DetailContent(record: HealthRecordResponse, presignedUrl: String?, onViewFile: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(18.dp),
        ) {
            if (presignedUrl != null) {
                NineMoButton(text = "View original file", onClick = onViewFile)
                Text(
                    "Secure link · refreshes every 15 min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Text("No attached file for this record.", style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                record.source?.let { MetaField("Source", it) }
                record.category?.let { MetaField("Category", it) }
                record.createdAt.let { MetaField("Date", it.take(10)) }
            }
        }

        Text(
            "OBSERVATIONS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )
        val codeDisplay = record.code.jsonString("display") ?: record.code.jsonString("text")
        val codeValue = record.code.jsonString("code")
        val quantityValue = record.valueQuantity.jsonString("value")
        val quantityUnit = record.valueQuantity.jsonString("unit")
        if (codeDisplay == null && quantityValue == null) {
            Text("No structured observations parsed for this document yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(codeDisplay ?: "Observation", style = MaterialTheme.typography.bodyLarge)
                    codeValue?.let { Text("Code $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                if (quantityValue != null) {
                    Text(
                        listOfNotNull(quantityValue, quantityUnit).joinToString(" "),
                        style = MaterialTheme.typography.titleMedium,
                        color = Berry,
                    )
                }
            }
        }

        record.notes?.let {
            Text(
                "NOTES",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MetaField(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun Map<String, kotlinx.serialization.json.JsonElement>.jsonString(key: String): String? =
    (this[key] as? JsonPrimitive)?.content
