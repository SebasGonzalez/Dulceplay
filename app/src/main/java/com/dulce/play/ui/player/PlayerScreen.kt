package com.dulce.play.ui.player

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.dulce.play.utils.LocalStorage

// ── Colores del sistema Cyber Neon ────────────────────────────────────────────
private val NeonCyan   = Color(0xFF00E5FF)
private val NeonPurple = Color(0xFFBB86FC)
private val NeonGreen  = Color(0xFF39FF14)
private val DarkBg     = Color(0xFF0A0A0F)
private val CardBg     = Color(0xFF141420)
private val SurfaceBg  = Color(0xFF1A1A2E)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val resultados by viewModel.onlineSearchResults.collectAsState()
    val cargando by viewModel.isSearching.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentMedia by viewModel.currentMedia.collectAsState()
    val mediaError by viewModel.mediaError.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val currentSec by viewModel.currentTimeSeconds.collectAsState()
    val totalSec by viewModel.totalDurationSeconds.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val playQueue by viewModel.playQueue.collectAsState()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsState()
    val favorites by viewModel.persistedFavorites.collectAsState()
    val playlists by viewModel.persistedPlaylists.collectAsState()
    val calidades by viewModel.listaCalidades.collectAsState()

    var showQueue by remember { mutableStateOf(false) }
    var showPlaylistMenu by remember { mutableStateOf<com.dulce.play.domain.model.MediaItem?>(null) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── TOP BAR ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(NeonPurple.copy(0.15f), NeonCyan.copy(0.08f))))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = NeonCyan)
                }
                Text(
                    "DulcePlay V3.9 🎵",
                    color = Color.White, fontWeight = FontWeight.Black,
                    fontSize = 18.sp, modifier = Modifier.weight(1f)
                )
                // Botón "Ver Cola"
                IconButton(onClick = { showQueue = !showQueue }) {
                    BadgedBox(
                        badge = {
                            if (playQueue.isNotEmpty()) Badge(containerColor = NeonCyan) {
                                Text("${playQueue.size}", fontSize = 8.sp, color = DarkBg)
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, "Cola", tint = if (showQueue) NeonCyan else Color.White)
                    }
                }
            }

            // ── REPRODUCTOR DE VIDEO ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = viewModel.exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // Overlay con gradiente inferior
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, DarkBg.copy(alpha = 0.8f))))
                )
            }

            // ── INFO DE MEDIA Y CONTROLES ─────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            currentMedia.title.ifBlank { "Sin reproducción" },
                            color = Color.White, fontWeight = FontWeight.Bold,
                            fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            currentMedia.artist.ifBlank { " " },
                            color = NeonCyan.copy(alpha = 0.8f), fontSize = 12.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Botón favorito
                    val isFav = favorites.any { it.id == currentMedia.id }
                    IconButton(onClick = { viewModel.toggleFavorite(currentMedia) }) {
                        Icon(
                            if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            "Favorito",
                            tint = if (isFav) Color(0xFFFF4081) else Color.White.copy(alpha = 0.5f)
                        )
                    }
                    // Menú 3 puntos
                    IconButton(onClick = { showPlaylistMenu = currentMedia }) {
                        Icon(Icons.Rounded.MoreVert, "Opciones", tint = Color.White.copy(0.7f))
                    }
                }

                // ── BARRA DE PROGRESO ─────────────────────────────────────────
                Slider(
                    value = progress,
                    onValueChange = { viewModel.seekPercent(it) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentSec), color = Color.White.copy(0.5f), fontSize = 10.sp)
                    Text(formatTime(totalSec), color = Color.White.copy(0.5f), fontSize = 10.sp)
                }

                // ── CONTROLES DE REPRODUCCIÓN ────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Aleatorio
                    PlayerControlBtn(
                        icon = { Icon(Icons.Rounded.Shuffle, "Aleatorio", modifier = Modifier.size(22.dp), tint = if (shuffleEnabled) NeonGreen else Color.White.copy(0.5f)) },
                        onClick = { viewModel.toggleShuffle() }
                    )
                    // Anterior
                    PlayerControlBtn(
                        icon = { Icon(Icons.Rounded.SkipPrevious, "Anterior", modifier = Modifier.size(30.dp), tint = Color.White) },
                        onClick = { viewModel.prev() }
                    )
                    // Play/Pause (botón grande)
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .shadow(12.dp, CircleShape)
                            .background(
                                Brush.radialGradient(listOf(NeonCyan.copy(0.9f), NeonPurple.copy(0.6f))),
                                CircleShape
                            )
                            .clickable { viewModel.togglePlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            "Play/Pause", tint = Color.White, modifier = Modifier.size(36.dp)
                        )
                    }
                    // Siguiente
                    PlayerControlBtn(
                        icon = { Icon(Icons.Rounded.SkipNext, "Siguiente", modifier = Modifier.size(30.dp), tint = Color.White) },
                        onClick = { viewModel.next() }
                    )
                    // Repetir (cicla entre 3 modos)
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        PlayerControlBtn(
                            icon = {
                                when (repeatMode) {
                                    PlayerViewModel.RepeatMode.ONE ->
                                        Icon(Icons.Rounded.RepeatOne, "Repetir uno", modifier = Modifier.size(22.dp), tint = NeonCyan)
                                    PlayerViewModel.RepeatMode.ALL ->
                                        Icon(Icons.Rounded.Repeat, "Repetir todo", modifier = Modifier.size(22.dp), tint = NeonCyan)
                                    PlayerViewModel.RepeatMode.NONE ->
                                        Icon(Icons.Rounded.Settings, "Calidad", modifier = Modifier.size(22.dp), tint = Color.White.copy(0.5f))
                                }
                            },
                            onClick = { if (calidades.isNotEmpty()) expanded = true else viewModel.toggleRepeat() }
                        )
                        
                        // Menú de Calidad flotante
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(CardBg).border(1.dp, NeonCyan.copy(0.2f), RoundedCornerShape(8.dp))
                        ) {
                            Text("SELECCIONAR CALIDAD", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                            calidades.forEach { calidad ->
                                DropdownMenuItem(
                                    text = { Text(calidad.nombre, color = Color.White, fontSize = 13.sp) },
                                    leadingIcon = { Icon(if(calidad.esAudio) Icons.Rounded.MusicNote else Icons.Rounded.Videocam, null, tint = NeonCyan.copy(0.6f), modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        viewModel.reproducirSeleccionado(calidad.url)
                                        expanded = false
                                    }
                                )
                            }
                            HorizontalDivider(color = Color.White.copy(0.1f))
                            DropdownMenuItem(
                                text = { Text("Cambiar Repetición", color = Color.Gray, fontSize = 12.sp) },
                                onClick = { viewModel.toggleRepeat(); expanded = false }
                            )
                        }
                    }
                }

                if (mediaError != null) {
                    Text("⚠️ $mediaError", color = Color(0xFFFF6B6B), fontSize = 11.sp)
                }
            }

            if (cargando) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = NeonCyan, trackColor = Color.Transparent
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

            // ── VISTA DE COLA O RESULTADOS DE BÚSQUEDA ───────────────────────
            AnimatedContent(
                targetState = showQueue,
                label = "queue_toggle",
                transitionSpec = {
                    fadeIn(tween(250)) togetherWith fadeOut(tween(200))
                }
            ) { isShowingQueue ->
                if (isShowingQueue) {
                    QueueView(
                        queue = playQueue,
                        currentIndex = currentQueueIndex,
                        viewModel = viewModel,
                        onClose = { showQueue = false }
                    )
                } else {
                    SearchResultsList(
                        resultados = resultados,
                        viewModel = viewModel,
                        favorites = favorites,
                        currentMedia = currentMedia,
                        onShowOptions = { showPlaylistMenu = it }
                    )
                }
            }
        }

        // ── DIALOGS ────────────────────────────────────────────────────────────

        // Menú de opciones ⋮
        showPlaylistMenu?.let { targetItem ->
            SongOptionsMenu(
                item = targetItem,
                playlists = playlists,
                isFavorite = favorites.any { it.id == targetItem.id },
                onDismiss = { showPlaylistMenu = null },
                onAddToQueue = { viewModel.addToQueue(targetItem); showPlaylistMenu = null },
                onToggleFavorite = { viewModel.toggleFavorite(targetItem); showPlaylistMenu = null },
                onAddToPlaylist = { pl -> viewModel.addTrackToUserPlaylist(pl.id, targetItem); showPlaylistMenu = null },
                onNewPlaylist = { showNewPlaylistDialog = true; showPlaylistMenu = null }
            )
        }

        // Crear nueva lista
        if (showNewPlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showNewPlaylistDialog = false },
                containerColor = SurfaceBg,
                title = { Text("➕ Nueva Lista", color = NeonCyan, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Nombre de la lista", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan, cursorColor = NeonCyan
                        ),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.createUserPlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showNewPlaylistDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) { Text("Crear", color = DarkBg, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showNewPlaylistDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
    }
}

