package com.dulce.play.ui.explore

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dulce.play.ui.components.GlassBox
import com.dulce.play.ui.player.PlayerViewModel
import com.dulce.play.ui.theme.*
import com.dulce.play.domain.model.MediaItem
import java.util.Calendar

@Composable
fun ExploreScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    onNavigateToPlayer: () -> Unit = {}
) {
    val currentProfile by viewModel.currentProfile.collectAsState()
    val topColombia by viewModel.topColombia.collectAsState()
    val topMexico by viewModel.topMexico.collectAsState()
    val topGlobal by viewModel.topGlobal.collectAsState()
    val recentPlayed by viewModel.recentPlayed.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val radioStations by viewModel.radioStations.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 120.dp)) {
        DailyGreetingHeader(currentProfile.name, accentColor)

        if (recentPlayed.isNotEmpty()) {
            VibrantSectionHeader("RECIENTES 🕒", "Sigue escuchando", Icons.Rounded.History, accentColor) {}
            LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(recentPlayed) { m -> SmallMediaCard(m) { viewModel.playMedia(m); onNavigateToPlayer() } }
            }
        }

        // --- COLOMBIA SECTIONS ---
        VibrantSectionHeader("LEYENDAS VIVAS 🇨🇴", "Lo mejor de nuestra historia", Icons.Rounded.AutoAwesome, PremiumGold) {
            viewModel.updateSearchQuery("Vallenato Clásicos Oficiales"); viewModel.executeSearch("Vallenato Clásicos Oficiales"); viewModel.setSearchOverlayActive(true)
        }
        
        VibrantSectionHeader("ÉXITOS COLOMBIA 🇨🇴", "Puro sabor nacional", Icons.Rounded.Whatshot, Color(0xFFFFD700)) { 
            viewModel.updateSearchQuery("Salsa Vallenato Colombia éxitos oficiales"); viewModel.executeSearch("Salsa Vallenato Colombia éxitos oficiales"); viewModel.setSearchOverlayActive(true)
        }
        TopChartRow(topColombia, viewModel, onNavigateToPlayer, accentColor)

        VibrantSectionHeader("RADIO EN VIVO 📻", "Emisoras principales", Icons.Rounded.Radio, accentColor) {}
        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(radioStations) { r ->
                Column(modifier = Modifier.width(90.dp).clickable { viewModel.playIPTVChannel(r); onNavigateToPlayer() }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(70.dp).clip(CircleShape).background(Color.White.copy(0.05f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Radio, null, tint = accentColor) }
                    Text(r.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }

        VibrantSectionHeader("MÉXICO LINDO 🇲🇽", "Regional y Corridos Oficiales", Icons.Rounded.TrendingUp, Color(0xFF006847)) { 
            viewModel.updateSearchQuery("Corridos Banda Regional Mexicano oficial"); viewModel.executeSearch("Corridos Banda Regional Mexicano oficial"); viewModel.setSearchOverlayActive(true)
        }
        TopChartRow(topMexico, viewModel, onNavigateToPlayer, accentColor)

        VibrantSectionHeader("TOP 10 GLOBAL 🌎", "Éxitos internacionales", Icons.Rounded.Public, ElectricBlue) { 
            viewModel.updateSearchQuery("Top Global Hits 2024 official complete"); viewModel.executeSearch("Top Global Hits 2024 official complete"); viewModel.setSearchOverlayActive(true)
        }
        TopChartRow(topGlobal, viewModel, onNavigateToPlayer, accentColor)

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun DailyGreetingHeader(name: String, accent: Color) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = if (hour < 12) "¡Buenos días!" else if (hour < 18) "¡Buenas tardes!" else "¡Buenas noches!"
    
    Box(modifier = Modifier.padding(24.dp)) {
        GlassBox(cornerRadius = 24.dp) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(54.dp).background(Brush.linearGradient(listOf(accent, PremiumGold)), CircleShape), contentAlignment = Alignment.Center) {
                    Text("🧠", fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Dulce-Mind dice...", color = PremiumGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("$greeting $name. ¿Qué vamos a escuchar hoy? 🚀", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun VibrantSectionHeader(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        Text(subtitle, color = TextMuted, fontSize = 12.sp)
    }
}

@Composable
fun TopChartRow(items: List<MediaItem>, viewModel: PlayerViewModel, onNavigateToPlayer: () -> Unit, accent: Color) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = accent) }
    } else {
        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            itemsIndexed(items) { index, media -> RankedMediaCard(index + 1, media) { viewModel.playMedia(media); onNavigateToPlayer() } }
        }
    }
}

@Composable
fun RankedMediaCard(rank: Int, media: MediaItem, onClick: () -> Unit) {
    Column(modifier = Modifier.width(140.dp).clickable { onClick() }) {
        Box {
            AsyncImage(model = media.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(140.dp).clip(RoundedCornerShape(20.dp)).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(20.dp)))
            Surface(modifier = Modifier.align(Alignment.TopStart).padding(8.dp), shape = RoundedCornerShape(6.dp), color = PremiumGold) {
                Text("${rank}°", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(media.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(media.artist, color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SmallMediaCard(media: MediaItem, onClick: () -> Unit) {
    Column(modifier = Modifier.width(100.dp).clickable { onClick() }) {
        AsyncImage(model = media.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)))
        Spacer(modifier = Modifier.height(4.dp))
        Text(media.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
