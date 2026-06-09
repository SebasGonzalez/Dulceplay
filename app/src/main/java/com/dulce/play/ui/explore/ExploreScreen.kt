package com.dulce.play.ui.explore

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dulce.play.domain.model.MediaItem
import com.dulce.play.domain.model.MediaType
import com.dulce.play.ui.components.GlassBox
import com.dulce.play.ui.player.PlayerViewModel
import com.dulce.play.ui.theme.*

@Composable
fun ExploreScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    onNavigateToPlayer: () -> Unit = {}
) {
    val topCO by viewModel.topColombia.collectAsState()
    val topMX by viewModel.topMexico.collectAsState()
    val topGL by viewModel.topGlobal.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // --- Greeting ---
        Text(
            text = "HOLA, ${currentProfile.name.uppercase()}",
            fontSize = 12.sp,
            color = AccentCyan,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Explora lo Mejor",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // --- Sections ---
        TopChartRow("Éxitos Colombia 🇨🇴", topCO, viewModel, onNavigateToPlayer)
        Spacer(modifier = Modifier.height(24.dp))
        TopChartRow("Tendencias México 🇲🇽", topMX, viewModel, onNavigateToPlayer)
        Spacer(modifier = Modifier.height(24.dp))
        TopChartRow("Global Hits 🌎", topGL, viewModel, onNavigateToPlayer)
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun TopChartRow(
    title: String,
    items: List<MediaItem>,
    viewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryNeon, strokeWidth = 2.dp)
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items) { item ->
                    TrendCard(item) {
                        viewModel.playMedia(item)
                        onNavigateToPlayer()
                    }
                }
            }
        }
    }
}

@Composable
fun TrendCard(item: MediaItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Play overlay
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.artist,
            color = AccentCyan,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
