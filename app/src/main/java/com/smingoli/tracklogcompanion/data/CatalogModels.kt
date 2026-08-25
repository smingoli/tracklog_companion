package com.smingoli.tracklogcompanion.data

import android.net.Uri

data class CatalogTotals(
    val tracks: Int,
    val availableTracks: Int,
    val releases: Int,
)

data class CatalogRelease(
    val id: Long,
    val title: String,
    val type: String,
    val status: String,
    val imagePath: String?,
    val trackCount: Int,
)

data class CatalogTrack(
    val id: Long,
    val title: String,
    val releases: List<String>,
) {
    val isAvailable: Boolean get() = releases.isEmpty()
    val membership: String
        get() = releases.joinToString().ifBlank { "Not assigned to a release" }
}

data class CatalogSnapshot(
    val totals: CatalogTotals,
    val releases: List<CatalogRelease>,
    val tracks: List<CatalogTrack>,
    val treeUri: Uri,
)

sealed interface CatalogUiState {
    data object Loading : CatalogUiState
    data object NeedsFolder : CatalogUiState
    data class Ready(val catalog: CatalogSnapshot) : CatalogUiState
    data class Error(val message: String) : CatalogUiState
}

