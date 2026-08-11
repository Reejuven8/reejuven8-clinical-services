package com.reejuven8.ninemo.android.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.ErrorView
import com.reejuven8.ninemo.android.ui.components.LoadingSpinner
import com.reejuven8.ninemo.shared.model.DietFoodSafetyResponse
import com.reejuven8.ninemo.shared.model.SafetyRating
import com.reejuven8.ninemo.shared.viewmodel.DietViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import org.koin.compose.viewmodel.koinViewModel

private val SafeBg = Color(0xFFD3EEDD)
private val SafeFg = Color(0xFF173F2A)
private val CautionBg = Color(0xFFFFDEAD)
private val CautionFg = Color(0xFF5C3700)
private val AvoidBg = Color(0xFFFFDAD6)
private val AvoidFg = Color(0xFF93000A)

/** P15 — "Is it safe?". safetyRating always server-supplied; client never classifies ingredients itself. */
@Composable
fun DietScreen(onBack: () -> Unit) {
    val vm: DietViewModel = koinViewModel()
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val recent by vm.recentSearches.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        Text("Is it safe?", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = query,
            onValueChange = vm::setQuery,
            placeholder = { Text("Search a food — Hindi works too, e.g. पनीर") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            shape = MaterialTheme.shapes.extraLarge,
        )
        androidx.compose.material3.TextButton(onClick = vm::search, modifier = Modifier.padding(top = 4.dp)) {
            Text("Search")
        }

        when (val state = results) {
            is UiState.Loading -> LoadingSpinner()
            is UiState.Error -> ErrorView(state.throwable, onRetry = vm::search)
            is UiState.Empty -> if (query.isNotBlank()) {
                Text("No matches found.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
            }
            is UiState.Success -> Column(Modifier.padding(top = 8.dp)) {
                state.data.forEach { item -> ResultCard(item) }
            }
        }

        if (recent.isNotEmpty()) {
            Text(
                "Recent searches",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
            )
            recent.forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(item.ingredientName, style = MaterialTheme.typography.bodyMedium)
                    RatingPill(item.safetyRating)
                }
            }
        }
    }
}

@Composable
private fun ResultCard(item: DietFoodSafetyResponse) {
    val (bg, fg) = when (item.safetyRating) {
        SafetyRating.SAFE -> SafeBg to SafeFg
        SafetyRating.CAUTION -> CautionBg to CautionFg
        SafetyRating.AVOID -> AvoidBg to AvoidFg
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(bg, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text(item.safetyRating.name, style = MaterialTheme.typography.labelLarge, color = fg)
        Text(
            item.ingredientNameHindi?.let { "${item.ingredientName} ($it)" } ?: item.ingredientName,
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Serif,
            color = fg,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(item.medicalReasoning, style = MaterialTheme.typography.bodyMedium, color = fg, modifier = Modifier.padding(top = 6.dp))
        item.safeQuantity?.let {
            Text("Safe quantity: $it", style = MaterialTheme.typography.labelMedium, color = fg, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun RatingPill(rating: SafetyRating) {
    val (bg, fg) = when (rating) {
        SafetyRating.SAFE -> SafeBg to SafeFg
        SafetyRating.CAUTION -> CautionBg to CautionFg
        SafetyRating.AVOID -> AvoidBg to AvoidFg
    }
    Text(
        rating.name,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        modifier = Modifier.background(bg, RoundedCornerShape(100.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
