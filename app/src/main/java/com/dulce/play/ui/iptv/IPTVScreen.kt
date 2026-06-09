package com.dulce.play.ui.iptv

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dulce.play.domain.model.IPTVChannel
import com.dulce.play.ui.components.GlassBox
import com.dulce.play.ui.player.PlayerViewModel
import com.dulce.play.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun IPTVScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    onNavigateToPlayer: () -> Unit = {}
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val coroutineScope = rememberCoroutineScope()

    val currentMedia by viewModel.currentMedia.collectAsState()
    val activeIPTVChannel by viewModel.activeIPTVChannel.collectAsState()
    val customIPTVPlaylists by viewModel.customIPTVPlaylists.collectAsState()
    val allChannels = viewModel.getAllIPTVChannels()

    // Grouping channels by category automatically
    val groupedChannels = remember(allChannels) {
        allChannels.groupBy { it.group }
    }

    // Interactive expansion states for categorized groups
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    // Dialog & overlay states
    var showImportDialog by remember { mutableStateOf(false) }
    var importTabState by remember { mutableStateOf(0) } // 0: Paste, 1: Remote URL, 2: Document File
    var playlistName by remember { mutableStateOf("") }
    var m3uBody by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var isWebLoading by remember { mutableStateOf(false) }

    // Xtream Codes states
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }
    var showXtreamPanel by remember { mutableStateOf(false) }
    var isXtreamLoading by remember { mutableStateOf(false) }

    // Floating breathing glow animation for sintonizing active channel
    val infiniteTransition = rememberInfiniteTransition(label = "iptv_breathing_glow")
    val liveGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_wave"
    )

    // Android Document storage document picker for local M3U/M3U8 list loading
    val localFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val m3uText = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (m3uText.trim().isNotEmpty()) {
                    var parsedTitle = "Lista Almacenamiento Local"
                    // Extract a readable file name from Uri if possible
                    uri.path?.substringAfterLast("/")?.let { extracted ->
                        if (extracted.endsWith(".m3u") || extracted.endsWith(".m3u8")) {
                            parsedTitle = extracted.replace(".m3u8", "").replace(".m3u", "").replace("_", " ")
                        }
                    }
                    viewModel.importM3UPlaylist(parsedTitle, m3uText)
                    Toast.makeText(context, "¡Lista local '$parsedTitle' cargada exitosamente!", Toast.LENGTH_LONG).show()
                    showImportDialog = false
                    playlistName = ""
                    m3uBody = ""
                } else {
                    Toast.makeText(context, "El archivo seleccionado está vacío.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al leer archivo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // --- IPTV Header Session Banner ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RECEPTOR TELEVISIÓN SATELITAL (IPTV)",
                    fontSize = 11.sp,
                    fontFamily = Typography.labelMedium.fontFamily,
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Portal de Emisiones",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            IconButton(
                onClick = { showImportDialog = true },
                modifier = Modifier
                    .background(PrimaryNeon.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, PrimaryNeon, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Cargar listas IPTV",
                    tint = Color.White
                )
            }
        }

        // --- Active Channel Theatre stage ---
        activeIPTVChannel?.let { activeCh ->
            GlassBox(
                cornerRadius = 20.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.dp, CardGlassBorder, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ambient Cine stage
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryNeon.copy(alpha = 0.15f))
                                    .border(1.5.dp, PrimaryNeon, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tv,
                                    contentDescription = null,
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "SINTONIZANDO SEÑAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Typography.labelMedium.fontFamily,
                                color = AccentCyan
                            )
                            Text(
                                text = activeCh.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onNavigateToPlayer() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.AspectRatio,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PANTALLA COMPLETA / CINE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(LiveRed.copy(alpha = liveGlowAlpha), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Categoría: ${activeCh.group}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = activeCh.country,
                            color = AccentCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Typography.labelMedium.fontFamily
                        )
                    }
                }
            }
        }

        // --- Xtream Codes Expansion Configuration Board ---
        GlassBox(
            cornerRadius = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.CloudSync, contentDescription = null, tint = AccentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PORTAL XTREAM CODES",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Typography.labelMedium.fontFamily,
                            letterSpacing = 1.sp
                        )
                    }

                    Button(
                        onClick = { showXtreamPanel = !showXtreamPanel },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showXtreamPanel) Color.White.copy(alpha = 0.12f) else PrimaryNeon
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (showXtreamPanel) "CERRAR" else "ABRIR APIS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                AnimatedVisibility(visible = showXtreamPanel) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        TextField(
                            value = xtreamServer,
                            onValueChange = { xtreamServer = it },
                            placeholder = { Text("https://portal-servidor.online:8080", color = TextSecondary) },
                            label = { Text("URL de Servidor Xtream", color = AccentCyan) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.44f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.22f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = PrimaryNeon,
                                unfocusedIndicatorColor = Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TextField(
                                value = xtreamUser,
                                onValueChange = { xtreamUser = it },
                                label = { Text("Usuario", color = AccentCyan) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Black.copy(alpha = 0.44f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.22f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = xtreamPass,
                                onValueChange = { xtreamPass = it },
                                label = { Text("Contraseña", color = AccentCyan) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Black.copy(alpha = 0.44f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.22f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (xtreamServer.trim().isEmpty() || xtreamUser.trim().isEmpty() || xtreamPass.trim().isEmpty()) {
                                    Toast.makeText(context, "Por favor completa todos los credenciales Xtream.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isXtreamLoading = true
                                viewModel.importXtreamCodes(
                                    serverUrl = xtreamServer,
                                    username = xtreamUser,
                                    password = xtreamPass,
                                    onSuccess = {
                                        isXtreamLoading = false
                                        showXtreamPanel = false
                                        xtreamServer = ""
                                        xtreamUser = ""
                                        xtreamPass = ""
                                        Toast.makeText(context, "Sincronizado con Xtream Codes. ¡Categorías cargadas!", Toast.LENGTH_LONG).show()
                                    },
                                    onError = { error ->
                                        isXtreamLoading = false
                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = !isXtreamLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isXtreamLoading) {
                                CircularProgressIndicator(color = CosmicBlack, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CONECTANDO A SATÉLITES...", color = CosmicBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("VINCULAR PORTAL XTREAM", color = CosmicBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- Categorized Directories Lists ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DIRECTORIO CATEGORIZADO DE TV (${allChannels.size} EMISORAS)",
                fontSize = 11.sp,
                fontFamily = Typography.labelMedium.fontFamily,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (groupedChannels.isEmpty()) {
            GlassBox(
                cornerRadius = 20.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Biblioteca Satelital vacía",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Importa una lista M3U o vincula tu portal Xtream.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedChannels.forEach { (groupName, channels) ->
                    val isExpanded = expandedCategories[groupName] ?: false

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, CardGlassBorder, RoundedCornerShape(16.dp))
                    ) {
                        // Category Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedCategories[groupName] = !isExpanded }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(AccentCyan.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MovieFilter,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = groupName.uppercase(),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "${channels.size} señales activas",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                                tint = Color.White
                            )
                        }

                        // Child TV Channels listing inside that Category
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                channels.forEach { channel ->
                                    val isCurrentActive = activeIPTVChannel?.id == channel.id
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.playIPTVChannel(channel)
                                                onNavigateToPlayer()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Channel Logo Thumbnail with Coil AsyncImage loading
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black)
                                                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (channel.logoUrl != "globe_logo" && channel.logoUrl.startsWith("http")) {
                                                AsyncImage(
                                                    model = channel.logoUrl,
                                                    contentDescription = "Logo de ${channel.name}",
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Text(
                                                    text = channel.name.take(2).uppercase(),
                                                    color = PrimaryNeon,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.sp,
                                                    fontFamily = Typography.labelMedium.fontFamily
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = channel.name,
                                                color = if (isCurrentActive) PrimaryNeon else Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = channel.country,
                                                color = TextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.playIPTVChannel(channel)
                                                onNavigateToPlayer()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isCurrentActive) Icons.Rounded.PauseCircleFilled else Icons.Rounded.PlayCircleFilled,
                                                contentDescription = "Sintonizar canal",
                                                tint = if (isCurrentActive) LiveRed else AccentCyan
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Custom Three-Way Import M3U Overmorphic Sheet ---
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isWebLoading) showImportDialog = false
                },
                title = {
                    Text(
                        text = "AÑADIR LISTA IPTV PREMIUM",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Typography.labelMedium.fontFamily
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Dialog Segment Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("TEXTO", "URL WEB", "ARCHIVO").forEachIndexed { index, label ->
                                val tabActive = importTabState == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (tabActive) PrimaryNeon else Color.Transparent)
                                        .clickable { importTabState = index }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (tabActive) Color.White else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = Typography.labelMedium.fontFamily
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        when (importTabState) {
                            0 -> { // TEXTO COPIADO Y PEGADO
                                TextField(
                                    value = playlistName,
                                    onValueChange = { playlistName = it },
                                    label = { Text("Nombre de la Lista", color = AccentCyan) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Black,
                                        unfocusedContainerColor = Color.Black.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextField(
                                    value = m3uBody,
                                    onValueChange = { m3uBody = it },
                                    placeholder = { Text("#EXTM3U\n#EXTINF:-1 group-title=\"Premium\" tvg-logo=\"http://logo.url\",Canal 1\nhttp://flujo.stream.m3u8", color = TextSecondary) },
                                    label = { Text("Contenido M3U / M3U8", color = AccentCyan) },
                                    minLines = 4,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Black,
                                        unfocusedContainerColor = Color.Black.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            1 -> { // ENLACE HTTP DESCARGABLE
                                TextField(
                                    value = playlistName,
                                    onValueChange = { playlistName = it },
                                    label = { Text("Nombre de la Lista", color = AccentCyan) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Black,
                                        unfocusedContainerColor = Color.Black.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextField(
                                    value = m3uUrl,
                                    onValueChange = { m3uUrl = it },
                                    placeholder = { Text("http://proveedor-iptv.com/get.php?auth=...", color = TextSecondary) },
                                    label = { Text("URL Directa M3U / M3U8", color = AccentCyan) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Black,
                                        unfocusedContainerColor = Color.Black.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            2 -> { // SELECCION DE ARCHIVO LOCAL DIRECTO DEL DISPOSITIVO
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .border(1.dp, CardGlassBorder, RoundedCornerShape(12.dp))
                                        .clickable { localFileLauncher.launch(arrayOf("*/*")) }
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = "Examinar almacenamiento",
                                            tint = PrimaryNeon,
                                            modifier = Modifier.size(34.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Examinar Almacenamiento Interno",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Soporta formatos .m3u y .m3u8",
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (importTabState < 2) {
                        Button(
                            onClick = {
                                if (playlistName.trim().isEmpty()) {
                                    Toast.makeText(context, "Asigna un nombre a la lista.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (importTabState == 0) {
                                    if (m3uBody.trim().isEmpty()) {
                                        Toast.makeText(context, "El texto crudo de la lista está vacío.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.importM3UPlaylist(playlistName, m3uBody)
                                    Toast.makeText(context, "Lista importada correctamente.", Toast.LENGTH_SHORT).show()
                                    showImportDialog = false
                                    playlistName = ""
                                    m3uBody = ""
                                } else {
                                    if (m3uUrl.trim().isEmpty()) {
                                        Toast.makeText(context, "Especifica la URL remota del archivo.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isWebLoading = true
                                    viewModel.loadM3UFromUrl(
                                        name = playlistName,
                                        urlString = m3uUrl,
                                        onSuccess = {
                                            isWebLoading = false
                                            showImportDialog = false
                                            playlistName = ""
                                            m3uUrl = ""
                                            Toast.makeText(context, "Lista web sincronizada con éxito.", Toast.LENGTH_LONG).show()
                                        },
                                        onError = { error ->
                                            isWebLoading = false
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            },
                            enabled = !isWebLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
                        ) {
                            if (isWebLoading) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("DESCARGANDO...", fontSize = 11.sp)
                            } else {
                                Text("IMPORTAR LISTA", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            if (!isWebLoading) {
                                showImportDialog = false
                                playlistName = ""
                                m3uBody = ""
                                m3uUrl = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("CANCELAR", color = Color.White, fontSize = 11.sp)
                    }
                },
                containerColor = MidnightNavy,
                modifier = Modifier.border(1.dp, CardGlassBorder, RoundedCornerShape(24.dp))
            )
        }
    }
}
