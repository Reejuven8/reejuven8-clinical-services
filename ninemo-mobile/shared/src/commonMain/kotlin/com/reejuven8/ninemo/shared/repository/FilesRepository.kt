package com.reejuven8.ninemo.shared.repository

import com.reejuven8.ninemo.shared.model.ApiResponse
import com.reejuven8.ninemo.shared.model.FileUploadResponse
import com.reejuven8.ninemo.shared.network.ApiRoutes
import com.reejuven8.ninemo.shared.network.apiUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Ktor 3.0.3 (this project's pin) has no official SSE client plugin — hand-rolled minimal
 * reader instead of bumping the whole client version under time pressure. The real backend
 * stream is simple enough to justify this: at most two `event: parse-progress` / bare-string
 * `data:` frames (PROCESSING then PARSED), then the server closes the connection.
 */
class FilesRepository(private val client: HttpClient) {

    suspend fun upload(fileName: String, contentType: String, bytes: ByteArray): Result<FileUploadResponse> = runCatching {
        client.submitFormWithBinaryData(
            url = apiUrl(ApiRoutes.FILE_UPLOAD),
            formData = formData {
                append("file", bytes, Headers.build {
                    append(HttpHeaders.ContentType, contentType)
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            },
        ).body<ApiResponse<FileUploadResponse>>().data ?: error("Missing upload response")
    }

    suspend fun downloadUrl(s3Key: String): Result<String> = runCatching {
        client.get(apiUrl(ApiRoutes.FILE_DOWNLOAD)) {
            parameter("s3Key", s3Key)
        }.body<ApiResponse<String>>().data ?: error("Missing presigned URL")
    }

    /** Emits each `data:` line under the `parse-progress` event (e.g. "PROCESSING", "PARSED"). */
    fun parseStatusEvents(s3Key: String): Flow<String> = flow {
        client.prepareGet(apiUrl(ApiRoutes.FILE_EVENTS)) {
            parameter("s3Key", s3Key)
            // AI parsing can outlast the client's default 15s request timeout — this stream
            // is expected to stay open until the server sends PARSED and closes it.
            timeout { requestTimeoutMillis = 10 * 60 * 1000L } // 10 minutes
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (line.startsWith("data:")) {
                    emit(line.removePrefix("data:").trim())
                }
            }
        }
    }
}
