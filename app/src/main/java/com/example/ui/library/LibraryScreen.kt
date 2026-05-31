package com.example.ui.library

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.domain.model.MediaItem
import com.example.domain.model.MediaType
import com.example.ui.components.GlassBox
import com.example.ui.player.PlayerViewModel
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.Typography

enum class LibraryTab(val title: String, val icon: ImageVector) {
    TRACKS("Temas", Icons.Rounded.MusicNote),
    VIDEOS("Videos", Icons.Rounded.Videocam),
    ARTISTS("Artistas", Icons.Rounded.Person),
    ALBUMS("Álbumes", Icons.Rounded.Album),
    PLAYLISTS("Playlists", Icons.Rounded.PlaylistPlay),
    HISTORY("Historial", Icons.Rounded.History)
}

@Composable
fun LibraryScreen(
    viewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit
) {
    val context = LocalContext.current
    var hasPermissionState by remember { mutableStateOf(false) }

    // Resolve needed permission depending on OS version
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    // Helper function to check if permissions are already granted
    fun checkLibraryPermissions(ctx: Context): Boolean {
        return permissionsToRequest.all {
            ContextCompat.checkSelfPermission(ctx, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    // Launcher for requesting permission
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        hasPermissionState = granted
        // Trigger local indexing thread (handles empty system lists elegantly)
        viewModel.scanLocalMedia()
    }

    // Run check on screen startup
    LaunchedEffect(Unit) {
        val granted = checkLibraryPermissions(context)
        hasPermissionState = granted
        viewModel.scanLocalMedia()
    }

    var activeTab by remember { mutableStateOf(LibraryTab.TRACKS) }
    val localMedia by viewModel.localMediaList.collectAsState()
    val userPlaylists by viewModel.userPlaylists.collectAsState()
    val userPlaylistItems by viewModel.userPlaylistItems.collectAsState()
    val playbackHistory by viewModel.playbackHistory.collectAsState()

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    var currentSelectedPlaylistForDetails by remember { mutableStateOf<String?>(null) }
    var showTrackSelectorForPlaylist by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 120.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Cyberpunk Header Branding ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MI BIBLIOTECA",
                            fontSize = 28.sp,
                            fontFamily = Typography.headlineMedium.fontFamily,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            lineHeight = 32.sp
                        )
                        Text(
                            text = "Persistencia Room • Almacenamiento Local",
                            fontSize = 11.sp,
                            color = AccentCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(PrimaryNeon.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, PrimaryNeon.copy(alpha = 0.4f), CircleShape)
                            .clickable { viewModel.scanLocalMedia() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refrescar escaneo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // --- Permissions Alert Banner if not granted ---
            if (!hasPermissionState) {
                item {
                    GlassBox(
                        cornerRadius = 16.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PrimaryNeon.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = PrimaryNeon,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Acceso Completo al Dispositivo",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Para reproducir tus propios archivos .MP3, .FLAC, .MP4 o .MKV, concédenos permiso de lectura.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Button(
                                onClick = { launcher.launch(permissionsToRequest) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                            ) {
                                Text(
                                    text = "CONCEDER PERMISO",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // --- Category Selector (Cyber Neon Horizontal Slider) ---
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(LibraryTab.values()) { tab ->
                        val active = activeTab == tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (active) Brush.horizontalGradient(listOf(PrimaryNeon, PrimaryNeon.copy(alpha = 0.6f)))
                                    else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.02f)))
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (active) PrimaryNeon.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    activeTab = tab
                                    currentSelectedPlaylistForDetails = null
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (active) Color.White else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tab.title,
                                    color = if (active) Color.White else Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // --- Main Tab Contents with Elegant Slide/Crossfades ---
            item {
                AnimatedContent(
                    targetState = activeTab,
                    label = "tab_content_animation",
                    transitionSpec = {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(150))
                    }
                ) { targetTab ->
                    when (targetTab) {
                        LibraryTab.TRACKS -> {
                            val audioFiles = localMedia.filter { it.mediaType == MediaType.AUDIO }
                            if (audioFiles.isEmpty()) {
                                EmptyLibraryState(message = "Buscando archivos de audio en tu dispositivo...")
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    audioFiles.forEach { track ->
                                        TrackMediaRow(
                                            track = track,
                                            onPlay = {
                                                viewModel.playMedia(track)
                                                onNavigateToPlayer()
                                            },
                                            onAddToPlaylist = {
                                                if (userPlaylists.isEmpty()) {
                                                    viewModel.createUserPlaylist("Favoritos Neon")
                                                }
                                                // Save track directly to first playlist if available, or list
                                                showTrackSelectorForPlaylist = track.id
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        LibraryTab.VIDEOS -> {
                            val videoFiles = localMedia.filter { it.mediaType == MediaType.VIDEO }
                            if (videoFiles.isEmpty()) {
                                EmptyLibraryState(message = "No se encontraron videos grabados en el almacenamiento.")
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    videoFiles.forEach { video ->
                                        TrackMediaRow(
                                            track = video,
                                            onPlay = {
                                                viewModel.playMedia(video)
                                                onNavigateToPlayer()
                                            },
                                            onAddToPlaylist = {
                                                showTrackSelectorForPlaylist = video.id
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        LibraryTab.ARTISTS -> {
                            val artistsMap = localMedia.groupBy { it.artist }
                            if (artistsMap.isEmpty()) {
                                EmptyLibraryState()
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    artistsMap.forEach { (artist, tracks) ->
                                        ArtistAlbumRow(
                                            title = artist,
                                            subtitle = "${tracks.size} pistas locales",
                                            icon = Icons.Rounded.Person,
                                            onClick = {
                                                // Play all artist tracks
                                                if (tracks.isNotEmpty()) {
                                                    viewModel.playMedia(tracks.first())
                                                    onNavigateToPlayer()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        LibraryTab.ALBUMS -> {
                            val albumsMap = localMedia.groupBy { it.album }
                            if (albumsMap.isEmpty()) {
                                EmptyLibraryState()
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    albumsMap.forEach { (album, tracks) ->
                                        ArtistAlbumRow(
                                            title = if (album.isEmpty()) "Sin Álbum" else album,
                                            subtitle = "${tracks.size} canciones",
                                            icon = Icons.Rounded.Album,
                                            onClick = {
                                                if (tracks.isNotEmpty()) {
                                                    viewModel.playMedia(tracks.first())
                                                    onNavigateToPlayer()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        LibraryTab.PLAYLISTS -> {
                            if (currentSelectedPlaylistForDetails == null) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Playlist Creator Trigger Button
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.03f))
                                            .border(1.dp, AccentCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                            .clickable { showCreatePlaylistDialog = true }
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = null,
                                                tint = AccentCyan
                                            )
                                            Text(
                                                text = "Crear Nueva Playlist Personalizada (Room)",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    if (userPlaylists.isEmpty()) {
                                        EmptyLibraryState(message = "Aún no has creado playlists personalizadas en Room DB.")
                                    } else {
                                        userPlaylists.forEach { playlist ->
                                            val itemsCount = userPlaylistItems[playlist.id]?.size ?: 0
                                            PlaylistRow(
                                                name = playlist.name,
                                                count = itemsCount,
                                                onClick = {
                                                    currentSelectedPlaylistForDetails = playlist.id
                                                },
                                                onDelete = {
                                                    viewModel.deleteUserPlaylist(playlist.id)
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Detail View of a Selected Playlist
                                val pId = currentSelectedPlaylistForDetails!!
                                val playlistDetails = userPlaylists.firstOrNull { it.id == pId }
                                val itemsInPlaylist = userPlaylistItems[pId] ?: emptyList()

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IconButton(onClick = { currentSelectedPlaylistForDetails = null }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ArrowBack,
                                                    contentDescription = "Volver",
                                                    tint = Color.White
                                                )
                                            }
                                            Text(
                                                text = playlistDetails?.name ?: "Detalle",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 18.sp
                                            )
                                        }

                                        Text(
                                            text = "${itemsInPlaylist.size} Items",
                                            color = AccentCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    if (itemsInPlaylist.isEmpty()) {
                                        EmptyLibraryState(message = "Esta playlist está vacía. Añade temas desde la pestaña 'Temas'.")
                                    } else {
                                        itemsInPlaylist.forEach { item ->
                                            TrackMediaRow(
                                                track = MediaItem(
                                                    id = item.mediaId,
                                                    title = item.title,
                                                    artist = item.artist,
                                                    coverUrl = item.coverUrl,
                                                    streamUrl = item.streamUrl,
                                                    mediaType = try { MediaType.valueOf(item.mediaType) } catch(e:Exception){MediaType.AUDIO}
                                                ),
                                                onPlay = {
                                                    viewModel.playMedia(
                                                        MediaItem(
                                                            id = item.mediaId,
                                                            title = item.title,
                                                            artist = item.artist,
                                                            coverUrl = item.coverUrl,
                                                            streamUrl = item.streamUrl,
                                                            mediaType = try { MediaType.valueOf(item.mediaType) } catch(e:Exception){MediaType.AUDIO}
                                                        )
                                                    )
                                                    onNavigateToPlayer()
                                                },
                                                onAddToPlaylist = {},
                                                isRemoveOption = true,
                                                onRemove = {
                                                    viewModel.removeTrackFromUserPlaylist(pId, item.mediaId)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        LibraryTab.HISTORY -> {
                            if (playbackHistory.isEmpty()) {
                                EmptyLibraryState(message = "Aún no has sintonizado contenido. ¡Tu historial de reproducción aparecerá aquí!")
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    playbackHistory.forEach { hist ->
                                        TrackMediaRow(
                                            track = MediaItem(
                                                id = hist.id,
                                                title = hist.title,
                                                artist = hist.artist,
                                                coverUrl = hist.coverUrl,
                                                streamUrl = hist.streamUrl,
                                                mediaType = try { MediaType.valueOf(hist.mediaType) } catch(e:Exception){MediaType.AUDIO}
                                            ),
                                            onPlay = {
                                                viewModel.playMedia(
                                                    MediaItem(
                                                        id = hist.id,
                                                        title = hist.title,
                                                        artist = hist.artist,
                                                        coverUrl = hist.coverUrl,
                                                        streamUrl = hist.streamUrl,
                                                        mediaType = try { MediaType.valueOf(hist.mediaType) } catch(e:Exception){MediaType.AUDIO}
                                                    )
                                                )
                                                onNavigateToPlayer()
                                            },
                                            onAddToPlaylist = {
                                                showTrackSelectorForPlaylist = hist.id
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Dialogs & Popups ---

        // Dialog for creating a playlist
        if (showCreatePlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                title = {
                    Text(
                        text = "Nueva Playlist Room",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Establece el nombre para tu playlist cyber-neon:",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = playlistNameInput,
                            onValueChange = { playlistNameInput = it },
                            placeholder = { Text("Ej. Synth Classics", color = Color.Gray) },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = AccentCyan
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (playlistNameInput.isNotBlank()) {
                                viewModel.createUserPlaylist(playlistNameInput)
                                playlistNameInput = ""
                                showCreatePlaylistDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
                    ) {
                        Text("Crear", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlaylistDialog = false }) {
                        Text("Cancelar", color = Color.White.copy(alpha = 0.5f))
                    }
                },
                containerColor = Color(0xFF0F0F1A),
                modifier = Modifier.border(1.dp, PrimaryNeon.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
            )
        }

        // Dropdown Dialog for picking playlist to add item to
        if (showTrackSelectorForPlaylist != null) {
            val trackId = showTrackSelectorForPlaylist!!
            val chosenMediaItem = localMedia.firstOrNull { it.id == trackId }
                ?: playbackHistory.firstOrNull { it.id == trackId }?.let {
                    MediaItem(
                        id = it.id,
                        title = it.title,
                        artist = it.artist,
                        coverUrl = it.coverUrl,
                        streamUrl = it.streamUrl,
                        mediaType = try { MediaType.valueOf(it.mediaType) } catch(e:Exception){MediaType.AUDIO}
                    )
                }

            if (chosenMediaItem != null) {
                AlertDialog(
                    onDismissRequest = { showTrackSelectorForPlaylist = null },
                    title = {
                        Text(
                            text = "Añadir a Playlist Room",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Elige una de tus listas guardadas en Room para agregar '${chosenMediaItem.title}':",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )

                            if (userPlaylists.isEmpty()) {
                                Text(
                                    text = "No tienes playlists creadas. Por favor crea una primero en la sección de Playlists.",
                                    color = PrimaryNeon,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.height(180.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(userPlaylists) { pl ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .clickable {
                                                    viewModel.addTrackToUserPlaylist(pl.id, chosenMediaItem)
                                                    showTrackSelectorForPlaylist = null
                                                }
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(text = pl.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showTrackSelectorForPlaylist = null }) {
                            Text("Cerrar", color = Color.White.copy(alpha = 0.5f))
                        }
                    },
                    containerColor = Color(0xFF0F0F1A),
                    modifier = Modifier.border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
                )
            }
        }
    }
}

@Composable
fun TrackMediaRow(
    track: MediaItem,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    isRemoveOption: Boolean = false,
    onRemove: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .clickable { onPlay() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Audio/Video Avatar Glowing Box
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (track.mediaType == MediaType.VIDEO) PrimaryNeon.copy(alpha = 0.15f)
                            else AccentCyan.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (track.mediaType == MediaType.VIDEO) Icons.Rounded.Videocam else Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = if (track.mediaType == MediaType.VIDEO) PrimaryNeon else AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = track.artist,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.mediaType == MediaType.VIDEO) {
                    Text(
                        text = "VÍDEO",
                        fontSize = 8.sp,
                        color = PrimaryNeon,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .background(PrimaryNeon.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (isRemoveOption) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Quitar",
                            tint = PrimaryNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    IconButton(onClick = onAddToPlaylist, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.PlaylistAdd,
                            contentDescription = "Añadir a playlist",
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistAlbumRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(AccentCyan.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }

            Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = "Reproducir todo", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun PlaylistRow(
    name: String,
    count: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(PrimaryNeon.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Rounded.FeaturedPlayList, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(text = name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "$count elementos guardados", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Eliminar List", tint = PrimaryNeon.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
                Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun EmptyLibraryState(message: String = "No hay resultados en esta sección de biblioteca.") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Inbox,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
