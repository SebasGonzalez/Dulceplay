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
import com.dulce.play.domain.model.MediaItem
import com.dulce.play.domain.model.IPTVChannel
import kotlinx.coroutines.delay

@Composable
fun EasyModeButton(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(color.copy(0.1f)).border(2.dp, color, RoundedCornerShape(20.dp)).clickable { onClick() }.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.size(48.dp).background(color, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.Black) }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text(desc, fontSize = 12.sp, color = Color.White.copy(0.6f))
            }
        }
    }
}

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

    val infiniteTransition = rememberInfiniteTransition(label = "accessibility_pulse")
    val sizePulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Reverse),
        label = "size_pulse"
    )

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Accessibility, null, tint = ElectricBlue, modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("MODO FÁCIL", fontSize = 28.sp, fontWeight = FontWeight.Black, color = ElectricBlue)
        }

        Text("Hola ${currentProfile.name}, usa estos botones gigantes:", color = Color.White, textAlign = TextAlign.Center)

        if (isPlaying) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ElectricBlue.copy(0.2f)).border(2.dp, ElectricBlue, RoundedCornerShape(20.dp)).clickable { onNavigateToPlayer() }.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Rounded.VolumeUp, null, tint = ElectricBlue, modifier = Modifier.size(36.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("REPRODUCIENDO:", fontSize = 11.sp, color = ElectricBlue, fontWeight = FontWeight.Black)
                        Text(currentMedia.title, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().scale(sizePulse).clickable { showVoiceDialog = true }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.8f))) {
            Row(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp).background(ElectricBlue, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Mic, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("HABLAR CON DULCE", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }

        EasyModeButton("1. MÚSICA RELAJANTE", "Sonidos suaves", Icons.Rounded.Spa, Color(0xFF00FFCC)) {
            viewModel.executeSearch("Relaxing")
            onNavigateToPlayer()
        }

        EasyModeButton("2. NOTICIAS EN VIVO", "Canales de TV", Icons.Rounded.LiveTv, ElectricBlue) {
            val ch = viewModel.getAllIPTVChannels().firstOrNull()
            ch?.let { viewModel.playIPTVChannel(it) }
            onNavigateToPlayer()
        }

        EasyModeButton("3. NUESTRA MÚSICA", "Sabor nacional", Icons.Rounded.Audiotrack, PremiumGold) {
            viewModel.executeSearch("Vallenato")
            onNavigateToPlayer()
        }

        Button(onClick = { viewModel.togglePlay() }, colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) Color.Red else Color.Green), modifier = Modifier.fillMaxWidth().height(70.dp)) {
            Text(if (isPlaying) "DETENER" else "REANUDAR", fontSize = 18.sp, fontWeight = FontWeight.Black)
        }

        TextButton(onClick = { viewModel.toggleEasyMode() }) {
            Text("SALIR DEL MODO FÁCIL", color = Color.White.copy(0.5f))
        }
    }

    if (showVoiceDialog) {
        IntelligenceCenterDialog(viewModel, { showVoiceDialog = false }, onNavigateToPlayer, null)
    }
}
