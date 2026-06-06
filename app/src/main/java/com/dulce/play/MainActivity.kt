package com.dulce.play

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.dulce.play.domain.model.MediaType
import androidx.compose.foundation.basicMarquee
import com.dulce.play.ui.auth.AuthScreen
import com.dulce.play.ui.auth.WelcomeScreen
import com.dulce.play.ui.explore.ExploreScreen
import com.dulce.play.ui.iptv.IPTVScreen
import com.dulce.play.ui.library.LibraryScreen
import com.dulce.play.ui.player.PlayerScreen
import com.dulce.play.ui.player.PlayerViewModel
import com.dulce.play.ui.components.*
import com.dulce.play.ui.assistant.AssistantFloatingButton
import com.dulce.play.ui.theme.*
import com.dulce.play.ui.settings.SettingsScreen
import com.dulce.play.ui.intelligence.IntelligentProfileScreen
import androidx.activity.compose.BackHandler

enum class DulceScreen { WELCOME, AUTH, INICIO, IPTV, PERFIL, CUENTA, PLAYER, LIBRARY }

class MainActivity : ComponentActivity() {
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val activeTheme by playerViewModel.activeTheme.collectAsState()
            val accentColor by playerViewModel.accentColor.collectAsState()
            val isExtremeSaver by playerViewModel.extremeBatterySaver.collectAsState()
            var currentScreen by remember { mutableStateOf(DulceScreen.WELCOME) }
            val currentAccount by playerViewModel.currentAccount.collectAsState()
            val brightness by playerViewModel.appBrightness.collectAsState()
            val isEasyMode by playerViewModel.isEasyMode.collectAsState()
            val particles by playerViewModel.particles.collectAsState()
            var showExitDialog by remember { mutableStateOf(false) }

            BackHandler(enabled = true) { showExitDialog = true }

            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = { Text("¿Te vas? 😔", fontWeight = FontWeight.Black) },
                    text = { Text("¿Quieres detener la música o prefieres que siga sonando en segundo plano?") },
                    confirmButton = { TextButton(onClick = { finish() }) { Text("SALIR", color = Color.Red) } },
                    dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("SEGUIR", color = accentColor) } },
                    containerColor = ElegantBlack, titleContentColor = Color.White, textContentColor = Color.Gray
                )
            }

            MyApplicationTheme(activeTheme = activeTheme, overrideAccent = accentColor, isExtremeSaver = isExtremeSaver) {
                LaunchedEffect(currentAccount) {
                    if (currentAccount == null) currentScreen = DulceScreen.WELCOME
                    else if (currentScreen == DulceScreen.WELCOME || currentScreen == DulceScreen.AUTH) currentScreen = DulceScreen.INICIO
                }
                LaunchedEffect(brightness) { window.attributes = window.attributes.apply { screenBrightness = brightness } }

                Box(modifier = Modifier.fillMaxSize().background(if (isExtremeSaver) Color.Black else ElegantBlack)) {
                    if (!isExtremeSaver) {
                        CosmicPlasmaBackground()
                        ParticleField(particles = particles, color = accentColor.copy(alpha = 0.1f))
                    }

                    Scaffold(
                        containerColor = Color.Transparent,
                        topBar = { if (currentScreen !in listOf(DulceScreen.WELCOME, DulceScreen.AUTH, DulceScreen.PLAYER) && !isEasyMode) DulceSearchTopBar(playerViewModel) },
                        bottomBar = {
                            if (currentScreen !in listOf(DulceScreen.WELCOME, DulceScreen.AUTH) && !isEasyMode) {
                                Column {
                                    if (currentScreen != DulceScreen.PLAYER) MiniPlayerControl(playerViewModel) { currentScreen = DulceScreen.PLAYER }
                                    MainBottomNavBar(currentScreen) { currentScreen = it }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            AnimatedContent(targetState = currentScreen, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }) { screen ->
                                when (screen) {
                                    DulceScreen.WELCOME -> WelcomeScreen({ currentScreen = DulceScreen.AUTH }, { currentScreen = DulceScreen.AUTH })
                                    DulceScreen.AUTH -> AuthScreen(playerViewModel) { currentScreen = DulceScreen.INICIO }
                                    DulceScreen.INICIO -> ExploreScreen(playerViewModel) { currentScreen = DulceScreen.PLAYER }
                                    DulceScreen.IPTV -> IPTVScreen(playerViewModel) { currentScreen = DulceScreen.PLAYER }
                                    DulceScreen.PERFIL -> IntelligentProfileScreen()
                                    DulceScreen.CUENTA -> SettingsScreen(playerViewModel)
                                    DulceScreen.PLAYER -> PlayerScreen(playerViewModel) { currentScreen = DulceScreen.INICIO }
                                    DulceScreen.LIBRARY -> LibraryScreen(playerViewModel) { currentScreen = DulceScreen.PLAYER }
                                }
                            }
                        }
                    }

                    if (currentScreen !in listOf(DulceScreen.WELCOME, DulceScreen.AUTH)) {
                        AssistantFloatingButton(playerViewModel, Modifier.align(Alignment.BottomEnd)) { currentScreen = DulceScreen.PLAYER }
                    }
                    if (playerViewModel.searchOverlayActive.collectAsState().value && !isEasyMode) DulceSearchOverlay(playerViewModel, { currentScreen = DulceScreen.PLAYER })
                }
            }
        }
    }
}

@Composable
fun MainBottomNavBar(active: DulceScreen, onSelected: (DulceScreen) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 24.dp, vertical = 12.dp)) {
        GlassBox(cornerRadius = 32.dp, modifier = Modifier.fillMaxWidth().height(68.dp).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                NavBarItem(Icons.Rounded.Home, "Inicio", active == DulceScreen.INICIO) { onSelected(DulceScreen.INICIO) }
                NavBarItem(Icons.Rounded.LiveTv, "IPTV", active == DulceScreen.IPTV) { onSelected(DulceScreen.IPTV) }
                NavBarItem(Icons.Rounded.Psychology, "MIND", active == DulceScreen.PERFIL) { onSelected(DulceScreen.PERFIL) }
                NavBarItem(Icons.Rounded.Person, "Cuenta", active == DulceScreen.CUENTA) { onSelected(DulceScreen.CUENTA) }
            }
        }
    }
}

@Composable
fun RowScope.NavBarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f))
    Column(modifier = Modifier.weight(1f).clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MiniPlayerControl(vm: PlayerViewModel, onClick: () -> Unit) {
    val media by vm.currentMedia.collectAsState(); val playing by vm.isPlaying.collectAsState(); val prog by vm.playbackProgress.collectAsState()
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(60.dp).clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(0.8f)).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(20.dp)).clickable { onClick() }) {
        Box(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(prog).height(2.dp).background(MaterialTheme.colorScheme.primary))
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = media.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) { Text(media.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee()); Text(media.artist, color = Color.White.copy(0.6f), fontSize = 12.sp, maxLines = 1) }
            IconButton(onClick = { vm.prev() }) { Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White) }
            IconButton(onClick = { vm.togglePlay() }, modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.Black) }
            IconButton(onClick = { vm.next() }) { Icon(Icons.Rounded.SkipNext, null, tint = Color.White) }
        }
    }
}
