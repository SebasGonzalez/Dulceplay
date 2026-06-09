package com.dulce.play.ui.player

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.dulce.play.domain.model.MediaType
import com.dulce.play.domain.model.MediaItem
import com.dulce.play.ui.components.GlassBox
import com.dulce.play.ui.components.ReflectiveVinylCover
import com.dulce.play.ui.theme.*

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val currentMedia by viewModel.currentMedia.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()
    val currentTimeSeconds by viewModel.currentTimeSeconds.collectAsState()
    val isCasting by viewModel.isCasting.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatEnabled by viewModel.repeatEnabled.collectAsState()
    val visualizerBars by viewModel.visualizerBars.collectAsState()
    val mediaError by viewModel.mediaError.collectAsState()

    // Search and local catalog states
    val searchQuery by viewModel.searchQuery.collectAsState()
    val onlineSearchResults by viewModel.onlineSearchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val localMediaList by viewModel.localMediaList.collectAsState()

    // 🔍 LOG DIAGNÓSTICO: ver qué llega a la pantalla
    androidx.compose.runtime.LaunchedEffect(onlineSearchResults) {
        android.util.Log.d("PANTALLA", "📺 onlineSearchResults cambió → ${onlineSearchResults.size} items")
        onlineSearchResults.forEachIndexed { i, item ->
            android.util.Log.d("PANTALLA", "  [$i] title='${item.title}' streamUrl='${item.streamUrl.take(50)}'")
        }
    }

    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0 = Reproductor, 1 = Buscar / Importar
    var showEqualizerPanel by remember { mutableStateOf(false) }
    var jacketOffsetX by remember { mutableFloatStateOf(0f) }

    // Standard Android intuitive storage directory file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // Retrieve display name of the selected file for clean item labeling
            var fileName = "Archivo de Medios"
            val resolver = context.contentResolver
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }

            // Detect if video format to parse accordingly
            val isVideo = fileName.endsWith(".mp4", ignoreCase = true) ||
                          fileName.endsWith(".mkv", ignoreCase = true) ||
                          fileName.endsWith(".mov", ignoreCase = true) ||
                          fileName.endsWith(".avi", ignoreCase = true) ||
                          fileName.endsWith(".wmv", ignoreCase = true) ||
                          fileName.endsWith(".flv", ignoreCase = true)

            viewModel.addLocalMedia(uri, fileName, isVideo)
            activeTab = 0 // Auto focus to player viewport
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Premium Top Header Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }

            // Cinematic Now Playing title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "REPRODUCIENDO",
                    fontSize = 11.sp,
                    fontFamily = Typography.labelMedium.fontFamily,
                    letterSpacing = 2.sp,
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (currentMedia.mediaType) {
                        MediaType.AUDIO -> "Alta Fidelidad ExoPlayer"
                        MediaType.VIDEO -> "Modo Cine Ultra HD"
                        MediaType.IPTV -> "IPTV Satelital Live"
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Light
                )
            }

            // Casting streaming trigger
            IconButton(
                onClick = { viewModel.toggleCast() },
                modifier = Modifier
                    .background(
                        if (isCasting) AccentCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (isCasting) AccentCyan else Color.White.copy(alpha = 0.15f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isCasting) Icons.Default.CastConnected else Icons.Default.Cast,
                    contentDescription = "Transmitir",
                    tint = if (isCasting) AccentCyan else Color.White
                )
            }
        }

        // --- Custom Flat Tab Navigation Pane ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeTab == 0) PrimaryNeon else Color.Transparent)
                    .clickable { activeTab = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.MusicVideo, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("REPRODUCTOR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeTab == 1) PrimaryNeon else Color.Transparent)
                    .clickable { activeTab = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("BUSCAR & IMPORTAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 12.dp))

        // --- Casting Overlay Banner ---
        AnimatedVisibility(
            visible = isCasting,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            GlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SettingsRemote,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text("Transmitiendo en Smart TV", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Chromecast Ultra • 4K HDR", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Button(
                        onClick = { viewModel.toggleCast() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("DESCONECTAR", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // --- Core MediaError Diagnostics Bar ---
        mediaError?.let { err ->
            GlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(1.dp, LiveRed, RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Rounded.ReportProblem, contentDescription = "Error", tint = LiveRed, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("FALLO DE REPRODUCCIÓN", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(err, color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        // --- TAB 0: REPRODUCTOR PRINCIPAL ---
        if (activeTab == 0) {
            // --- Primary Viewport (Dynamic decoding matching chosen Media Type) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                if (jacketOffsetX > 150f) {
                                    viewModel.prev()
                                } else if (jacketOffsetX < -150f) {
                                    viewModel.next()
                                }
                                jacketOffsetX = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                jacketOffsetX += dragAmount.x
                            }
                        )
                    }
                    .offset(x = jacketOffsetX.dp * 0.15f),
                contentAlignment = Alignment.Center
            ) {
                if (currentMedia.mediaType == MediaType.VIDEO || currentMedia.mediaType == MediaType.IPTV) {
                    // Panoramic Cinema Native Video Stage (Connected to real Android ExoPlayer rendering)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = viewModel.exoPlayer
                                    useController = true
                                    setShowNextButton(false)
                                    setShowPreviousButton(false)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    // Audio Rotating Vinyl cover with customizable neon illustrations
                    ReflectiveVinylCover(
                        illustrationType = currentMedia.coverUrl,
                        isPlaying = isPlaying,
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }

            // --- Core Audio Frequency Visualizer ---
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    val barWidth = (size.width / 32)
                    val spacing = 2.dp.toPx()
                    visualizerBars.forEachIndexed { index, heightMultiplier ->
                        val barHeight = size.height * heightMultiplier
                        val startX = index * barWidth + spacing / 2
                        val startY = size.height - barHeight
                        
                        val gradient = Brush.verticalGradient(
                            colors = listOf(AccentCyan, PrimaryNeon)
                        )

                        drawRoundRect(
                            brush = gradient,
                            topLeft = Offset(startX, startY),
                            size = Size(barWidth - spacing, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }

            // --- Metadata Information Card ---
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentMedia.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = currentMedia.artist,
                    fontSize = 15.sp,
                    color = AccentCyan,
                    maxLines = 1,
                    fontWeight = FontWeight.Medium
                )
                if (currentMedia.album.isNotEmpty()) {
                    Text(
                        text = "Álbum: ${currentMedia.album}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // --- Scrub SeekBar Progress Control ---
            Spacer(modifier = Modifier.height(20.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = playbackProgress,
                    onValueChange = { viewModel.seekPercent(it) },
                    colors = SliderDefaults.colors(
                        activeTrackColor = PrimaryNeon,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                        thumbColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Elapsed time
                    Text(
                        text = formatSeconds(currentTimeSeconds),
                        fontSize = 12.sp,
                        fontFamily = Typography.labelMedium.fontFamily,
                        color = TextSecondary
                    )

                    // Technical quality tag info (dynamic format details depending on track genre)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = null,
                            tint = AccentCyan.copy(alpha = 0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentMedia.genre == "Local Storage") "FORMATO CORTE LOCAL • MULTI-CODEC" else "1411 KBPS • FLAC • CO-ACC",
                            fontSize = 10.sp,
                            fontFamily = Typography.labelMedium.fontFamily,
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                    }

                    // Total track duration
                    Text(
                        text = currentMedia.durationText,
                        fontSize = 12.sp,
                        fontFamily = Typography.labelMedium.fontFamily,
                        color = TextSecondary
                    )
                }
            }

            // --- Shuffle, Repeat, Equalizer Controls ---
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.toggleShuffle() }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) PrimaryNeon else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = { showEqualizerPanel = !showEqualizerPanel },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Equalizer,
                        contentDescription = "Equalizer",
                        tint = AccentCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleRepeat() }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatEnabled) PrimaryNeon else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // --- Core Player Remote (Prev, Play, Next) ---
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.prev() },
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Pista Anterior",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                IconButton(
                    onClick = { viewModel.togglePlay() },
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(PrimaryNeon, SecondaryPurple)
                            ),
                            shape = CircleShape
                        )
                        .shadow(12.dp, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                IconButton(
                    onClick = { viewModel.next() },
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Siguiente Pista",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // --- Custom Equalizer Dialog Panel ---
            AnimatedVisibility(
                visible = showEqualizerPanel,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                GlassBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ECOLOGÍA ATMOS PRO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Typography.labelMedium.fontFamily,
                                color = AccentCyan
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { showEqualizerPanel = false }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        val presets = listOf("Por defecto", "Súper Bajo", "Modo Cine", "Electrónica")
                        var selectedPreset by remember { mutableStateOf("Por defecto") }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { prep ->
                                val active = selectedPreset == prep
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) PrimaryNeon else Color.White.copy(alpha = 0.08f))
                                        .border(1.dp, if (active) AccentCyan else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .clickable { selectedPreset = prep }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(prep, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Ajustes de Ganancia Frecuencial", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))

                        val bands = listOf("60 Hz", "230 Hz", "910 Hz", "14 kHz")
                        bands.forEach { bandName ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(bandName, color = Color.White, fontSize = 11.sp, fontFamily = Typography.labelMedium.fontFamily)
                                Slider(
                                    value = if (selectedPreset == "Súper Bajo" && bandName == "60 Hz") 0.9f else 0.5f,
                                    onValueChange = {},
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = AccentCyan,
                                        thumbColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp)
                                )
                                Text("+0 dB", color = TextSecondary, fontSize = 10.sp, fontFamily = Typography.labelMedium.fontFamily)
                            }
                        }
                    }
                }
            }
        }

        // --- TAB 1: BUSCAR & CONECTAR CON ALMACENAMIENTO LOCAL ___
        if (activeTab == 1) {
            // --- Elegant Folder Picker Card for Local Storage Audios & Videos ---
            GlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { filePickerLauncher.launch(arrayOf("audio/*", "video/*")) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentCyan.copy(alpha = 0.15f))
                                .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "SINTONIZAR ALMACENAMIENTO",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Usa FLAC, MP3, WAV, M4A, Opus, MP4, MKV, AVI",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // --- Real-time Catalog Search bar ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Buscar canción, artista o álbum...", color = TextSecondary) },
                leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null, tint = AccentCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            // Boton X limpia; botón buscar lanza búsqueda inmediata
                            viewModel.buscarAhora(searchQuery)
                        }) {
                            Icon(imageVector = Icons.Rounded.Search, contentDescription = "Buscar", tint = AccentCyan)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { viewModel.buscarAhora(searchQuery) } // ENTER → YouTube inmediato
                ),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = MidnightNavy.copy(alpha = 0.5f),
                    unfocusedContainerColor = MidnightNavy.copy(alpha = 0.2f),
                    cursorColor = AccentCyan
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // --- Botones de acceso rápido por país ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Botón Colombia 🇨🇴
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.15f),
                                    Color(0xFF003087).copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { viewModel.cargarSeccionColombia() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🇨🇴", fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "COLOMBIA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD700),
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Botón México 🇲🇽
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF006847).copy(alpha = 0.15f),
                                    Color(0xFFCE1126).copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFF006847).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { viewModel.cargarSeccionMexico() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🇲🇽", fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "MÉXICO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4CAF50),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // --- Results / Subsections ---
            if (isSearching) {
                // Interactive Searching Animation
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = AccentCyan, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "BUCEANDO EN REPOSITORIO MUSIC API...",
                        fontSize = 11.sp,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Typography.labelMedium.fontFamily
                    )
                }
            } else if (searchQuery.isNotEmpty() && onlineSearchResults.isEmpty()) {
                // Not found state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Rounded.MusicOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No se encontraron resultados en línea para \"$searchQuery\"", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                // --- ONLINE TRACK SEARCH RESULTS SECTION ---
                if (onlineSearchResults.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Rounded.CloudQueue, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CATÁLOGO DE MÚSICA EN LÍNEA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Typography.labelMedium.fontFamily,
                            color = Color.White
                        )
                    }

                    onlineSearchResults.forEach { onlineTrack ->
                        // Detect if this song title has a corresponding matched file downloaded locally
                        val isLocallyReady = viewModel.isMediaAvailableLocally(onlineTrack.title, onlineTrack.artist)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(
                                    1.dp,
                                    if (isLocallyReady) AccentCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.03f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.playMedia(onlineTrack) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Online Track Cover
                            AsyncImage(
                                model = onlineTrack.coverUrl,
                                contentDescription = "Carátula",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = onlineTrack.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${onlineTrack.artist} • ${onlineTrack.album}",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Glistening local check badge if available in device storage
                                if (isLocallyReady) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.DownloadDone,
                                            contentDescription = null,
                                            tint = AccentCyan,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "DISPONIBLE EN DISCO LOCAL",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = AccentCyan
                                        )
                                    }
                                }
                            }

                            // Control Play Button Action
                            IconButton(
                                onClick = { viewModel.playMedia(onlineTrack) },
                                modifier = Modifier
                                    .background(PrimaryNeon.copy(alpha = 0.1f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Reproducir versión online",
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // --- LOCAL DEVICE STORAGE SCANNED ARCHIVE LIST ---
                // ✅ REGLA DE ORO: Solo se muestra cuando NO hay búsqueda activa
                if (searchQuery.isEmpty() && onlineSearchResults.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Rounded.LibraryMusic, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TU BIBLIOTECA LOCAL (${localMediaList.size} ARCHIVOS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Typography.labelMedium.fontFamily,
                        color = Color.White
                    )
                }

                if (localMediaList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.02f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Rounded.FolderZip, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aún no has sintonizado archivos locales.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    localMediaList.forEach { localTrack ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.playMedia(localTrack) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (localTrack.mediaType == MediaType.VIDEO) LiveRed.copy(alpha = 0.15f)
                                        else AccentCyan.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (localTrack.mediaType == MediaType.VIDEO) Icons.Rounded.Tv else Icons.Rounded.Audiotrack,
                                    contentDescription = null,
                                    tint = if (localTrack.mediaType == MediaType.VIDEO) LiveRed else AccentCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = localTrack.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (localTrack.mediaType == MediaType.VIDEO) "Video Local • Sintonizado" else "Audio Local • Decodificado",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            IconButton(
                                onClick = { viewModel.playMedia(localTrack) },
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Reproducir pista local",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    }
                } // fin if (searchQuery.isEmpty() && onlineSearchResults.isEmpty())
            }
        }
    }
}

private fun formatSeconds(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
