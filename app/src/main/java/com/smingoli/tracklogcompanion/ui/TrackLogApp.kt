package com.smingoli.tracklogcompanion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smingoli.tracklogcompanion.ui.theme.TrackLogTheme

private enum class Destination(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Outlined.Home),
    Releases("Releases", Icons.Outlined.Album),
    Tracks("Tracks", Icons.Outlined.MusicNote),
}

private data class ReleasePreview(
    val title: String,
    val type: String,
    val trackCount: Int,
    val colors: List<Color>,
)

private val previewReleases = listOf(
    ReleasePreview("Neon Weather", "Album", 11, listOf(Color(0xFFEE6C4D), Color(0xFF293241))),
    ReleasePreview("Midnight Rooms", "EP", 6, listOf(Color(0xFF735D78), Color(0xFFB392AC))),
    ReleasePreview("Northern Lines", "Single", 2, listOf(Color(0xFF33658A), Color(0xFFF6AE2D))),
    ReleasePreview("Paper Satellites", "Album", 10, listOf(Color(0xFF2A9D8F), Color(0xFFE9C46A))),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackLogApp() {
    var destination by remember { mutableStateOf(Destination.Home) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TrackLog", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (destination) {
            Destination.Home -> HomeScreen(Modifier.padding(padding))
            Destination.Releases -> ReleasesScreen(Modifier.padding(padding))
            Destination.Tracks -> TracksScreen(Modifier.padding(padding))
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your music, at a glance", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Read-only companion for your TrackLog catalog",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { CatalogTotals() }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recent releases", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "See all",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { }.padding(8.dp),
                )
            }
        }
        item {
            ReleaseGrid(previewReleases)
        }
    }
}

@Composable
private fun CatalogTotals() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MetricCard("107", "Tracks", Modifier.weight(1f))
        MetricCard("14", "Available", Modifier.weight(1f))
        MetricCard("12", "Releases", Modifier.weight(1f))
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReleaseGrid(releases: List<ReleasePreview>) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        releases.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { release ->
                    ReleaseCard(release, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReleaseCard(release: ReleasePreview, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clickable { }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(release.colors)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = Color.White.copy(alpha = 0.82f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            release.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${release.type} · ${release.trackCount} tracks",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReleasesScreen(modifier: Modifier = Modifier) {
    var filter by remember { mutableStateOf("All") }
    Column(modifier = modifier.fillMaxSize().padding(top = 16.dp)) {
        Text(
            "Releases",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("All", "Albums", "EPs", "Singles").forEach { label ->
                FilterChip(
                    selected = filter == label,
                    onClick = { filter = label },
                    label = { Text(label) },
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(previewReleases) { ReleaseCard(it) }
        }
    }
}

@Composable
private fun TracksScreen(modifier: Modifier = Modifier) {
    var availableOnly by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "Tracks",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
        )
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(!availableOnly, { availableOnly = false }, { Text("All") })
            FilterChip(availableOnly, { availableOnly = true }, { Text("Available") })
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(14) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        (index + 1).toString().padStart(2, '0'),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 18.dp),
                    )
                    Text("Track title ${index + 1}", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun TrackLogAppPreview() {
    TrackLogTheme { TrackLogApp() }
}
