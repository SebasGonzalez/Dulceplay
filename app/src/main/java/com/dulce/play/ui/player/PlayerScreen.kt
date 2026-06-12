package com.dulce.play.ui.player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val resultados by viewModel.onlineSearchResults.collectAsState()
    val cargando by viewModel.isSearching.collectAsState()
    val calidades by viewModel.listaCalidades.collectAsState()
    val mediaError by viewModel.mediaError.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentMedia by viewModel.currentMedia.collectAsState()

    var textoBusqueda by remember { mutableStateOf("") }
    var mostrarMenuCalidad by remember { mutableStateOf(false) }

    // Abrir automáticamente el menú cuando llegan las calidades
    LaunchedEffect(calidades) {
        if (calidades.isNotEmpty()) {
            mostrarMenuCalidad = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DulcePlay V3.5 🎵", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF6200EA))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF121212))
        ) {

            // 🔍 BUSCADOR
            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = {
                    textoBusqueda = it
                    if (it.length > 2) viewModel.buscarEnYouTube(it)
                },
                label = { Text("Buscar en YouTube...", color = Color.LightGray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.Cyan,
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color.White
                )
            )

            // 🎥 REPRODUCTOR PRINCIPAL
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
            }

            // 📝 Título del medio actual
            if (currentMedia.title.isNotBlank() && currentMedia.title != "Sintonizando DulcePlay") {
                Text(
                    text = currentMedia.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // ⚠️ ERROR
            if (mediaError != null) {
                Text(
                    text = "⚠️ $mediaError",
                    color = Color(0xFFFF6B6B),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }

            // ⚙️ CONTROLES
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Estado de reproducción
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isPlaying) Color(0xFF00BCD4) else Color(0xFF2D2D2D)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.MusicNote else Icons.Filled.PlayArrow,
                        contentDescription = "Estado",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    Button(
                        onClick = {
                            // Si ya hay calidades cargadas, mostrar menú directamente
                            // Si no, solo abrir (el LaunchedEffect abrirá cuando lleguen)
                            mostrarMenuCalidad = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EA)),
                        enabled = !cargando
                    ) {
                        if (cargando && calidades.isEmpty()) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (cargando && calidades.isEmpty()) "Cargando..." else "Calidad ⚙️")
                    }

                    // 🔽 MENÚ DE CALIDADES DESPLEGABLE
                    DropdownMenu(
                        expanded = mostrarMenuCalidad && calidades.isNotEmpty(),
                        onDismissRequest = { mostrarMenuCalidad = false },
                        modifier = Modifier.background(Color(0xFF1E1E1E))
                    ) {
                        // Encabezado del menú
                        Text(
                            text = "Selecciona calidad",
                            color = Color.Cyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                        calidades.forEach { calidad ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (calidad.esAudio) Icons.Filled.Headphones else Icons.Filled.Videocam,
                                        contentDescription = null,
                                        tint = if (calidad.esAudio) Color(0xFF00BCD4) else Color(0xFF9C27B0),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        calidad.nombre,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    viewModel.reproducirSeleccionado(calidad.url)
                                    mostrarMenuCalidad = false
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = { /* Fullscreen Logic */ }) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = "Pantalla Completa", tint = Color.White)
                }
            }

            // ⏱️ INDICADOR DE CARGA / LISTA DE RESULTADOS
            if (cargando) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Cyan,
                    trackColor = Color(0xFF1E1E1E)
                )
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(resultados) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable {
                                viewModel.playMedia(item)
                                // El menú se abrirá automáticamente vía LaunchedEffect cuando lleguen las calidades
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.coverUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp, 60.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    fontSize = 13.sp
                                )
                                Text(
                                    item.artist,
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Reproducir",
                                tint = Color(0xFF6200EA),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
