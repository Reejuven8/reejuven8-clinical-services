package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.ContentArticle
import com.reejuven8.ninemo.shared.model.SpringPage
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/** P21 Content feed. Published articles are paginated; category/week filters return plain lists. */
class ContentRepository(private val client: HttpClient) {

    suspend fun listPublished(page: Int = 0, size: Int = 20): Result<List<ContentArticle>> = runCatching {
        client.get(apiUrl(ApiRoutes.CONTENT)) {
            parameter("page", page)
            parameter("size", size)
        }.body<ApiResponse<SpringPage<ContentArticle>>>().data?.content ?: emptyList()
    }

    suspend fun byCategory(category: String): Result<List<ContentArticle>> = runCatching {
        client.get(apiUrl("${ApiRoutes.CONTENT}/category/$category"))
            .body<ApiResponse<List<ContentArticle>>>().data ?: emptyList()
    }

    suspend fun byWeek(gestationalWeek: Int): Result<List<ContentArticle>> = runCatching {
        client.get(apiUrl("${ApiRoutes.CONTENT}/week/$gestationalWeek"))
            .body<ApiResponse<List<ContentArticle>>>().data ?: emptyList()
    }
}
