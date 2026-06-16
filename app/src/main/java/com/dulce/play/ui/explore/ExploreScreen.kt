package com.dulce.play.ui.explore

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dulce.play.domain.model.MediaItem
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
    val top50 by viewModel.top50YouTube.collectAsState()
    val tendencias by viewModel.tendenciasColombia.collectAsState()
    val loMasEscuchado by viewModel.loMasEscuchado.collectAsState()
    val vallenato by viewModel.seccionVallenato.collectAsState()
    val salsa by viewModel.seccionSalsa.collectAsState()
    val urbano by viewModel.seccionUrbano.collectAsState()
    val popular by viewModel.seccionPopular.collectAsState()
    val loadingStates by viewModel.sectionLoadingStates.collectAsState()
    val errorStates by viewModel.sectionErrorStates.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()

    // Animación de entrada
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
            initialOffsetY = { 40 },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = 8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── Header de bienvenida ────────────────────────────────────────
            item {
                ExploreHeader(name = currentProfile.name)
            }

            // ── Carrusel de géneros horizontales (chips) ────────────────────
            item {
                GenreChipsRow(viewModel, onNavigateToPlayer)
            }

            // ── Top 50 YouTube ──────────────────────────────────────────────
            item {
                ExploreSection(
                    title = "🔥 Top 50 YouTube",
                    subtitle = "Los más vistos del momento",
                    items = top50,
                    isLoading = loadingStates["top50YouTube"] == true,
                    hasError = errorStates["top50YouTube"] == true,
                    viewModel = viewModel,
                    onNavigateToPlayer = onNavigateToPlayer,
                    accentColor = Color(0xFFFF4444)
                )
            }

            // ── Tendencias Colombia ─────────────────────────────────────────
            item {
                ExploreSection(
                    title = "🇨🇴 Tendencias Colombia",
                    subtitle = "Lo viral en tu país",
                    items = tendencias,
                    isLoading = loadingStates["tendenciasColombia"] == true,
                    hasError = errorStates["tendenciasColombia"] == true,
                    viewModel = viewModel,
                    onNavigateToPlayer = onNavigateToPlayer,
                    accentColor = Color(0xFFFFD700)
                )
            }

            // ── Lo Más Escuchado ────────────────────────────────────────────
            item {
                ExploreSection(
                    title = "🎧 Lo Más Escuchado",
                    subtitle = "Éxitos del momento en Latinoamérica",
                    items = loMasEscuchado,
                    isLoading = loadingStates["loMasEscuchado"] == true,
                    hasError = errorStates["loMasEscuchado"] == true,
                    viewModel = viewModel,
                    onNavigateToPlayer = onNavigateToPlayer,
                    accentColor = AccentCyan
                )
            }

            // ── Vallenato ───────────────────────────────────────────────────
            item {
                ExploreSection(
                    title = "🪗 Vallenato",
                    subtitle = "Carlos Vives, Binomio de Oro y más",
                    items = vallenato,
                    isLoading = loadingStates["vallenato"] == true,
                    hasError = errorStates["vallenato"] == true,
                    viewModel = viewModel,
                    onNavigateToPlayer = onNavigateToPlayer,
                    accentColor = Color(0xFFFF8C00)
                )
            }

            // ── Salsa ────────────────────────────────────────────────────────
            item {
                ExploreSection(
                    title = "💃 Salsa",
                    subtitle = "Cali, Cartagena y el mundo",
                    items = salsa,
                    isLoading = loadingStates["salsa"] == true,
                    hasError = errorStates["salsa"] == true,
                    viewModel = viewModel,
                    onNavigateToPlayer = onNavigateToPlayer,
                    accentColor = Color(0xFFE040FB)
                )
            }

            // ── Urbano / Reggaetón ─────────────────────────────────────────
            item {
                ExploreSection(
                    title = "🔊 Urbano",
                    subtitle = "Reggaetón, trap y lo que pega",
                    items = urbano,
                    isLoading = loadingStates["urbano"] == true,
                    hasError = errorStates["urbano"] == true,
                    viewModel = viewModel,
                    onNavigateToPlayer = onNavigateToPlayer,
                    accentColor = PrimaryNeon
                )
            }

            // ── Popular Colombiana ──────────────────────────────────────────
            item {
                ExploreSection(
                    title = "🎶 Popular",
                    subtitle = "Música popular colombiana",
                    items = popular,
                    isLoading = loadingStates["popular"] == true,
                    hasError = errorStates["popular"] == true,
                    viewModel = viewModel,
                    onNavigateToPlayer = onNavigateToPlayer,
                    accentColor = Color(0xFF00E5FF)
                )
            }

            // ── Éxitos Colombia ──────────────────────────────────────────────
            item {
                ExploreSection(
                    title = "🇨🇴 Éxitos Colombia",
                    subtitle = "Lo mejor de Colombia",
                    items = topCO,
                    isLoading = loadingStates["topColombia"] == true,
                    hasError = errorStates["topColombia"] == true,
                    viewModel = viewModel,
                    onNavigateToPlayer = onNavigateToPlayer,
                    accentColor = Color(0xFF76FF03)
                )
            }

            // ── Tendencias México ────────────────────────────────────────────
            item {
                ExploreSection(
                    title = "🇲🇽 Tendencias México",
                    subtitle = "Éxitos mexicanos",
                    items = topMX,
                    isLoading = loadingStates["topMexico"] == true,
                    hasError = errorStates["topMexico"] == true,
                    viewModel = viewModel,
                    onNavigateToPlayer = onNavigateToPlayer,
                    accentColor = Color(0xFFFF6D00)
                )
            }

            // ── Global Hits ──────────────────────────────────────────────────
            item {
                ExploreSection(
                    title = "🌎 Global Hits",
                    subtitle = "Lo mejor del mundo",
                    items = topGL,
                    isLoading = loadingStates["topGlobal"] == true,
                    hasError = errorStates["topGlobal"] == true,
                    viewModel = viewModel,
                    onNavigateToPlayer = onNavigateToPlayer,
                    accentColor = Color(0xFF40C4FF)
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// ── Header de bienvenida ────────────────────────────────────────────────────

@Composable
private fun ExploreHeader(name: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = "HOLA, ${name.uppercase()} 👋",
            fontSize = 11.sp,
            color = AccentCyan,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Explora lo Mejor",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = "Tu música, tus géneros, tus tendencias",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Chips de género ─────────────────────────────────────────────────────────

@Composable
private fun GenreChipsRow(viewModel: PlayerViewModel, onNavigateToPlayer: () -> Unit) {
    val generos = listOf(
        Triple("🪗 Vallenato", "vallenato exitos 2026", Color(0xFFFF8C00)),
        Triple("💃 Salsa", "salsa exitos 2026 cali", Color(0xFFE040FB)),
        Triple("🔊 Urbano", "reggaeton urbano 2026", PrimaryNeon),
        Triple("🎶 Popular", "musica popular colombiana", AccentCyan),
        Triple("🎸 Rock", "rock en espanol exitos 2026", Color(0xFFFF4444)),
        Triple("🎺 Cumbia", "cumbia exitos 2026", Color(0xFFFFD700)),
        Triple("🌍 Tropical", "musica tropical 2026", Color(0xFF76FF03)),
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        items(generos) { (label, query, color) ->
            GenreChip(label = label, color = color) {
                viewModel.buscarEnYouTube(query)
                onNavigateToPlayer()
            }
        }
    }
}

@Composable
private fun GenreChip(label: String, color: Color, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chip_scale"
    )
    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .clickable {
                pressed = true
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Sección de exploración ──────────────────────────────────────────────────

@Composable
fun ExploreSection(
    title: String,
    subtitle: String,
    items: List<MediaItem>,
    isLoading: Boolean,
    hasError: Boolean,
    viewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    accentColor: Color = AccentCyan
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {
        // Header de sección
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = accentColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }
            // Indicador de estado
            AnimatedVisibility(visible = isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Estado: Cargando (shimmer placeholder)
        if (isLoading && items.isEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) {
                    ShimmerCard()
                }
            }
            return@Column
        }

        // Estado: Error
        if (hasError && items.isEmpty()) {
            ErrorStateCard(accentColor = accentColor)
            return@Column
        }

        // Estado: Vacío (sin resultados aún, pero sin error tampoco)
        if (items.isEmpty()) {
            EmptyStateCard(accentColor = accentColor)
            return@Column
        }

        // Contenido real
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items, key = { it.id }) { item ->
                PremiumMediaCard(
                    item = item,
                    accentColor = accentColor
                ) {
                    viewModel.playMedia(item, autoPlay = true)
                    onNavigateToPlayer()
                }
            }
        }
    }
}

