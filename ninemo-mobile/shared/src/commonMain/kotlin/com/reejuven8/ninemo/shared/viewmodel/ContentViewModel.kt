package com.reejuven8.ninemo.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reejuven8.ninemo.shared.model.ContentArticle
import com.reejuven8.ninemo.shared.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** P21 — Content feed. Published articles, optional client-side category filter. */
class ContentViewModel(private val repository: ContentRepository) : ViewModel() {

    private val _articles = MutableStateFlow<UiState<List<ContentArticle>>>(UiState.Loading)
    val articles: StateFlow<UiState<List<ContentArticle>>> = _articles.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null) // null = All
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _articles.value = UiState.Loading
            repository.listPublished().fold(
                onSuccess = { _articles.value = if (it.isEmpty()) UiState.Empty else UiState.Success(it) },
                onFailure = { _articles.value = UiState.Error(it) },
            )
        }
    }

    fun setCategory(category: String?) { _selectedCategory.value = category }

    fun categoriesFrom(articles: List<ContentArticle>): List<String> =
        articles.mapNotNull { it.category }.distinct().sorted()
}
