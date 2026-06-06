package com.dulce.play.ui.library

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dulce.play.ui.player.PlayerViewModel
import com.dulce.play.ui.theme.*

@Composable
fun LibraryScreen(
    viewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit
) {
    val favorites by viewModel.favorites.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("MI BIBLIOTECA", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(24.dp))
        
        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aún no tienes favoritos", color = TextMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(favorites) { item ->
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(0.05f)).clickable { viewModel.playMedia(item); onNavigateToPlayer() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.MusicNote, null, tint = accentColor)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(item.artist, color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
