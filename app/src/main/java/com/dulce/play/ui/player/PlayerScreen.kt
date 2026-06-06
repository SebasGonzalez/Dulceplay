package com.dulce.play.ui.player

import android.net.Uri
import android.content.pm.ActivityInfo
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.dulce.play.domain.model.MediaType
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
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatEnabled by viewModel.repeatEnabled.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val visualizerBars by viewModel.visualizerBars.collectAsState()
    val showLyrics by viewModel.showLyrics.collectAsState()
    val currentLyrics by viewModel.currentLyrics.collectAsState()

    val context = LocalContext.current
    val isVideo = currentMedia.mediaType == MediaType.VIDEO || currentMedia.mediaType == MediaType.IPTV

    LaunchedEffect(currentMedia) {
        val activity = context as? Activity
        activity?.requestedOrientation = if (isVideo) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // --- BLURRED BACKGROUND ---
        if (!isVideo) {
            AsyncImage(
                model = currentMedia.coverUrl,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(80.dp).scale(1.5f)
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
        }

        if (isVideo) {
            AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = viewModel.exoPlayer; useController = true } }, modifier = Modifier.fillMaxSize())
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp).background(Color.Black.copy(0.4f), CircleShape)) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val width = size.width
                        if (change.position.x < width / 2) {
                            viewModel.setAppBrightness((viewModel.appBrightness.value + (dragAmount.y / -1000f)).coerceIn(0.1f, 1f))
                        } else {
                            viewModel.adjustVolume(dragAmount.y / -1000f)
                        }
                    }
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ExpandMore, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    IconButton(onClick = { /* Open EQ */ }) {
                        Icon(Icons.Rounded.GraphicEq, null, tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // --- CONTENT / LYRICS ---
                AnimatedContent(targetState = showLyrics, label = "lyrics") { lyricsActive ->
                    if (lyricsActive) {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp).verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                            Text(currentLyrics, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Row(modifier = Modifier.height(300.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                visualizerBars.forEach { height ->
                                    Box(modifier = Modifier.width(4.dp).fillMaxHeight(height).clip(CircleShape).background(accentColor.copy(alpha = 0.4f)))
                                }
                            }
                            ReflectiveVinylCover(modifier = Modifier.size(260.dp), illustrationType = currentMedia.coverUrl, isPlaying = isPlaying)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                IconButton(onClick = { viewModel.toggleLyrics() }) {
                    Icon(Icons.Rounded.KeyboardArrowUp, null, tint = if (showLyrics) accentColor else Color.White.copy(0.5f))
                    Text("LETRAS", color = if (showLyrics) accentColor else Color.White.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(currentMedia.title, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
                Text(currentMedia.artist, fontSize = 18.sp, color = Color.White.copy(alpha = 0.7f))

                Spacer(modifier = Modifier.height(32.dp))

                // --- PROGRESS BAR ---
                Slider(
                    value = playbackProgress,
                    onValueChange = { viewModel.seekPercent(it) },
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatSeconds(currentTimeSeconds), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(currentMedia.durationText, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- CONTROLS ---
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = { viewModel.toggleShuffle() }) { Icon(Icons.Rounded.Shuffle, null, tint = if (shuffleEnabled) accentColor else Color.White.copy(0.4f)) }
                    IconButton(onClick = { viewModel.skip15(false) }) { Icon(Icons.Rounded.History, null, tint = Color.White) }
                    IconButton(onClick = { viewModel.prev() }) { Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
                    
                    FloatingActionButton(onClick = { viewModel.togglePlay() }, containerColor = accentColor, shape = CircleShape, modifier = Modifier.size(64.dp)) {
                        Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(36.dp))
                    }
                    
                    IconButton(onClick = { viewModel.next() }) { Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
                    IconButton(onClick = { viewModel.skip15(true) }) { Icon(Icons.Rounded.Update, null, tint = Color.White) }
                    IconButton(onClick = { viewModel.toggleRepeat() }) { Icon(Icons.Rounded.Repeat, null, tint = if (repeatEnabled) accentColor else Color.White.copy(0.4f)) }
                }

                Spacer(modifier = Modifier.weight(1.2f))
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    IconButton(onClick = { /* Timer */ }) { Icon(Icons.Rounded.Timer, null, tint = Color.White) }
                    IconButton(onClick = { viewModel.toggleFavorite(currentMedia) }) {
                        Icon(if (viewModel.isFavorite(currentMedia.id)) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (viewModel.isFavorite(currentMedia.id)) Color.Red else Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = { /* Share */ }) { Icon(Icons.Rounded.Share, null, tint = Color.White) }
                }
            }
        }
    }
}

fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
