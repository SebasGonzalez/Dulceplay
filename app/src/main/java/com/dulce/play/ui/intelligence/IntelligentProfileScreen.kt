package com.dulce.play.ui.intelligence

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dulce.play.ui.components.GlassBox
import com.dulce.play.ui.theme.*

@Composable
fun IntelligentProfileScreen(
    profileName: String = "Usuario",
    detectedMood: String = "Relajado",
    favoriteGenre: String = "Chillhop",
    activeRoutine: String = "Escuchando música nocturna"
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Mi Perfil",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "Análisis cognitivo de DULCE-MIND",
                fontSize = 14.sp,
                color = TextMuted
            )
        }

        item {
            IntelligenceCard(
                title = "Estado Emocional",
                value = detectedMood,
                icon = "✨",
                accentColor = ElectricBlue
            )
        }

        item {
            IntelligenceCard(
                title = "Preferencias",
                value = favoriteGenre,
                icon = "🎵",
                accentColor = DeepPurple
            )
        }

        item {
            IntelligenceCard(
                title = "Hábito Detectado",
                value = activeRoutine,
                icon = "📅",
                accentColor = PremiumGold
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(DeepPurple.copy(alpha = 0.1f))
                    .border(1.dp, DeepPurple.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "INTELIGENCIA ACTIVA 🧠",
                        color = PremiumGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tu memoria cognitiva está protegida y sincronizada en la nube de DulcePlay.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun IntelligenceCard(title: String, value: String, icon: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardDark)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).background(accentColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = title, fontSize = 12.sp, color = accentColor, fontWeight = FontWeight.Bold)
                Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}
