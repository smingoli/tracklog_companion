package com.smingoli.tracklogcompanion.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CatalogController(context: Context) {
    val storage = CatalogStorage(context.applicationContext)
    var state: CatalogUiState by mutableStateOf(CatalogUiState.Loading)
        private set

    suspend fun initialise() {
        state = CatalogUiState.Loading
        state = withContext(Dispatchers.IO) {
            runCatching { storage.openSaved() }
                .fold(
                    onSuccess = { snapshot -> snapshot?.let(CatalogUiState::Ready) ?: CatalogUiState.NeedsFolder },
                    onFailure = { CatalogUiState.Error(it.userMessage()) },
                )
        }
    }

    suspend fun selectFolder(context: Context, uri: Uri) {
        state = CatalogUiState.Loading
        state = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                storage.import(uri)
            }.fold(
                onSuccess = CatalogUiState::Ready,
                onFailure = { CatalogUiState.Error(it.userMessage()) },
            )
        }
    }

    suspend fun refresh() {
        val uri = storage.savedTreeUri()
        if (uri == null) {
            state = CatalogUiState.NeedsFolder
            return
        }
        state = CatalogUiState.Loading
        state = withContext(Dispatchers.IO) {
            runCatching { storage.import(uri) }.fold(
                onSuccess = CatalogUiState::Ready,
                onFailure = { CatalogUiState.Error(it.userMessage()) },
            )
        }
    }

    suspend fun importZip(context: Context, zipUri: Uri, destinationTreeUri: Uri) {
        state = CatalogUiState.Loading
        state = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    destinationTreeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                storage.importZip(zipUri, destinationTreeUri)
            }.fold(
                onSuccess = CatalogUiState::Ready,
                onFailure = { CatalogUiState.Error(it.userMessage()) },
            )
        }
    }

    private fun Throwable.userMessage(): String =
        message?.takeIf(String::isNotBlank) ?: "The catalogue could not be opened"
}
