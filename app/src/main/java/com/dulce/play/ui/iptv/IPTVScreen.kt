package com.dulce.play.ui.iptv

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dulce.play.ui.player.PlayerViewModel
import com.dulce.play.ui.theme.*

@Composable
fun IPTVScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    onNavigateToPlayer: () -> Unit = {}
) {
    val radioStations by viewModel.radioStations.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("RADIOS Y TV EN VIVO", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(radioStations) { radio ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(0.05f)).clickable { viewModel.playIPTVChannel(radio); onNavigateToPlayer() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(50.dp).background(accentColor.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Radio, null, tint = accentColor)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(radio.name, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(radio.group, color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
