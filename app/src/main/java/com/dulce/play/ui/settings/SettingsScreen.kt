package com.dulce.play.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dulce.play.ui.components.GlassBox
import com.dulce.play.ui.player.PlayerViewModel
import com.dulce.play.ui.player.PlayerViewModel.VisualTheme
import com.dulce.play.ui.player.PlayerViewModel.CastState
import com.dulce.play.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Retrieve active configurations
    val activeTheme by viewModel.activeTheme.collectAsState()
    val castState by viewModel.castState.collectAsState()
    val castDevice by viewModel.castDevice.collectAsState()
    val availableDevices by viewModel.availableCastDevices.collectAsState()

    val eqEnabled by viewModel.eqEnabled.collectAsState()
    val eqPreset by viewModel.eqPreset.collectAsState()
    val eqBands by viewModel.eqBands.collectAsState()

    val assistantVoice by viewModel.assistantVoiceEnabled.collectAsState()
    val assistantLearn by viewModel.assistantAutoLearn.collectAsState()

    val profileLockPin by viewModel.profileLocks.collectAsState()
    val activeProfile by viewModel.currentProfile.collectAsState()

    // Temporary JSON buffer for restore actions
    var jsonBackupInput by remember { mutableStateOf("") }
    var showBackupDialog by remember { mutableStateOf(false) }

    // Simulated local storage tracker
    var cacheSizeMB by remember { mutableStateOf(20.5f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- Grand Settings Title ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "AJUSTES AVANZADOS",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.5.sp
            )
        }

        Text(
            text = "Personaliza el metaverso multimedia y administra cada rincón de DulcePlay de forma inmediata.",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        // 1. 🎨 THEME CUSTOMIZER DESIGN BLOCK
        SettingsSectionCard(title = "Apariencia & Temas Visuales", icon = Icons.Rounded.Palette) {
            Text(
                text = "Sintoniza los neones virtuales alternando el revestimiento gráfico de la interfaz:",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val themesList = listOf(
                Triple(VisualTheme.CYBER_NEON, "Cyber Neon 🌸", "Rosa intenso y púrpura cyberpunk"),
                Triple(VisualTheme.CLASSIC_DARK, "Oscuro Clásico 🌪️", "Monocromático, acero y plata mate"),
                Triple(VisualTheme.ELECTRIC_BLUE, "Azul Eléctrico ❄️", "Holográfico cian, cobalto y azul"),
                Triple(VisualTheme.NATURE_GREEN, "Verde Naturaleza 🍃", "Tonos menta neón, esmeralda y musgo")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                themesList.forEach { (theme, name, desc) ->
                    val isSelected = activeTheme == theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f))
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.selectTheme(theme) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.selectTheme(theme) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = Color.White.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(desc, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 2. 🎚️ GRAPHIC EQUALIZER BLOCK
        SettingsSectionCard(title = "Ecualizador Gráfico de Sonido", icon = Icons.Rounded.Tune) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Activar DSP Ecualizador", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Permite calibrar ondas físicas de ExoPlayer", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
                Switch(
                    checked = eqEnabled,
                    onCheckedChange = { viewModel.toggleEqualizer() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                )
            }

            AnimatedVisibility(visible = eqEnabled) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text("Preajustes Rápidos (Presets):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf("Plano", "Refuerzo Bajos", "Agudos Nítidos", "Cine / Teatro", "Power Rock", "Clásica Suave")
                        presets.forEach { pre ->
                            val isSelected = eqPreset == pre
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.applyEqualizerPreset(pre) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = pre,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Calibración manual por bandas:", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)

                    val frequencies = listOf("60 Hz", "230 Hz", "910 Hz", "4 kHz", "14 kHz")
                    eqBands.forEachIndexed { idx, value ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(frequencies[idx], color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("$value%", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = value.toFloat(),
                                onValueChange = { viewModel.updateEqBand(idx, it.toInt()) },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    thumbColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. 📺 GOOGLE CAST INTEGRATION CARD
        SettingsSectionCard(title = "Sincronización de Pantalla & Cast (Google Cast)", icon = Icons.Rounded.Cast) {
            Text(
                text = "Transmite listas dinámicas, música o televisión IPTV en vivo directamente a tu Smart TV o Chromecast cercano:",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (castState) {
                CastState.DISCONNECTED -> {
                    Button(
                        onClick = { viewModel.searchCastDevices() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_chromecast_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("BUSCAR DISPOSITIVOS CAST CERCANOS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                CastState.SEARCHING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Escaneando frecuencias de red WiFi...", color = Color.White, fontSize = 13.sp)
                    }
                }
                CastState.CONNECTED -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Green.copy(alpha = 0.15f))
                            .border(1.5.dp, Color.Green, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CastConnected, null, tint = Color.Green, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("TRANSMISIÓN EN CURSO", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Canal activo enviado a: $castDevice", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                        }

                        IconButton(
                            onClick = { viewModel.stopCasting() },
                            modifier = Modifier.background(LiveRed, CircleShape).size(30.dp)
                        ) {
                            Icon(Icons.Rounded.Stop, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (availableDevices.isNotEmpty() && castState == CastState.SEARCHING) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Pantallas detectadas:", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                
                availableDevices.forEach { dev ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { viewModel.startCasting(dev) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Tv, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(dev, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("SINTONIZAR", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // 4. 💾 COPIA DE SEGURIDAD (BACKUP / RESTORE) CARD
        SettingsSectionCard(title = "Capa de Respaldo & Nube", icon = Icons.Rounded.CloudUpload) {
            Text(
                "Protege tu metaverso. Exporta un archivo cifrado conteniendo tus listas creadas, preferencias y sintonías preferidas.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val backupText = viewModel.createJSONBackup()
                        // Copy to clipboard safely
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("dulceplay_respaldo", backupText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "¡Respaldo JSON copiado al portapapeles! Cuárdalo seguro 📋", Toast.LENGTH_LONG).show()
                        showBackupDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.FileDownload, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESPALDO", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        showBackupDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.FileUpload, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESTAURAR", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 5. 🤖 SMART ASSISTANT CONFIGURATION
        SettingsSectionCard(title = "Configuración de DULCE-BOT", icon = Icons.Rounded.SmartToy) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Voz del Asistente Activa", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("El asistente lee respuestas verbalmente", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
                Switch(
                    checked = assistantVoice,
                    onCheckedChange = { viewModel.toggleAssistantVoice() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Cognición de Hábitos Room", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Permite leer reproducciones para predecir", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
                Switch(
                    checked = assistantLearn,
                    onCheckedChange = { viewModel.toggleAssistantAutoLearn() }
                )
            }
        }

        // 6. 🔒 SECURITY & PIN LOCKS
        SettingsSectionCard(title = "Seguridad del Perfil", icon = Icons.Rounded.Security) {
            val hasLock = profileLockPin.containsKey(activeProfile.id)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Capa de Bloqueo de Perfil PIN", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (hasLock) "Bloqueado seguro con PIN: ****" else "Abierto, sin restringir acceso", 
                        color = if (hasLock) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f), 
                        fontSize = 11.sp
                    )
                }
                
                Button(
                    onClick = {
                        val newPin = if (hasLock) "" else "1234"
                        viewModel.setProfileLockPin(activeProfile.id, newPin)
                        val msg = if (hasLock) "Bloqueo desactivado del perfil" else "¡Perfil blindado con PIN temporal '1234'!"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (hasLock) LiveRed else MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (hasLock) "QUITAR PIN" else "PONER PIN '1234'", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 7. 💾 CACHE STORAGE CLEANER MANAGER
        SettingsSectionCard(title = "Almacenamiento Virtual", icon = Icons.Rounded.Storage) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Espacio temporal saturado", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Caché de IPTV, portafolios y carátulas", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
                Text(
                    text = String.format("%.2f MB", cacheSizeMB),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    cacheSizeMB = 0f
                    Toast.makeText(context, "Se vaciaron 20.5 MB de caché y recursos IPTV con éxito", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                border = BorderStroke(1.2.dp, if (cacheSizeMB > 0f) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = cacheSizeMB > 0f
            ) {
                Text("VACIAR CACHÉ DE IMÁGENES Y TELEVISIÓN", color = if (cacheSizeMB > 0f) Color.White else Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // Spaced out for bottom margins and bottom navigation overlap
    }

    // Backup & Restore Overlay Dialog
    if (showBackupDialog) {
        Dialog(
            onDismissRequest = { showBackupDialog = false }
        ) {
            GlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "IMPORTAR Y RESTAURAR",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "Pega la cadena de bloques JSON extraída o tu código de respaldo generado anteriormente para resintonizar tus datos de Room:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )

                    TextField(
                        value = jsonBackupInput,
                        onValueChange = { jsonBackupInput = it },
                        placeholder = { Text("Pega JSON de respaldo...", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (viewModel.restoreFromBackupJSON(jsonBackupInput)) {
                                    Toast.makeText(context, "¡Copia de seguridad restaurada de forma impecable! 🎉", Toast.LENGTH_LONG).show()
                                    showBackupDialog = false
                                } else {
                                    Toast.makeText(context, "Error en el parsing. Verifica que la cadena sea válida.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("RESTAURAR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showBackupDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CERRAR", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassBox(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp)),
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title.uppercase(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(bottom = 12.dp))

            content()
        }
    }
}
