package com.dulce.play

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dulce.play.ui.auth.AuthScreen
import com.dulce.play.ui.explore.ExploreScreen
import com.dulce.play.ui.iptv.IPTVScreen
import com.dulce.play.ui.library.LibraryScreen
import com.dulce.play.ui.player.PlayerScreen
import com.dulce.play.ui.player.PlayerViewModel
import com.dulce.play.ui.components.CosmicPlasmaBackground
import com.dulce.play.ui.components.GlassBox
import com.dulce.play.ui.components.ParticleField
import com.dulce.play.ui.components.EasyModeScreen
import com.dulce.play.ui.assistant.AssistantFloatingButton
import com.dulce.play.ui.theme.MyApplicationTheme
import com.dulce.play.ui.theme.PrimaryNeon
import com.dulce.play.ui.theme.AccentCyan
import com.dulce.play.ui.theme.Typography
import com.dulce.play.ui.settings.SettingsScreen
import com.dulce.play.ui.components.DulceSearchTopBar
import com.dulce.play.ui.components.DulceSearchOverlay

enum class DulceScreen {
    AUTH, EXPLORE, IPTV, PLAYER, LIBRARY, SETTINGS
}

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MainContent()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainContent() {
        val activeTheme by playerViewModel.activeTheme.collectAsState()
        MyApplicationTheme(activeTheme = activeTheme) {
            val particles by playerViewModel.particles.collectAsState()
            var currentScreen by remember { mutableStateOf(DulceScreen.AUTH) }
            val isPlaying by playerViewModel.isPlaying.collectAsState()
            val currentMedia by playerViewModel.currentMedia.collectAsState()
            val isEasyMode by playerViewModel.isEasyMode.collectAsState()
            val isSearchOverlayActive by playerViewModel.searchOverlayActive.collectAsState()

            Box(modifier = Modifier.fillMaxSize()) {
                // --- Atmospheric Animated Plasma + Floating Particle Backdrops ---
                CosmicPlasmaBackground()
                ParticleField(
                    particles = particles,
                    color = AccentCyan.copy(alpha = 0.18f)
                )

                // --- Edge-to-Edge Safe Area Content Viewport ---
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (currentScreen != DulceScreen.AUTH && currentScreen != DulceScreen.PLAYER && !isEasyMode) {
                            DulceSearchTopBar(viewModel = playerViewModel)
                        }
                    },
                    bottomBar = {
                        if (currentScreen != DulceScreen.AUTH && !isEasyMode) {
                            FloatingBottomNavBar(
                                activeScreen = currentScreen,
                                isPlaying = isPlaying,
                                activeTitle = currentMedia.title,
                                activeArtist = currentMedia.artist,
                                onScreenSelected = { currentScreen = it }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (isEasyMode) {
                            EasyModeScreen(
                                viewModel = playerViewModel,
                                onNavigateToPlayer = { currentScreen = DulceScreen.PLAYER }
                            )
                        } else {
                            Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                                when (screen) {
                                    DulceScreen.AUTH -> {
                                        AuthScreen(
                                            viewModel = playerViewModel,
                                            onAuthSuccess = { currentScreen = DulceScreen.EXPLORE }
                                        )
                                    }
                                    DulceScreen.EXPLORE -> {
                                        ExploreScreen(
                                            viewModel = playerViewModel,
                                            onNavigateToPlayer = { currentScreen = DulceScreen.PLAYER }
                                        )
                                    }
                                    DulceScreen.IPTV -> {
                                        IPTVScreen(
                                            viewModel = playerViewModel,
                                            onNavigateToPlayer = { currentScreen = DulceScreen.PLAYER }
                                        )
                                    }
                                    DulceScreen.PLAYER -> {
                                        PlayerScreen(
                                            viewModel = playerViewModel,
                                            onBack = { currentScreen = DulceScreen.EXPLORE }
                                        )
                                    }
                                    DulceScreen.LIBRARY -> {
                                        LibraryScreen(
                                            viewModel = playerViewModel,
                                            onNavigateToPlayer = { currentScreen = DulceScreen.PLAYER }
                                        )
                                    }
                                    DulceScreen.SETTINGS -> {
                                        SettingsScreen(
                                            viewModel = playerViewModel
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Global floating companion orb ---
                if (currentScreen != DulceScreen.AUTH) {
                    AssistantFloatingButton(
                        viewModel = playerViewModel,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }

                // --- Global Dulce-Search Overlay ---
                if (isSearchOverlayActive && currentScreen != DulceScreen.AUTH && !isEasyMode) {
                    DulceSearchOverlay(
                        viewModel = playerViewModel,
                        onNavigateToPlayer = { currentScreen = DulceScreen.PLAYER }
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingBottomNavBar(
    activeScreen: DulceScreen,
    isPlaying: Boolean,
    activeTitle: String,
    activeArtist: String,
    onScreenSelected: (DulceScreen) -> Unit
) {
    // Elegant floating glass bar spanning with clean paddings
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars) // Safeguards bottom gesture bars overlap
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Mini Floating Stream Controller Overlay ---
        AnimatedVisibility(
            visible = activeScreen != DulceScreen.PLAYER,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            GlassBox(
                cornerRadius = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clickable { onScreenSelected(DulceScreen.PLAYER) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(PrimaryNeon, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.MusicNote else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = activeTitle,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = activeArtist,
                                color = AccentCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isPlaying) {
                            Text(
                                text = "SONANDO",
                                fontSize = 8.sp,
                                fontFamily = Typography.labelMedium.fontFamily,
                                color = PrimaryNeon,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Abrir reproductor completo",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // --- Core Navigation Ribbon ---
        GlassBox(
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(
                    icon = Icons.Rounded.Explore,
                    label = "Explorar",
                    active = activeScreen == DulceScreen.EXPLORE,
                    onClick = { onScreenSelected(DulceScreen.EXPLORE) }
                )

                NavBarItem(
                    icon = Icons.Rounded.FeaturedPlayList,
                    label = "Retro Player",
                    active = activeScreen == DulceScreen.PLAYER,
                    onClick = { onScreenSelected(DulceScreen.PLAYER) }
                )

                NavBarItem(
                    icon = Icons.Rounded.LiveTv,
                    label = "IPTV Sat",
                    active = activeScreen == DulceScreen.IPTV,
                    onClick = { onScreenSelected(DulceScreen.IPTV) }
                )

                NavBarItem(
                    icon = Icons.Rounded.FolderCopy,
                    label = "Biblioteca",
                    active = activeScreen == DulceScreen.LIBRARY,
                    onClick = { onScreenSelected(DulceScreen.LIBRARY) }
                )

                NavBarItem(
                    icon = Icons.Rounded.Settings,
                    label = "Ajustes",
                    active = activeScreen == DulceScreen.SETTINGS,
                    onClick = { onScreenSelected(DulceScreen.SETTINGS) }
                )
            }
        }
    }
}

@Composable
fun RowScope.NavBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1.12f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tab_scale"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) PrimaryNeon else Color.White.copy(alpha = 0.45f),
            modifier = Modifier
                .size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (active) Color.White else Color.White.copy(alpha = 0.45f),
            fontSize = 9.sp,
            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}
