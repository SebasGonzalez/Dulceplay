package com.dulce.play.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dulce.play.ui.player.PlayerViewModel
import com.dulce.play.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val currentAccount by viewModel.currentAccount.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val voiceType by viewModel.voiceType.collectAsState()
    val brightness by viewModel.appBrightness.collectAsState()
    val childLock by viewModel.childLockEnabled.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(scrollState).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 👤 ACCOUNT ---
        Box(modifier = Modifier.size(90.dp).background(accentColor.copy(alpha = 0.1f), CircleShape).border(2.dp, accentColor, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Person, null, tint = accentColor, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(currentAccount?.displayName ?: "Usuario", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
        
        Spacer(modifier = Modifier.height(32.dp))

        // --- PREFERENCES ---
        SectionHeader("Personalización Visual", accentColor)
        
        // Accent Color
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(ElectricBlue, DeepPurple, PremiumGold).forEach { color ->
                Box(
                    modifier = Modifier.size(50.dp).clip(CircleShape).background(color).border(if (accentColor == color) 3.dp else 0.dp, Color.White, CircleShape).clickable { viewModel.setAccentColor(color) }
                )
            }
        }

        // Brightness
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LightMode, null, tint = accentColor)
            Slider(value = brightness, onValueChange = { viewModel.setAppBrightness(it) }, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor))
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader("Seguridad y Control", accentColor)
        AccountOptionItem("Bloqueo Infantil", Icons.Rounded.ChildFriendly, if (childLock) Color.Red else accentColor) {
            viewModel.toggleChildLock()
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader("Asistente Dulce", accentColor)
        
        // Voice Type
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("female" to "Mujer", "male" to "Hombre", "silent" to "Silencio").forEach { (id, label) ->
                FilterChip(
                    selected = voiceType == id,
                    onClick = { viewModel.setVoiceType(id) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor, selectedLabelColor = Color.Black)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- Support ---
        AccountOptionItem("Soporte y Comunicación", Icons.Rounded.Email, accentColor) {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:sebasgnz@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "Informe / Sugerencia - DulcePlay [V2.1]")
            }
            context.startActivity(Intent.createChooser(intent, "Enviar correo..."))
        }

        Spacer(modifier = Modifier.height(40.dp))
        
        Button(onClick = { viewModel.logout() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)), shape = RoundedCornerShape(16.dp)) {
            Text("CERRAR SESIÓN", color = Color.Red, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Text(title.uppercase(), color = color, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
}

@Composable
fun AccountOptionItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accentColor: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.05f)).clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).background(accentColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp)) }
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f))
    }
}
