package com.reejuven8.ninemo.android.ui.screens.community

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.EmptyView
import com.reejuven8.ninemo.android.ui.components.ErrorView
import com.reejuven8.ninemo.android.ui.components.LoadingSpinner
import com.reejuven8.ninemo.android.ui.theme.Berry
import com.reejuven8.ninemo.shared.model.ContentArticle
import com.reejuven8.ninemo.shared.viewmodel.ContentViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import org.koin.compose.viewmodel.koinViewModel

/** P21 — Content feed. Published articles with a client-side category filter. */
@Composable
fun ContentFeedScreen(onBack: () -> Unit) {
    val vm: ContentViewModel = koinViewModel()
    val articlesState by vm.articles.collectAsStateWithLifecycle()
    val selectedCategory by vm.selectedCategory.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Articles", style = MaterialTheme.typography.headlineSmall)
        }

        when (val state = articlesState) {
            is UiState.Loading -> LoadingSpinner()
            is UiState.Error -> ErrorView(state.throwable, onRetry = vm::load)
            is UiState.Empty -> EmptyView("No articles published yet.")
            is UiState.Success -> {
                val categories = vm.categoriesFrom(state.data)
                if (categories.isNotEmpty()) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CategoryChip("All", selectedCategory == null) { vm.setCategory(null) }
                        categories.forEach { c -> CategoryChip(c, selectedCategory == c) { vm.setCategory(c) } }
                    }
                }
                val filtered = state.data.filter { selectedCategory == null || it.category == selectedCategory }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    filtered.forEach { ArticleCard(it) }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun ArticleCard(article: ContentArticle) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded }
            .padding(16.dp)
            .animateContentSize(),
    ) {
        article.category?.let {
            Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = Berry)
        }
        Text(article.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 2.dp))
        val preview = article.summary ?: article.body
        Text(
            if (expanded) article.body else preview,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            modifier = Modifier.padding(top = 6.dp),
        )
        article.author?.let {
            Text(
                "by $it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