// ── Tarjeta Premium de Media ─────────────────────────────────────────────────

@Composable
fun PremiumMediaCard(
    item: MediaItem,
    accentColor: Color = AccentCyan,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_scale"
    )

    Column(
        modifier = Modifier
            .width(150.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable {
                isPressed = true
                onClick()
            }
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), clip = false)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(accentColor.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            // Imagen de portada
            AsyncImage(
                model = item.coverUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Overlay degradado inferior para legibilidad
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )

            // Botón de play sobre la imagen
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
                    .background(
                        Brush.radialGradient(
                            listOf(accentColor.copy(alpha = 0.9f), accentColor.copy(alpha = 0.4f))
                        ),
                        shape = CircleShape
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Reproducir",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.artist,
            color = accentColor.copy(alpha = 0.8f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Estados visuales: Shimmer, Error, Vacío ──────────────────────────────────

@Composable
private fun ShimmerCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    Column(modifier = Modifier.width(150.dp)) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = shimmerAlpha))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = shimmerAlpha))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White.copy(alpha = shimmerAlpha * 0.7f))
        )
    }
}

@Composable
private fun ErrorStateCard(accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A0000))
            .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Rounded.WifiOff,
                contentDescription = null,
                tint = Color.Red.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    "Sin conexión",
                    color = Color.Red.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Verifica tu conexión a internet",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = accentColor.copy(alpha = 0.5f)
            )
            Text(
                "Cargando contenido...",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }
    }
}

// ── Backward compatibility: TopChartRow y TrendCard originales ──────────────

@Composable
fun TopChartRow(
    title: String,
    items: List<MediaItem>,
    viewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit
) {
    ExploreSection(
        title = title,
        subtitle = "",
        items = items,
        isLoading = items.isEmpty(),
        hasError = false,
        viewModel = viewModel,
        onNavigateToPlayer = onNavigateToPlayer
    )
}

@Composable
fun TrendCard(item: MediaItem, onClick: () -> Unit) {
    PremiumMediaCard(item = item, onClick = onClick)
}