// ── Vista de Cola de Reproducción ─────────────────────────────────────────────

@Composable
private fun QueueView(
    queue: List<com.dulce.play.domain.model.MediaItem>,
    currentIndex: Int,
    viewModel: PlayerViewModel,
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        if (queue.isNotEmpty() && currentIndex < queue.size) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "🎵 Cola · ${queue.size} canciones",
                color = NeonCyan, fontWeight = FontWeight.Black, fontSize = 14.sp
            )
            TextButton(onClick = onClose) { Text("Cerrar", color = Color.Gray) }
        }

        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            itemsIndexed(queue, key = { _, item -> item.id }) { idx, item ->
                val isCurrent = idx == currentIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isCurrent) NeonCyan.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable { viewModel.playMedia(item, autoPlay = true) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Número / indicador de reproducción
                    Box(
                        modifier = Modifier.width(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCurrent) {
                            Icon(Icons.Rounded.VolumeUp, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        } else {
                            Text("${idx + 1}", color = Color.White.copy(0.4f), fontSize = 12.sp)
                        }
                    }
                    AsyncImage(
                        model = item.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(0.05f)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, color = if (isCurrent) NeonCyan else Color.White,
                            fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.artist, color = Color.White.copy(0.45f), fontSize = 11.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(
                        onClick = { viewModel.removeFromQueue(item.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.Close, "Quitar", tint = Color.White.copy(0.35f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ── Lista de resultados de búsqueda ──────────────────────────────────────────

@Composable
private fun SearchResultsList(
    resultados: List<com.dulce.play.domain.model.MediaItem>,
    viewModel: PlayerViewModel,
    favorites: List<com.dulce.play.domain.model.MediaItem>,
    currentMedia: com.dulce.play.domain.model.MediaItem,
    onShowOptions: (com.dulce.play.domain.model.MediaItem) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (resultados.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.SearchOff, null, tint = NeonCyan.copy(0.4f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Busca canciones arriba", color = Color.White.copy(0.4f), fontSize = 14.sp)
                    }
                }
            }
        }
        items(resultados, key = { it.id }) { item ->
            val isCurrentItem = item.id == currentMedia.id
            val isFav = favorites.any { it.id == item.id }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isCurrentItem) NeonCyan.copy(alpha = 0.07f) else Color.Transparent)
                    .border(
                        width = if (isCurrentItem) 1.dp else 0.dp,
                        color = if (isCurrentItem) NeonCyan.copy(0.3f) else Color.Transparent,
                        shape = RoundedCornerShape(0.dp)
                    )
                    .clickable { viewModel.playMedia(item, autoPlay = true) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    AsyncImage(
                        model = item.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.size(62.dp, 46.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(0.04f)),
                        contentScale = ContentScale.Crop
                    )
                    if (isCurrentItem) {
                        Box(
                            Modifier.size(62.dp, 46.dp).clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.VolumeUp, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, color = if (isCurrentItem) NeonCyan else Color.White,
                        fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.artist, color = Color.White.copy(0.45f), fontSize = 11.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                // Favorito rápido
                IconButton(onClick = { viewModel.toggleFavorite(item) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        "Fav", tint = if (isFav) Color(0xFFFF4081) else Color.White.copy(0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                // ⋮ Opciones
                IconButton(onClick = { onShowOptions(item) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.MoreVert, "Opciones", tint = Color.White.copy(0.45f), modifier = Modifier.size(18.dp))
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.04f), thickness = 0.5.dp)
        }
    }
}

// ── Menú de opciones de canción ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongOptionsMenu(
    item: com.dulce.play.domain.model.MediaItem,
    playlists: List<LocalStorage.DulcePlaylist>,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (LocalStorage.DulcePlaylist) -> Unit,
    onNewPlaylist: () -> Unit
) {
    var showPlaylistPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            // Cabecera
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = item.coverUrl, contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(0.05f)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.artist, color = NeonCyan.copy(0.8f), fontSize = 11.sp, maxLines = 1)
                }
            }
            HorizontalDivider(color = Color.White.copy(0.08f))

            if (!showPlaylistPicker) {
                // Opciones principales
                SongOptionItem(icon = Icons.Rounded.AddToQueue, label = "Añadir a Cola", tint = NeonCyan, onClick = onAddToQueue)
                SongOptionItem(
                    icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    label = if (isFavorite) "Quitar de Favoritos" else "❤️ Añadir a Favoritos",
                    tint = if (isFavorite) Color(0xFFFF4081) else Color.White.copy(0.8f),
                    onClick = onToggleFavorite
                )
                SongOptionItem(icon = Icons.Rounded.LibraryAdd, label = "Añadir a Lista...", tint = NeonPurple, onClick = { showPlaylistPicker = true })
            } else {
                // Selector de listas
                Text(
                    "Elige una lista", color = NeonPurple, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                if (playlists.isEmpty()) {
                    Text("No tienes listas creadas aún.", color = Color.White.copy(0.4f), modifier = Modifier.padding(horizontal = 20.dp))
                }
                playlists.forEach { pl ->
                    SongOptionItem(
                        icon = Icons.Rounded.PlaylistAdd,
                        label = pl.name,
                        tint = Color.White.copy(0.8f),
                        onClick = { onAddToPlaylist(pl) }
                    )
                }
                SongOptionItem(icon = Icons.Rounded.Add, label = "➕ Nueva Lista...", tint = NeonCyan, onClick = onNewPlaylist)
                SongOptionItem(icon = Icons.Rounded.ArrowBack, label = "Volver", tint = Color.Gray, onClick = { showPlaylistPicker = false })
            }
        }
    }
}

@Composable
private fun SongOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = tint, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PlayerControlBtn(
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(Color.White.copy(alpha = 0.06f), CircleShape)
    ) { icon() }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
