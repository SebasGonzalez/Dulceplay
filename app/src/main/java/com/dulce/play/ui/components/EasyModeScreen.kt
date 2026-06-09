package com.dulce.play.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dulce.play.ui.player.PlayerViewModel
import com.dulce.play.ui.theme.*
import com.dulce.play.ui.assistant.IntelligenceCenterDialog
import kotlinx.coroutines.delay

@Composable
fun EasyModeScreen(
    viewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentProfile by viewModel.currentProfile.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentMedia by viewModel.currentMedia.collectAsState()
    var showVoiceDialog by remember { mutableStateOf(false) }

    // Pulsing animation for the Speech Accessibility Button
    val infiniteTransition = rememberInfiniteTransition(label = "accessibility_pulse")
    val sizePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "size_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- Grand Accessibility Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Accessibility,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "MODO FÁCIL",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = AccentCyan,
                letterSpacing = 2.sp
            )
        }

        Text(
            text = "Hola ${currentProfile.name}, toca los botones gigantes para usar tu reproductor de forma súper simplificada:",
            fontSize = 18.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // --- Active playing banner ---
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(PrimaryNeon.copy(alpha = 0.2f))
                    .border(2.dp, PrimaryNeon, RoundedCornerShape(20.dp))
                    .clickable { onNavigateToPlayer() }
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VolumeUp,
                        contentDescription = "Sonando",
                        tint = PrimaryNeon,
                        modifier = Modifier.size(36.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "REPRODUCIENDO AHORA:",
                            fontSize = 13.sp,
                            color = PrimaryNeon,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            currentMedia.title,
                            fontSize = 20.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Abrir",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // --- GIANT VOICE ACCESS BUTTON ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(sizePulse)
                .border(3.dp, PrimaryNeon, RoundedCornerShape(24.dp))
                .clickable { showVoiceDialog = true }
                .testTag("easy_voice_assistant_launcher"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(PrimaryNeon, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Hablar",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "HABLAR DE VIVA VOZ\n(TOCAR PARA DAR COMANDOS)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- TRADITIONAL SIMPLIFIED WORKSPACE ITEMS (GIANT TOUCH TARGETS: 76dp) ---
        Text(
            text = "ACCIONES RÁPIDAS DE UN SOLO TOQUE:",
            fontSize = 15.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        // BUTTON 1: MUSIC FOR CONCENTRATION
        GiantAccessibilityButton(
            title = "1. MÚSICA TRANQUILA Y RELAJANTE",
            description = "Música instrumental suave",
            icon = Icons.Rounded.Spa,
            color = Color(0xFF00FFCC),
            onClick = {
                val item = viewModel.getFilteredMediaList().firstOrNull { 
                    it.genre.lowercase().contains("ambient") 
                } ?: viewModel.getFilteredMediaList().firstOrNull()
                item?.let { viewModel.playMedia(it) }
                onNavigateToPlayer()
            }
        )

        // BUTTON 2: LIVE NEWS (IPTV TELESur EN VIVO)
        GiantAccessibilityButton(
            title = "2. SINTONIZAR NOTICIAS EN VIVO",
            description = "Canales informativos regionales",
            icon = Icons.Rounded.LiveTv,
            color = AccentCyan,
            onClick = {
                val ch = viewModel.getAllIPTVChannels().firstOrNull { 
                    it.group.lowercase().contains("noticias") || it.name.lowercase().contains("telesur")
                } ?: viewModel.getAllIPTVChannels().firstOrNull()
                ch?.let { viewModel.playIPTVChannel(it) }
                onNavigateToPlayer()
            }
        )

        // BUTTON 3: FOLKLORE / CULTURA REGIONAL
        GiantAccessibilityButton(
            title = "3. MÚSICA FOLCLÓRICA TRADICIONAL",
            description = "Apreciar la cultura patria",
            icon = Icons.Rounded.Audiotrack,
            color = Color(0xFFFFCC00),
            onClick = {
                val item = viewModel.getFilteredMediaList().firstOrNull { 
                    it.genre.lowercase().contains("folklore") || it.genre.lowercase().contains("cumbia") 
                } ?: viewModel.getFilteredMediaList().firstOrNull()
                item?.let { viewModel.playMedia(it) }
                onNavigateToPlayer()
            }
        )

        // PLAYBACK STOP GIGANTIC PAD
        Button(
            onClick = { viewModel.togglePlay() },
            colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) LiveRed else Color.Green),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .testTag("easy_mode_play_toggle")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = if (isPlaying) "DETENER REPRODUCTOR (PAUSAR)" else "REANUDAR REPRODUCTOR (PLAY)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // EXIT BUTTON
        Button(
            onClick = { viewModel.toggleEasyMode() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
            border = BorderStroke(2.dp, LiveRed),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("easy_mode_exit_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.HighlightOff, null, tint = LiveRed, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("🛑 SALIR DEL MODO FÁCIL (VOLVER AL DISEÑO PREMIUM)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showVoiceDialog) {
        IntelligenceCenterDialog(
            viewModel = viewModel,
            onDismiss = { showVoiceDialog = false }
        )
    }
}

@Composable
fun GiantAccessibilityButton(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.08f))
            .border(2.5.dp, color, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.Black, modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    description,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
