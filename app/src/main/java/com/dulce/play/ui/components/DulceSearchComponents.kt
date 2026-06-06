package com.dulce.play.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dulce.play.ui.player.PlayerViewModel
import com.dulce.play.ui.player.PlayerViewModel.SearchFilter
import com.dulce.play.ui.theme.*

@Composable
fun DulceSearchTopBar(viewModel: PlayerViewModel, modifier: Modifier = Modifier) {
    val query by viewModel.searchQuery.collectAsState()
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp).height(56.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(0.05f)).border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(24.dp)).clickable { viewModel.setSearchOverlayActive(true) }.padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(if (query.isNotEmpty()) query else "Buscar música o videos...", color = Color.White.copy(0.5f), fontSize = 14.sp)
        }
    }
}

@Composable
fun SearchFilterTabs(viewModel: PlayerViewModel) {
    val currentFilter by viewModel.searchFilter.collectAsState()
    val primary = MaterialTheme.colorScheme.primary
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SearchFilter.entries.forEach { filter ->
            val active = currentFilter == filter
            Box(modifier = Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (active) primary else Color.White.copy(0.05f)).clickable { viewModel.setSearchFilter(filter) }, contentAlignment = Alignment.Center) {
                Text(filter.name, color = if (active) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun DulceSearchOverlay(viewModel: PlayerViewModel, onNavigateToPlayer: () -> Unit, modifier: Modifier = Modifier) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.onlineSearchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val primary = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.fillMaxSize().background(Color.Black.copy(0.98f))) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.setSearchOverlayActive(false) }) { Icon(Icons.Rounded.Close, null, tint = Color.White) }
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Nombre de canción o artista...") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        viewModel.executeSearch(query)
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    })
                )
                IconButton(onClick = { 
                    viewModel.executeSearch(query)
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }) {
                    Icon(Icons.Rounded.Search, null, tint = primary)
                }
            }
            SearchFilterTabs(viewModel)
            if (isSearching) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = primary)
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                items(results) { item ->
                    SearchResultRow(item.title, item.artist, item.coverUrl, primary) {
                        viewModel.playMedia(item); viewModel.setSearchOverlayActive(false); onNavigateToPlayer()
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultRow(title: String, artist: String, cover: String, color: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.05f)).clickable { onClick() }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = cover, contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
        }
        Icon(Icons.Rounded.PlayCircle, null, tint = color)
    }
}
