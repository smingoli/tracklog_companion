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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    var pendingZipUri by remember { mutableStateOf<Uri?>(null) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch { controller.selectFolder(context, uri) }
    }
    val importDestinationPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val zipUri = pendingZipUri
        pendingZipUri = null
        if (uri != null && zipUri != null) {
            scope.launch { controller.importZip(context, zipUri, uri) }
        }
    }
    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingZipUri = uri
            importDestinationPicker.launch(null)
        }
    }
    val startZipImport = {
        zipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
    }

    LaunchedEffect(controller) { controller.initialise() }

    when (val state = controller.state) {
        CatalogUiState.Loading -> LoadingScreen()
        CatalogUiState.NeedsFolder -> ConnectCatalogScreen(
            onSelectFolder = { folderPicker.launch(null) },
            onImportZip = startZipImport,
        )
        is CatalogUiState.Error -> CatalogErrorScreen(
            message = state.message,
            onChooseFolder = { folderPicker.launch(null) },
            onImportZip = startZipImport,
            onTryAgain = { scope.launch { controller.initialise() } },
        )
        is CatalogUiState.Ready -> CatalogApp(
            catalog = state.catalog,
            storage = controller.storage,
            onChangeFolder = { folderPicker.launch(state.catalog.treeUri) },
            onImportZip = startZipImport,
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
private fun ConnectCatalogScreen(onSelectFolder: () -> Unit, onImportZip: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
    ) {
        item {
            Icon(Icons.Outlined.FolderOpen, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
        }
        item {
            Text("Connect your catalogue", style = MaterialTheme.typography.headlineLarge)
        }
        item {
            Text(
                "Select the TrackLog folder copied to this Android device. Access is remembered and catalog.db is always opened read-only.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text("TrackLog/\n  catalog.db\n  images/releases/", Modifier.padding(18.dp))
            }
        }
        item {
            Button(onClick = onSelectFolder, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.FolderOpen, null)
                Spacer(Modifier.size(10.dp))
                Text("Select TrackLog folder")
            }
        }
        item {
            OutlinedButton(onClick = onImportZip, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Archive, null)
                Spacer(Modifier.size(10.dp))
                Text("Import TrackLog ZIP")
            }
        }
        item {
            Text(
                "Imported files are written only to the folder you choose. Catalogue browsing remains read-only.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CatalogErrorScreen(
    message: String,
    onChooseFolder: () -> Unit,
    onImportZip: () -> Unit,
    onTryAgain: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        item {
            Text("Catalogue could not be opened", style = MaterialTheme.typography.headlineLarge)
        }
        item {
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Button(onClick = onChooseFolder, modifier = Modifier.fillMaxWidth()) { Text("Choose another folder") }
        }
        item {
            OutlinedButton(onClick = onImportZip, modifier = Modifier.fillMaxWidth()) { Text("Import TrackLog ZIP") }
        }
        item {
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
    onImportZip: () -> Unit,
    onRefresh: () -> Unit,
) {
    var destinationName by rememberSaveable { mutableStateOf(Destination.Home.name) }
    val destination = Destination.valueOf(destinationName)
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var availableOnly by rememberSaveable { mutableStateOf(false) }
    var selectedReleaseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedTrackId by rememberSaveable { mutableStateOf<Long?>(null) }

    if (settingsOpen) {
        SettingsScreen(catalog, { settingsOpen = false }, onChangeFolder, onImportZip, onRefresh)
        return
    }

    if (searchOpen) {
        SearchScreen(
            catalog = catalog,
            storage = storage,
            onBack = { searchOpen = false },
            onRelease = {
                searchOpen = false
                selectedReleaseId = it
            },
            onTrack = {
                searchOpen = false
                selectedTrackId = it
            },
        )
        return
    }

    selectedTrackId?.let { trackId ->
        val track = catalog.tracks.firstOrNull { it.id == trackId }
        if (track != null) {
            TrackDetailScreen(
                track = track,
                catalog = catalog,
                storage = storage,
                onBack = { selectedTrackId = null },
                onRelease = { releaseId ->
                    selectedTrackId = null
                    selectedReleaseId = releaseId
                },
            )
            return
        }
    }

    selectedReleaseId?.let { releaseId ->
        val release = catalog.releases.firstOrNull { it.id == releaseId }
        if (release != null) {
            ReleaseDetailScreen(
                release = release,
                catalog = catalog,
                storage = storage,
                onBack = { selectedReleaseId = null },
                onTrack = { selectedTrackId = it },
            )
            return
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp
        Row(Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                NavigationRail {
                    Spacer(Modifier.height(8.dp))
                    Destination.entries.forEach { item ->
                        NavigationRailItem(
                            selected = destination == item,
                            onClick = { destinationName = item.name },
                            icon = { Icon(item.icon, null) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
            CatalogScaffold(
                modifier = Modifier.weight(1f),
                destination = destination,
                useBottomBar = !useNavigationRail,
                catalog = catalog,
                storage = storage,
                availableOnly = availableOnly,
                onDestination = { destinationName = it.name },
                onAvailableOnly = { availableOnly = it },
                onSearch = { searchOpen = true },
                onSettings = { settingsOpen = true },
                onRelease = { selectedReleaseId = it },
                onTrack = { selectedTrackId = it },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogScaffold(
    modifier: Modifier,
    destination: Destination,
    useBottomBar: Boolean,
    catalog: CatalogSnapshot,
    storage: CatalogStorage,
    availableOnly: Boolean,
    onDestination: (Destination) -> Unit,
    onAvailableOnly: (Boolean) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onRelease: (Long) -> Unit,
    onTrack: (Long) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (destination == Destination.Home) "TrackLog" else "TrackLog - ${destination.label}",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, "Search") }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (useBottomBar) {
                NavigationBar {
                    Destination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { onDestination(item) },
                            icon = { Icon(item.icon, null) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        when (destination) {
            Destination.Home -> HomeScreen(
                catalog,
                Modifier.padding(padding),
                onTracks = { onAvailableOnly(false); onDestination(Destination.Tracks) },
                onAvailable = { onAvailableOnly(true); onDestination(Destination.Tracks) },
                onReleases = { onDestination(Destination.Releases) },
            )
            Destination.Releases -> ReleasesScreen(
                catalog.releases,
                catalog.treeUri,
                storage,
                onRelease,
                Modifier.padding(padding),
            )
            Destination.Tracks -> TracksScreen(
                catalog.tracks,
                availableOnly,
                onAvailableOnly,
                onTrack,
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
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your music, at a glance", style = MaterialTheme.typography.headlineLarge)
                Text("Read-only companion for your TrackLog catalog", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(catalog.totals.tracks.toString(), "Tracks", Modifier.weight(1f), onTracks)
                MetricCard(catalog.totals.availableTracks.toString(), "Available", Modifier.weight(1f), onAvailable)
                MetricCard(catalog.totals.releases.toString(), "Releases", Modifier.weight(1f), onReleases)
            }
        }
        item {
            Text(
                "Available tracks are not assigned to a release.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (catalog.totals.tracks == 0 && catalog.totals.releases == 0) {
            item { Text("This catalogue is empty. Add music in TrackLog Desktop, copy the updated folder, then refresh here.") }
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
    onRelease: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableStateOf("All") }
    val filtered = releases.filter {
        filter == "All" || it.type.equals(filter.removeSuffix("s"), ignoreCase = true)
    }
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
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
                columns = GridCells.Adaptive(minSize = 156.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(filtered, key = { it.id }) { release ->
                    ReleaseCard(release, treeUri, storage) { onRelease(release.id) }
                }
            }
        }
    }
}

@Composable
private fun ReleaseCard(release: CatalogRelease, treeUri: Uri, storage: CatalogStorage, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
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
private fun ReleaseArtwork(
    release: CatalogRelease,
    treeUri: Uri,
    storage: CatalogStorage,
    modifier: Modifier = Modifier,
    maxDimension: Int = 768,
) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, release.imagePath, treeUri, maxDimension) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val uri = storage.findArtwork(treeUri, release.imagePath) ?: return@runCatching null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                var sampleSize = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxDimension * 2) {
                    sampleSize *= 2
                }
                val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)?.asImageBitmap()
                }
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
    onTrack: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filtered = if (availableOnly) tracks.filter(CatalogTrack::isAvailable) else tracks
    Column(modifier = modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    modifier = Modifier.fillMaxWidth().clickable { onTrack(track.id) }.padding(horizontal = 20.dp, vertical = 13.dp),
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
private fun ReleaseDetailScreen(
    release: CatalogRelease,
    catalog: CatalogSnapshot,
    storage: CatalogStorage,
    onBack: () -> Unit,
    onTrack: (Long) -> Unit,
) {
    val tracksById = remember(catalog.tracks) { catalog.tracks.associateBy { it.id } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Release") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            if (maxWidth >= 600.dp) {
                Row(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ReleaseArtwork(
                            release,
                            catalog.treeUri,
                            storage,
                            Modifier.fillMaxWidth().aspectRatio(1f),
                            maxDimension = 1400,
                        )
                        ReleaseMetadata(release)
                    }
                    VerticalDivider()
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Text(
                            "Tracks",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 8.dp),
                        )
                        ReleaseTrackList(release, tracksById, onTrack)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                ) {
                    item {
                        ReleaseArtwork(
                            release,
                            catalog.treeUri,
                            storage,
                            Modifier.fillMaxWidth().aspectRatio(1f),
                            maxDimension = 1400,
                        )
                    }
                    item {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ReleaseMetadata(release)
                            Spacer(Modifier.height(6.dp))
                            Text("Tracks", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    items(release.trackIds.size) { index ->
                        val track = tracksById[release.trackIds[index]] ?: return@items
                        ReleaseTrackRow(index, track, onTrack)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseMetadata(release: CatalogRelease) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(release.title, style = MaterialTheme.typography.headlineLarge)
        Text(
            "${release.type} · ${release.trackCount} tracks",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(release.status, color = MaterialTheme.colorScheme.primary)
        release.description?.takeIf(String::isNotBlank)?.let {
            SelectionContainer { Text(it) }
        }
    }
}

@Composable
private fun ReleaseTrackList(
    release: CatalogRelease,
    tracksById: Map<Long, CatalogTrack>,
    onTrack: (Long) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        items(release.trackIds.size) { index ->
            val track = tracksById[release.trackIds[index]] ?: return@items
            ReleaseTrackRow(index, track, onTrack)
        }
    }
}

@Composable
private fun ReleaseTrackRow(index: Int, track: CatalogTrack, onTrack: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTrack(track.id) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            (index + 1).toString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(34.dp),
        )
        Text(track.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, null)
    }
    HorizontalDivider(Modifier.padding(start = 54.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
}

private enum class TrackSection(val label: String) { Overview("Overview"), Lyrics("Lyrics"), Notes("Notes") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackDetailScreen(
    track: CatalogTrack,
    catalog: CatalogSnapshot,
    storage: CatalogStorage,
    onBack: () -> Unit,
    onRelease: (Long) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sections = buildList {
        if (!track.description.isNullOrBlank() || track.releases.isNotEmpty()) add(TrackSection.Overview)
        if (!track.lyrics.isNullOrBlank()) add(TrackSection.Lyrics)
        if (!track.notes.isNullOrBlank()) add(TrackSection.Notes)
    }
    var sectionName by rememberSaveable(track.id) { mutableStateOf(sections.firstOrNull()?.name) }
    val section = sectionName?.let(TrackSection::valueOf)
    val exportLyrics = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                    it.write(track.lyrics.orEmpty())
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(track.title, style = MaterialTheme.typography.headlineLarge)
                    if (track.releases.isNotEmpty()) {
                        Text(
                            if (track.releases.size == 1) "Appears on 1 release" else "Appears on ${track.releases.size} releases",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (sections.size > 1) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sections.forEach { item ->
                            FilterChip(section == item, { sectionName = item.name }, { Text(item.label) })
                        }
                    }
                }
            }
            when (section) {
                TrackSection.Overview -> {
                    track.description?.takeIf(String::isNotBlank)?.let { description ->
                        item {
                            DetailTextSection("About", description)
                        }
                    }
                    if (track.releases.isNotEmpty()) {
                        item { Text("Releases", style = MaterialTheme.typography.headlineSmall) }
                        items(track.releases, key = { it.releaseId }) { link ->
                            val release = catalog.releases.firstOrNull { it.id == link.releaseId }
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onRelease(link.releaseId) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (release != null) {
                                    ReleaseArtwork(release, catalog.treeUri, storage, Modifier.size(62.dp))
                                }
                                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                                    Text(link.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${link.type} · Track ${link.trackOrder}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(Icons.Outlined.ChevronRight, null)
                            }
                        }
                    }
                }
                TrackSection.Lyrics -> {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lyrics", style = MaterialTheme.typography.headlineSmall)
                            Button(
                                onClick = { exportLyrics.launch(safeLyricsFilename(track.title)) },
                            ) { Text("Export") }
                        }
                    }
                    item { SelectionContainer { Text(track.lyrics.orEmpty(), style = MaterialTheme.typography.bodyLarge) } }
                }
                TrackSection.Notes -> item { DetailTextSection("Notes", track.notes.orEmpty()) }
                null -> item { Text("No description, lyrics, notes, or release links are available for this track.") }
            }
        }
    }
}

@Composable
private fun DetailTextSection(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        SelectionContainer { Text(text, style = MaterialTheme.typography.bodyLarge) }
    }
}

private fun safeLyricsFilename(title: String): String {
    val safe = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "lyrics" }
    return "$safe.txt"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(
    catalog: CatalogSnapshot,
    storage: CatalogStorage,
    onBack: () -> Unit,
    onRelease: (Long) -> Unit,
    onTrack: (Long) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val trimmedQuery = query.trim()
    val releases = remember(catalog.releases, trimmedQuery) {
        if (trimmedQuery.isEmpty()) emptyList()
        else catalog.releases.filter { it.title.contains(trimmedQuery, ignoreCase = true) }
    }
    val tracks = remember(catalog.tracks, trimmedQuery) {
        if (trimmedQuery.isEmpty()) emptyList()
        else catalog.tracks.filter { it.title.contains(trimmedQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                placeholder = { Text("Search releases and tracks") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Outlined.Close, "Clear search")
                        }
                    }
                },
                singleLine = true,
            )

            when {
                trimmedQuery.isEmpty() -> SearchMessage(
                    icon = Icons.Outlined.Search,
                    title = "Search your catalogue",
                    message = "Find a release or track by title.",
                )
                releases.isEmpty() && tracks.isEmpty() -> SearchMessage(
                    icon = Icons.Outlined.SearchOff,
                    title = "No results",
                    message = "Nothing matched “$trimmedQuery”.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (releases.isNotEmpty()) {
                        item {
                            Text(
                                "Releases",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
                            )
                        }
                        items(releases, key = { "release-${it.id}" }) { release ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onRelease(release.id) }
                                    .padding(vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ReleaseArtwork(release, catalog.treeUri, storage, Modifier.size(62.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                                    Text(release.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${release.type} · ${release.trackCount} tracks",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(Icons.Outlined.ChevronRight, null)
                            }
                        }
                    }
                    if (tracks.isNotEmpty()) {
                        item {
                            Text(
                                "Tracks",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(top = 22.dp, bottom = 8.dp),
                            )
                        }
                        items(tracks, key = { "track-${it.id}" }) { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTrack(track.id) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(track.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        track.membership,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Icon(Icons.Outlined.ChevronRight, null)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchMessage(icon: ImageVector, title: String, message: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    catalog: CatalogSnapshot,
    onBack: () -> Unit,
    onChangeFolder: () -> Unit,
    onImportZip: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
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
                    OutlinedButton(onClick = onImportZip) {
                        Icon(Icons.Outlined.Archive, null)
                        Spacer(Modifier.size(8.dp))
                        Text("Import TrackLog ZIP")
                    }
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
    TrackLogTheme { ConnectCatalogScreen({}, {}) }
}
