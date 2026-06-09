package com.dulce.play.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dulce.play.domain.model.MediaItem
import com.dulce.play.domain.model.MediaType
import com.dulce.play.domain.model.IPTVChannel
import com.dulce.play.ui.player.PlayerViewModel
import com.dulce.play.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DulceSearchTopBar(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val isOverlayActive by viewModel.searchOverlayActive.collectAsState()

    // Persistent elegant glass top bar for entry
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .clickable {
                viewModel.setSearchOverlayActive(true)
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Buscar",
                tint = AccentCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (query.isNotEmpty()) query else "Buscar en DulcePlay (música, IPTV, canales...)",
                color = if (query.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = {
                        viewModel.updateSearchQuery("")
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Limpiar",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = "Dulce Intelligent Search",
                    tint = PrimaryNeon.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DulceSearchOverlay(
    viewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val onlineResults by viewModel.onlineSearchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val suggestion by viewModel.searchSuggestion.collectAsState()
    val localMediaList by viewModel.localMediaList.collectAsState()
    val playlistsState by viewModel.userPlaylists.collectAsState()
    val historyState by viewModel.playbackHistory.collectAsState()

    // Local lists for manual quick filtering
    val localTracks = remember(query, localMediaList) {
        val tracksList = (viewModel.getFilteredMediaList().filter { it.mediaType == MediaType.AUDIO } +
                localMediaList).distinctBy { it.id }
        if (query.length < 2) emptyList() else {
            tracksList.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.genre.contains(query, ignoreCase = true)
            }
        }
    }

    val localVideos = remember(query) {
        val videosList = viewModel.getFilteredMediaList().filter { it.mediaType == MediaType.VIDEO }
        if (query.length < 2) emptyList() else {
            videosList.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
            }
        }
    }

    val localIPTVChannels = remember(query) {
        val channels = viewModel.getAllIPTVChannels()
        if (query.length < 2) emptyList() else {
            channels.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.group.contains(query, ignoreCase = true) ||
                it.country.contains(query, ignoreCase = true)
            }
        }
    }

    val localPlaylists = remember(query, playlistsState) {
        if (query.length < 2) emptyList() else {
            playlistsState.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
    }

    val localHistory = remember(query, historyState) {
        if (query.length < 2) emptyList() else {
            historyState.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
            }
        }
    }

    val hasLocalResults = localTracks.isNotEmpty() ||
            localVideos.isNotEmpty() ||
            localIPTVChannels.isNotEmpty() ||
            localPlaylists.isNotEmpty() ||
            localHistory.isNotEmpty()

    // Full screen overlay with glass background
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) {} // Prevent click-throughs
    ) {
        val isWideScreen = maxWidth > 650.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // --- Header: Search Bar inside Overlay with Back/Dismiss and Auto Focus ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                IconButton(
                    onClick = { viewModel.setSearchOverlayActive(false) },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cerrar buscador",
                        tint = AccentCyan
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Real TextField wrapper with Glassmorphic visual look and sound key inputs
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        viewModel.updateSearchQuery(it)
                    },
                    placeholder = { Text("Escribe artista, video, IPTV...", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        cursorColor = PrimaryNeon
                    ),
                    shape = RoundedCornerShape(24.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = AccentCyan
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Limpiar consulta",
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                )
            }

            // --- INTUITIVE SEARCH SUGGESTIONS CHIP (Did you mean?) ---
            AnimatedVisibility(
                visible = suggestion != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                suggestion?.let { sug ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp, start = 8.dp, end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryNeon.copy(alpha = 0.15f))
                            .border(1.dp, PrimaryNeon.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.updateSearchQuery(sug)
                                viewModel.addSearchQueryToHistory(sug)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Sugerencia inteligente",
                            tint = PrimaryNeon,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "¿Quizás quisiste decir: ",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = sug,
                            color = AccentCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "?",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // --- SEARCH HISTORY SECTION (Recientes) ---
            if (query.isBlank() && searchHistory.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BÚSQUEDAS RECIENTES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            letterSpacing = 1.sp
                        )
                        TextButton(
                            onClick = { viewModel.clearSearchHistory() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "Borrar historial",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Borrar",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(searchHistory) { historyTerm ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        viewModel.updateSearchQuery(historyTerm)
                                        viewModel.addSearchQueryToHistory(historyTerm)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = historyTerm,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- MAIN LISTS: BOTH COLUMN (WIDE) OR ONE COLUMN VIEW (NARROW) ---
            if (query.isBlank()) {
                // Empty State placeholder beautifully styled with instructions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "DULCE-SEARCH UNIVERSAL",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Encuentra canciones, videos, canales IPTV o echa un vistazo en internet de forma instantánea y libre.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.widthIn(max = 280.dp)
                        )
                    }
                }
            } else {
                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Buscando en celular e internet...",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else if (!hasLocalResults && onlineResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No se encontraron resultados para \"$query\"",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Prueba con otra palabra o revisa la ortografía.",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    // Results Present!
                    if (isWideScreen) {
                        // Adaptive: Two Column canonical layout
                        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            // Column Left: De tu celular (Local)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                ResultSectionHeader(
                                    title = "DE TU CELULAR (LOCAL)",
                                    icon = Icons.Rounded.Smartphone,
                                    badgeCount = localTracks.size + localVideos.size + localIPTVChannels.size + localPlaylists.size + localHistory.size
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    renderLocalResults(
                                        viewModel = viewModel,
                                        localTracks = localTracks,
                                        localVideos = localVideos,
                                        localIPTVChannels = localIPTVChannels,
                                        localPlaylists = localPlaylists,
                                        localHistory = localHistory,
                                        onNavigateToPlayer = onNavigateToPlayer
                                    )
                                }
                            }

                            // Column Right: Encontrados en línea (Online)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                ResultSectionHeader(
                                    title = "ENCONTRADOS EN LÍNEA (INTERNET)",
                                    icon = Icons.Rounded.Cloud,
                                    badgeCount = onlineResults.size,
                                    pulseColor = PrimaryNeon
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    renderOnlineResults(
                                        viewModel = viewModel,
                                        onlineResults = onlineResults,
                                        onNavigateToPlayer = onNavigateToPlayer
                                    )
                                }
                            }
                        }
                    } else {
                        // Portrait: Unified single column list
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            // --- LOCAL MATCHES HEADER ---
                            if (hasLocalResults) {
                                item {
                                    ResultSectionHeader(
                                        title = "DE TU CELULAR (LOCAL)",
                                        icon = Icons.Rounded.Smartphone,
                                        badgeCount = localTracks.size + localVideos.size + localIPTVChannels.size + localPlaylists.size + localHistory.size
                                    )
                                }
                                renderLocalResults(
                                    viewModel = viewModel,
                                    localTracks = localTracks,
                                    localVideos = localVideos,
                                    localIPTVChannels = localIPTVChannels,
                                    localPlaylists = localPlaylists,
                                    localHistory = localHistory,
                                    onNavigateToPlayer = onNavigateToPlayer
                                )
                            }

                            // --- ONLINE MATCHES HEADER ---
                            if (onlineResults.isNotEmpty()) {
                                item {
                                    ResultSectionHeader(
                                        title = "ENCONTRADOS EN LÍNEA (ONLINE)",
                                        icon = Icons.Rounded.Cloud,
                                        badgeCount = onlineResults.size,
                                        pulseColor = PrimaryNeon
                                    )
                                }
                                renderOnlineResults(
                                    viewModel = viewModel,
                                    onlineResults = onlineResults,
                                    onNavigateToPlayer = onNavigateToPlayer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeCount: Int,
    pulseColor: Color = AccentCyan
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = pulseColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            letterSpacing = 1.2.sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .background(pulseColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .border(0.5.dp, pulseColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = badgeCount.toString(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Extension to split local elements render
fun LazyListScope.renderLocalResults(
    viewModel: PlayerViewModel,
    localTracks: List<MediaItem>,
    localVideos: List<MediaItem>,
    localIPTVChannels: List<IPTVChannel>,
    localPlaylists: List<com.dulce.play.data.local.entity.UserPlaylistEntity>,
    localHistory: List<com.dulce.play.data.local.entity.PlaybackHistoryEntity>,
    onNavigateToPlayer: () -> Unit
) {
    if (localTracks.isNotEmpty()) {
        item {
            SubCategoryLabel(title = "Canciones")
        }
        items(localTracks) { track ->
            SearchResultRow(
                title = track.title,
                subtitle = "Canción • ${track.artist}",
                coverUrl = track.coverUrl,
                iconType = Icons.Rounded.MusicNote,
                iconTint = AccentCyan,
                onClick = {
                    viewModel.addSearchQueryToHistory(viewModel.searchQuery.value)
                    viewModel.playMedia(track)
                    viewModel.setSearchOverlayActive(false)
                    onNavigateToPlayer()
                }
            )
        }
    }

    if (localVideos.isNotEmpty()) {
        item {
            SubCategoryLabel(title = "Videos")
        }
        items(localVideos) { video ->
            SearchResultRow(
                title = video.title,
                subtitle = "Video • ${video.artist}",
                coverUrl = video.coverUrl,
                iconType = Icons.Rounded.Movie,
                iconTint = PrimaryNeon,
                onClick = {
                    viewModel.addSearchQueryToHistory(viewModel.searchQuery.value)
                    viewModel.playMedia(video)
                    viewModel.setSearchOverlayActive(false)
                    onNavigateToPlayer()
                }
            )
        }
    }

    if (localIPTVChannels.isNotEmpty()) {
        item {
            SubCategoryLabel(title = "Televisión de Satélite (IPTV)")
        }
        items(localIPTVChannels) { channel ->
            SearchResultRow(
                title = channel.name,
                subtitle = "Canal Live • ${channel.group}",
                coverUrl = channel.logoUrl,
                iconType = Icons.Rounded.Tv,
                iconTint = Color.Green,
                onClick = {
                    viewModel.addSearchQueryToHistory(viewModel.searchQuery.value)
                    viewModel.playIPTVChannel(channel)
                    viewModel.setSearchOverlayActive(false)
                    onNavigateToPlayer()
                }
            )
        }
    }

    if (localPlaylists.isNotEmpty()) {
        item {
            SubCategoryLabel(title = "Playlists en Room")
        }
        items(localPlaylists) { playlist ->
            SearchResultRow(
                title = playlist.name,
                subtitle = "Creada en este perfil",
                coverUrl = "playlist_cover",
                iconType = Icons.Rounded.PlaylistPlay,
                iconTint = AccentCyan,
                onClick = {
                    viewModel.addSearchQueryToHistory(viewModel.searchQuery.value)
                    // Find first track in playlist and play it, or trigger play
                    val items = viewModel.userPlaylistItems.value[playlist.id] ?: emptyList()
                    if (items.isNotEmpty()) {
                        val firstMatchId = items.first().mediaId
                        val trackObj = (viewModel.getFilteredMediaList() + viewModel.localMediaList.value)
                            .firstOrNull { it.id == firstMatchId }
                        if (trackObj != null) {
                            viewModel.playMedia(trackObj)
                            onNavigateToPlayer()
                        }
                    }
                    viewModel.setSearchOverlayActive(false)
                }
            )
        }
    }

    if (localHistory.isNotEmpty()) {
        item {
            SubCategoryLabel(title = "Historial Reciente")
        }
        items(localHistory) { item ->
            val mappedItem = MediaItem(
                id = item.id,
                title = item.title,
                artist = item.artist,
                coverUrl = item.coverUrl,
                streamUrl = item.streamUrl,
                mediaType = MediaType.valueOf(item.mediaType),
                album = "Historial"
            )
            SearchResultRow(
                title = item.title,
                subtitle = "Historial • ${item.artist}",
                coverUrl = item.coverUrl,
                iconType = Icons.Rounded.History,
                iconTint = Color.White.copy(alpha = 0.5f),
                onClick = {
                    viewModel.addSearchQueryToHistory(viewModel.searchQuery.value)
                    viewModel.playMedia(mappedItem)
                    viewModel.setSearchOverlayActive(false)
                    onNavigateToPlayer()
                }
            )
        }
    }
}

fun LazyListScope.renderOnlineResults(
    viewModel: PlayerViewModel,
    onlineResults: List<MediaItem>,
    onNavigateToPlayer: () -> Unit
) {
    items(onlineResults) { track ->
        SearchResultRow(
            title = track.title,
            subtitle = "Internet • ${track.artist}",
            coverUrl = track.coverUrl,
            iconType = Icons.Rounded.Cloud,
            iconTint = PrimaryNeon,
            isOnline = true,
            onClick = {
                viewModel.addSearchQueryToHistory(viewModel.searchQuery.value)
                viewModel.playMedia(track)
                viewModel.setSearchOverlayActive(false)
                onNavigateToPlayer()
            }
        )
    }
}

@Composable
fun SubCategoryLabel(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.4f),
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 8.dp)
    )
}

@Composable
fun SearchResultRow(
    title: String,
    subtitle: String,
    coverUrl: String,
    iconType: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isOnline: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Artwork / Cover with dynamic Coil or placeholder
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (coverUrl.startsWith("http") || coverUrl.startsWith("https")) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = "Carátula",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = iconType,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOnline) {
                        Box(
                            modifier = Modifier
                                .background(PrimaryNeon.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "ONLINE",
                                color = PrimaryNeon,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(iconTint.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Reproducir",
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
