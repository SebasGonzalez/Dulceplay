package com.example.ui.explore

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
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MediaItem
import com.example.domain.model.MediaType
import com.example.ui.components.GlassBox
import com.example.ui.player.PlayerViewModel
import com.example.ui.theme.*

@Composable
fun ExploreScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    onNavigateToPlayer: () -> Unit = {}
) {
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()
    val filteredMediaList = viewModel.getFilteredMediaList()
    val moodPlaylists = viewModel.getMoodPlaylists()

    val countries = listOf("Global", "Colombia", "México", "España", "USA", "Argentina", "Brasil")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // --- Header Section with Profile Greeting ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HOLA, ${currentProfile.name.uppercase()}",
                    fontSize = 12.sp,
                    fontFamily = Typography.labelMedium.fontFamily,
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Descubre DulcePlay",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            // Small profile card status
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (currentProfile.isPremium) AccentCyan else TextSecondary,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentProfile.isPremium) "PRO" else "FREE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Typography.labelMedium.fontFamily,
                        color = Color.White
                    )
                }
            }
        }

        // --- Premium Featured Release Banner (Glassmorphic Hero) ---
        GlassBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(PrimaryNeon.copy(alpha = 0.15f), Color.Transparent),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                            )
                        )
                    }
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .background(AccentCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LANZAMIENTO EXCLUSIVO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = Typography.labelMedium.fontFamily,
                        color = AccentCyan
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Neon Odyssey • Remastered Edition",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Por Retro Synth Lord. El álbum futurista definitivo con códecs de sonido espacial 9.1 Atmos en exclusiva.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                Button(
                    onClick = {
                        // Play premium release track indices immediately
                        val track = filteredMediaList.firstOrNull { it.id == "1" }
                        if (track != null) {
                            viewModel.playMedia(track)
                            onNavigateToPlayer()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ESCUCHAR AHORA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- Elegant Advanced Country Selector (Horizontally Scrollable) ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TENDENCIAS POR PAÍS",
                fontSize = 12.sp,
                fontFamily = Typography.labelMedium.fontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(countries) { country ->
                val active = selectedCountry == country
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) PrimaryNeon else Color.White.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            if (active) AccentCyan else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.selectCountry(country) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = country,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else TextSecondary
                    )
                }
            }
        }

        // --- Top Worldwide / Regional Tracks (List layout) ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Rounded.TrendingUp, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedCountry == "Global") "TOP MUNDIAL EN TIEMPO REAL" else "TOP RECOMENDADO EN ${selectedCountry.uppercase()}",
                    fontSize = 12.sp,
                    fontFamily = Typography.labelMedium.fontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = "VER TODO",
                fontSize = 11.sp,
                color = AccentCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {}
            )
        }

        // Track lists items with beautiful index counts
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filteredMediaList.forEachIndexed { idx, mediaItem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .clickable {
                            viewModel.playMedia(mediaItem)
                            onNavigateToPlayer()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Position Number
                    Text(
                        text = String.format("%02d", idx + 1),
                        color = if (idx == 0) PrimaryNeon else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = Typography.labelMedium.fontFamily,
                        modifier = Modifier.width(32.dp)
                    )

                    // Virtual Custom Cover (Vector/Background styling representation)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = when (mediaItem.coverUrl) {
                                        "colombia" -> listOf(Color(0xFFFFD700), Color(0xFFCE1126))
                                        "mexico" -> listOf(Color(0xFF006847), Color(0xFFCE1126))
                                        "brasil" -> listOf(Color(0xFF009739), Color(0xFFFEDF00))
                                        "ambient" -> listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
                                        "cyberpunk" -> listOf(Color(0xFFFF0D7B), Color(0xFF9013FE))
                                        else -> listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title & Artist Group
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mediaItem.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (mediaItem.isPremium) {
                                Text(
                                    text = "PRO ",
                                    color = AccentCyan,
                                    fontSize = 9.sp,
                                    fontFamily = Typography.labelMedium.fontFamily,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = mediaItem.artist,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Playing stream indicators
                    IconButton(onClick = {
                        viewModel.playMedia(mediaItem)
                        onNavigateToPlayer()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Reproducir pista",
                            tint = AccentCyan
                        )
                    }
                }
            }
        }

        // --- Mood playlists / Smart curation cards ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Rounded.DynamicFeed, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "PLAYLISTS POR ESTADO DE ÁNIMO",
                fontSize = 12.sp,
                fontFamily = Typography.labelMedium.fontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(moodPlaylists) { (title, description) ->
                GlassBox(
                    modifier = Modifier
                        .width(170.dp)
                        .height(115.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                // Select first element matching electro genre/ambient as placeholder
                                val matched = filteredMediaList.firstOrNull() ?: filteredMediaList.first()
                                viewModel.playMedia(matched)
                                onNavigateToPlayer()
                            }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(description, color = TextSecondary, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 3)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
