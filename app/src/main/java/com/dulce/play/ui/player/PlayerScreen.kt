package com.dulce.play.ui.player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val formatos by viewModel.formatosDisponibles.collectAsState()

    var textoBusqueda by remember { mutableStateOf("") }
    var mostrarMenuCalidad by remember { mutableStateOf(false) }
    var modoAudioUnico by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DulcePlay V3.4 🎵", color = Color.White) },
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

            // ⚙️ CONTROLES
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { modoAudioUnico = !modoAudioUnico }) {
                    Icon(
                        imageVector = if (modoAudioUnico) Icons.Filled.Headphones else Icons.Filled.PlayArrow,
                        contentDescription = "Modo",
                        tint = if (modoAudioUnico) Color.Cyan else Color.White
                    )
                }

                Button(
                    onClick = { mostrarMenuCalidad = true },
                    enabled = formatos != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EA))
                ) {
                    Text("Calidad ⚙️")
                }

                IconButton(onClick = { /* Fullscreen Logic */ }) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = "Pantalla Completa", tint = Color.White)
                }
            }

            // 🔽 MENÚ DE CALIDADES (AlertDialog)
            if (mostrarMenuCalidad && formatos != null) {
                AlertDialog(
                    onDismissRequest = { mostrarMenuCalidad = false },
                    title = { Text("Seleccionar Calidad") },
                    text = {
                        LazyColumn {
                            item { Text("Audio", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp)) }
                            items(formatos!!.audio) { calidad ->
                                ListItem(
                                    headlineContent = { Text(calidad.nombre) },
                                    modifier = Modifier.clickable {
                                        viewModel.reproducirConCalidad(calidad.url)
                                        mostrarMenuCalidad = false
                                    }
                                )
                            }
                            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                            item { Text("Video", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp)) }
                            items(formatos!!.video) { calidad ->
                                ListItem(
                                    headlineContent = { Text(calidad.nombre) },
                                    modifier = Modifier.clickable {
                                        viewModel.reproducirConCalidad(calidad.url)
                                        mostrarMenuCalidad = false
                                    }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { mostrarMenuCalidad = false }) { Text("Cerrar") }
                    }
                )
            }

            // ⏱️ LISTA
            if (cargando) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color.Cyan)
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(resultados) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable {
                                viewModel.playMedia(item)
                                mostrarMenuCalidad = true
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = item.coverUrl,
                                contentDescription = null,
                                modifier = Modifier.size(100.dp, 60.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2)
                                Text(item.artist, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
