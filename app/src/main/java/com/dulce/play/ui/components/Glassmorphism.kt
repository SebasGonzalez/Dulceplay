package com.dulce.play.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dulce.play.ui.theme.GlassWhite
import com.dulce.play.ui.theme.GlassBorder
import com.dulce.play.ui.theme.ElectricBlue
import com.dulce.play.ui.theme.ElegantBlack
import com.dulce.play.ui.theme.DeepPurple
import com.dulce.play.ui.theme.ElectricBlue
import com.dulce.play.ui.theme.DeepPurple
import com.dulce.play.ui.theme.DeepPurple
import com.dulce.play.ui.theme.ElegantBlack

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 16.dp,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(GlassWhite)
            .border(borderWidth, GlassBorder, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun ParticleField(
    particles: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier,
    color: Color = ElectricBlue.copy(alpha = 0.2f)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        particles.forEach { (x, y) ->
            drawCircle(
                color = color,
                radius = 3.dp.toPx() + (y * 2.dp.toPx()), // size scales on depth/height
                center = Offset(x * w, y * h)
            )
        }
    }
}

@Composable
fun CosmicPlasmaBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "plasma")
    
    val pulseX by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x"
    )
    val pulseY by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 150f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ElegantBlack)
            .drawBehind {
                // Sophisticated Dark Radial Glow 1 (Deep Violet Top Layer)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(DeepPurple.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(size.width * 0.5f + pulseX, size.height * 0.25f)
                    ),
                    radius = size.width * 1.0f
                )

                // Sophisticated Dark Radial Glow 2 (Atmospheric Rose Bottom Layer)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(DeepPurple.copy(alpha = 0.42f), Color.Transparent),
                        center = Offset(size.width * 0.15f - pulseX * 0.5f, size.height * 0.75f + pulseY)
                    ),
                    radius = size.width * 0.85f
                )
            }
    )
}

@Composable
fun ReflectiveVinylCover(
    modifier: Modifier = Modifier,
    illustrationType: String,
    isPlaying: Boolean,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "disc")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_angle"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "disc_scale"
    )

    // Parallax holographic angle shift representation
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "holographic_shimmer"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(16.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.Black)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
        ) {
            val width = size.width
            val height = size.height

            // Render a custom elegant abstract artistic canvas matching the requested track context!
            val artworkBackground = when (illustrationType) {
                "colombia" -> Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFF003893), Color(0xFFCE1126)))
                "mexico" -> Brush.horizontalGradient(listOf(Color(0xFF006847), Color(0xFFFFFFFF), Color(0xFFCE1126)))
                "brasil" -> Brush.radialGradient(listOf(Color(0xFF009739), Color(0xFFFEDF00), Color(0xFF012169)))
                "ambient" -> Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6), Color(0xFF67E8F9)))
                "cyberpunk" -> Brush.sweepGradient(listOf(Color(0xFFFF0D7B), Color(0xFF9013FE), Color(0xFF00F5FF), Color(0xFFFF0D7B)))
                "music", "music_logo" -> Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)))
                else -> Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
            }

            // Draw underlying colorful pattern
            drawRect(brush = artworkBackground)

            // Draw a spinning vinyl textured glass graphic
            val discAngle = if (isPlaying) angle else 0f
            rotate(discAngle) {
                // Vinyl outer ring
                drawCircle(
                    color = Color.Black.copy(alpha = 0.85f),
                    radius = width * 0.40f
                )

                // Sound wave ridges (vinyl lines)
                for (r in 1..8) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.04f + (r * 0.005f)),
                        radius = width * (0.05f * r),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                }

                // Inner vinyl plate label
                drawCircle(
                    color = ElectricBlue.copy(alpha = 0.6f),
                    radius = width * 0.12f
                )

                // Draw central spindle hole
                drawCircle(
                    color = Color.Black,
                    radius = width * 0.02f
                )
            }

            // Holographic reflection overlay sheet (Parallax representation)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.0f),
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.0f),
                        ElectricBlue.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    start = Offset(shimmerOffset - 100f, 0f),
                    end = Offset(shimmerOffset + 200f, height)
                )
            )
        }

        // Live premium playing overlay
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Row(
                    modifier = Modifier
                        .background(ElectricBlue.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color.White, RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
