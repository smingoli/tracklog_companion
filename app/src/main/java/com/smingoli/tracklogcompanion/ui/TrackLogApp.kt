package com.smingoli.tracklogcompanion.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smingoli.tracklogcompanion.data.*
import com.smingoli.tracklogcompanion.ui.theme.TrackLogTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Destination(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Outlined.Home),
    Releases("Releases", Icons.Outlined.Album),
    Tracks("Tracks", Icons.Outlined.MusicNote),
}

@Composable
fun TrackLogApp() {
    val context = LocalContext.current
    val controller = remember { CatalogController(context) }
    val scope = rememberCoroutineScope()
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch { controller.selectFolder(context, uri) }
    }

    LaunchedEffect(controller) { controller.initialise() }

    when (val state = controller.state) {
        CatalogUiState.Loading -> LoadingScreen()
        CatalogUiState.NeedsFolder -> ConnectCatalogScreen { folderPicker.launch(null) }
        is CatalogUiState.Error -> CatalogErrorScreen(
            message = state.message,
            onChooseFolder = { folderPicker.launch(null) },
            onTryAgain = { scope.launch { controller.initialise() } },
        )
        is CatalogUiState.Ready -> CatalogApp(
            catalog = state.catalog,
            storage = controller.storage,
            onChangeFolder = { folderPicker.launch(state.catalog.treeUri) },
            onRefresh = { scope.launch { controller.refresh() } },
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(18.dp))
            Text("Opening your catalogue…")
        }
    }
}

@Composable
private fun ConnectCatalogScreen(onSelectFolder: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Icon(Icons.Outlined.FolderOpen, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Connect your catalogue", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Select the TrackLog folder copied to this Android device. Access is remembered and catalog.db is always opened read-only.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text("TrackLog/\n  catalog.db\n  images/releases/", Modifier.padding(18.dp))
            }
            Button(onClick = onSelectFolder, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.FolderOpen, null)
                Spacer(Modifier.size(10.dp))
                Text("Select TrackLog folder")
            }
            Text(
                "Your catalogue remains on this device and is never modified.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CatalogErrorScreen(message: String, onChooseFolder: () -> Unit, onTryAgain: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Catalogue could not be opened", style = MaterialTheme.typography.headlineLarge)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onChooseFolder, modifier = Modifier.fillMaxWidth()) { Text("Choose another folder") }
            OutlinedButton(onClick = onTryAgain, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogApp(
    catalog: CatalogSnapshot,
    storage: CatalogStorage,
    onChangeFolder: () -> Unit,
    onRefresh: () -> Unit,
) {
    var destination by remember { mutableStateOf(Destination.Home) }
    var settingsOpen by remember { mutableStateOf(false) }
    var availableOnly by remember { mutableStateOf(false) }

    if (settingsOpen) {
        SettingsScreen(catalog, { settingsOpen = false }, onChangeFolder, onRefresh)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TrackLog", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Outlined.Search, "Search") }
                    IconButton(onClick = { settingsOpen = true }) { Icon(Icons.Outlined.Settings, "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (destination) {
            Destination.Home -> HomeScreen(
                catalog,
                Modifier.padding(padding),
                onTracks = { availableOnly = false; destination = Destination.Tracks },
                onAvailable = { availableOnly = true; destination = Destination.Tracks },
                onReleases = { destination = Destination.Releases },
            )
            Destination.Releases -> ReleasesScreen(catalog.releases, catalog.treeUri, storage, Modifier.padding(padding))
            Destination.Tracks -> TracksScreen(
                catalog.tracks,
                availableOnly,
                { availableOnly = it },
                Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun HomeScreen(
    catalog: CatalogSnapshot,
    modifier: Modifier = Modifier,
    onTracks: () -> Unit,
    onAvailable: () -> Unit,
    onReleases: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Your music, at a glance", style = MaterialTheme.typography.headlineLarge)
            Text("Read-only companion for your TrackLog catalog", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(catalog.totals.tracks.toString(), "Tracks", Modifier.weight(1f), onTracks)
            MetricCard(catalog.totals.availableTracks.toString(), "Available", Modifier.weight(1f), onAvailable)
            MetricCard(catalog.totals.releases.toString(), "Releases", Modifier.weight(1f), onReleases)
        }
        Text(
            "Available tracks are not assigned to a release.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (catalog.totals.tracks == 0 && catalog.totals.releases == 0) {
            Text("This catalogue is empty. Add music in TrackLog Desktop, copy the updated folder, then refresh here.")
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 18.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReleasesScreen(
    releases: List<CatalogRelease>,
    treeUri: Uri,
    storage: CatalogStorage,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf("All") }
    val filtered = releases.filter {
        filter == "All" || it.type.equals(filter.removeSuffix("s"), ignoreCase = true)
    }
    Column(modifier = modifier.fillMaxSize().padding(top = 16.dp)) {
        Text("Releases", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 20.dp))
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("All", "Albums", "EPs", "Singles").forEach { label ->
                FilterChip(filter == label, { filter = label }, { Text(label) })
            }
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No releases found") }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(filtered, key = { it.id }) { ReleaseCard(it, treeUri, storage) }
            }
        }
    }
}

@Composable
private fun ReleaseCard(release: CatalogRelease, treeUri: Uri, storage: CatalogStorage) {
    Column(modifier = Modifier.clickable { }) {
        ReleaseArtwork(release, treeUri, storage, Modifier.fillMaxWidth().aspectRatio(1f))
        Spacer(Modifier.height(10.dp))
        Text(release.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            "${release.type} · ${release.trackCount} tracks",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(release.status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ReleaseArtwork(release: CatalogRelease, treeUri: Uri, storage: CatalogStorage, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, release.imagePath, treeUri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val uri = storage.findArtwork(treeUri, release.imagePath) ?: return@runCatching null
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
    }
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(image, release.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Outlined.LibraryMusic, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TracksScreen(
    tracks: List<CatalogTrack>,
    availableOnly: Boolean,
    onAvailableOnlyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filtered = if (availableOnly) tracks.filter(CatalogTrack::isAvailable) else tracks
    Column(modifier = modifier.fillMaxSize()) {
        Text("Tracks", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 0.dp))
        Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!availableOnly, { onAvailableOnlyChange(false) }, { Text("All") })
            FilterChip(availableOnly, { onAvailableOnlyChange(true) }, { Text("Available") })
        }
        Text(
            "${filtered.size} tracks",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            items(filtered, key = { it.id }) { track ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { }.padding(horizontal = 20.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(track.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            track.membership,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(Modifier.padding(start = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    catalog: CatalogSnapshot,
    onBack: () -> Unit,
    onChangeFolder: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item {
                SettingsSection("TrackLog folder") {
                    Text(catalog.treeUri.lastPathSegment ?: catalog.treeUri.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onChangeFolder) { Text("Change folder") }
                }
            }
            item {
                SettingsSection("Catalogue") {
                    Text("catalog.db · Ready", color = MaterialTheme.colorScheme.primary)
                    Text("${catalog.totals.releases} releases · ${catalog.totals.tracks} tracks")
                    Button(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, null)
                        Spacer(Modifier.size(8.dp))
                        Text("Refresh catalogue")
                    }
                }
            }
            item {
                SettingsSection("About") {
                    Text("TrackLog Companion")
                    Text("Catalogue access is read-only.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        content()
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ConnectPreview() {
    TrackLogTheme { ConnectCatalogScreen {} }
}
