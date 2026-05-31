package com.example.ui.assistant

import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.IPTVChannel
import com.example.domain.model.MediaItem
import com.example.domain.model.MediaType
import com.example.ui.components.GlassBox
import com.example.ui.player.PlayerViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

enum class AssistantState {
    IDLE, LISTENING, THINKING, SPEAKING
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "USER" or "DULCE_BOT"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedMedia: MediaItem? = null,
    val suggestedChannel: IPTVChannel? = null,
    val specialAction: String? = null // "EASY_MODE", "DRIVING_MODE", "FAMILY_MODE" etc.
)

@Composable
fun AssistantFloatingButton(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    var showAssistantDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Breathing pulse animation for the holographic AI Orb
    val infiniteTransition = rememberInfiniteTransition(label = "hologram_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val neonAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon_alpha"
    )

    Box(
        modifier = modifier
            .padding(bottom = 100.dp, end = 16.dp) // Offset above floating bottom bar
            .size(60.dp)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AccentCyan, 
                        PrimaryNeon.copy(alpha = 0.8f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            )
            .border(
                2.dp, 
                Color.White.copy(alpha = neonAlpha), 
                CircleShape
            )
            .shadow(16.dp, CircleShape)
            .clickable { showAssistantDialog = true }
            .testTag("assistant_floating_bubble"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.SmartToy,
            contentDescription = "Asistente Inteligente DulcePlay",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }

    if (showAssistantDialog) {
        IntelligenceCenterDialog(
            viewModel = viewModel,
            onDismiss = { showAssistantDialog = false }
        )
    }
}

@Composable
fun IntelligenceCenterDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rawInputState by remember { mutableStateOf("") }
    var assistantState by remember { mutableStateOf(AssistantState.IDLE) }
    
    val profile by viewModel.currentProfile.collectAsState()
    val isEasyMode by viewModel.isEasyMode.collectAsState()
    val isDrivingMode by viewModel.isDrivingMode.collectAsState()
    val isFamilyMode by viewModel.isFamilyMode.collectAsState()
    val isWellnessMode by viewModel.isWellnessMode.collectAsState()
    val sleepSecondsRemaining by viewModel.sleepTimerRemainingSeconds.collectAsState()

    // Simulated / In-App Conversational Feed
    val chatHistory = remember {
        mutableStateListOf(
            ChatMessage(
                sender = "DULCE_BOT",
                text = "¡Hola ${profile.name}! Soy DULCE-BOT, tu copiloto multimedia del Cyber-Metaverso. \n\nPuedo programar temporizadores de sueño, analizar tus hábitos de Room, y activar modos especiales de accesibilidad. Pregúntame, por ejemplo: \n\n* 'Pon música para estudiar'\n* 'Activa noticias de Colombia'\n* 'Pon folklore'\n* 'Inicia el modo fácil'\n* 'Programar modo noche'"
            )
        )
    }

    // Android Speech Recognition contract
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val spokenTexts = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenQuery = spokenTexts?.firstOrNull()
            if (!spokenQuery.isNullOrBlank()) {
                scope.launch {
                    processAssistantQuery(
                        query = spokenQuery,
                        viewModel = viewModel,
                        history = chatHistory,
                        setAssetState = { assistantState = it }
                    )
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .border(1.5.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(28.dp)),
                    cornerRadius = 28.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // --- Assistant Header ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AnimatedOrbVisual(state = assistantState)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "DULCE-BOT",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = when(assistantState) {
                                            AssistantState.IDLE -> "Santuario Inteligente Activo"
                                            AssistantState.LISTENING -> "Escuchando tu voz..."
                                            AssistantState.THINKING -> "Procesando hábitos de Room..."
                                            AssistantState.SPEAKING -> "Comunicando respuesta..."
                                        },
                                        color = AccentCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDismiss() },
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Cerrar",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // --- Active Special Modes Badges Grid ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isEasyMode) {
                                ModeIndicatorBadge("Modo Fácil", Icons.Rounded.Accessibility, PrimaryNeon)
                            }
                            if (isDrivingMode) {
                                ModeIndicatorBadge("Modo Manejo", Icons.Rounded.DriveEta, AccentCyan)
                            }
                            if (isFamilyMode) {
                                ModeIndicatorBadge("Modo Familiar", Icons.Rounded.ChildCare, ElectricViolet)
                            }
                            if (isWellnessMode) {
                                ModeIndicatorBadge("Modo Bienestar", Icons.Rounded.Spa, Color.Green)
                            }
                            if (sleepSecondsRemaining > 0) {
                                ModeIndicatorBadge(
                                    "Apagado en ${sleepSecondsRemaining / 60}m", 
                                    Icons.Rounded.Timer, 
                                    Color.Yellow
                                )
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 12.dp))

                        // --- Interactive Voice / Text Chat Scroller ---
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                reverseLayout = false
                            ) {
                                items(chatHistory) { msg ->
                                    ChatBubble(
                                        message = msg,
                                        onTriggerMedia = { media ->
                                            viewModel.playMedia(media)
                                            onDismiss()
                                        },
                                        onTriggerChannel = { ch ->
                                            viewModel.playIPTVChannel(ch)
                                            onDismiss()
                                        },
                                        onToggleAction = { action ->
                                            when(action) {
                                                "EASY_MODE" -> viewModel.toggleEasyMode()
                                                "DRIVING_MODE" -> viewModel.toggleDrivingMode()
                                                "FAMILY_MODE" -> viewModel.toggleFamilyMode()
                                                "WELLNESS_MODE" -> viewModel.toggleWellnessMode()
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // --- AI Core Input Deck ---
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            // Shortcuts panel
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val suggestions = listOf(
                                    "Estudiar", "Noticias de Colombia", "¿Qué ver hoy?", 
                                    "Modo fácil", "Modo noche", "Modo Bienestar", "Modo familiar"
                                )
                                suggestions.forEach { hint ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.06f))
                                            .border(1.dp, AccentCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                            .clickable {
                                                rawInputState = hint
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(hint, color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mic Voice Prompt Button with Google STT interface
                                IconButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Díctale un comando a Dulce-Bot en español")
                                            }
                                            assistantState = AssistantState.LISTENING
                                            speechLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            // Handle speech unavailable - do a nice mock speech simulation
                                            scope.launch {
                                                assistantState = AssistantState.LISTENING
                                                val mockCommands = listOf(
                                                    "estudiar", "noticias de Colombia", "modo noche", "modo fácil", 
                                                    "folklore de artistas locales", "¿qué puedo ver hoy?", "respirar"
                                                )
                                                val chosenText = mockCommands.random()
                                                Toast.makeText(context, "Emulación de Voz: '" + chosenText + "'", Toast.LENGTH_SHORT).show()
                                                delay(1500)
                                                rawInputState = chosenText
                                                assistantState = AssistantState.IDLE
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .background(PrimaryNeon, CircleShape)
                                        .size(42.dp)
                                        .testTag("assistant_mic_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Hablale al asistente",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                TextField(
                                    value = rawInputState,
                                    onValueChange = { rawInputState = it },
                                    placeholder = { 
                                        Text(
                                            "Escribe comando en español...", 
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 13.sp
                                        ) 
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("assistant_text_input")
                                )

                                IconButton(
                                    onClick = {
                                        if (rawInputState.isNotBlank()) {
                                            val query = rawInputState
                                            rawInputState = ""
                                            scope.launch {
                                                processAssistantQuery(
                                                    query = query,
                                                    viewModel = viewModel,
                                                    history = chatHistory,
                                                    setAssetState = { assistantState = it }
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .size(42.dp)
                                        .testTag("assistant_send_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Enviar",
                                        tint = AccentCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModeIndicatorBadge(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AnimatedOrbVisual(state: AssistantState) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_orb")
    
    val targetScale = when(state) {
        AssistantState.IDLE -> 1.0f
        AssistantState.LISTENING -> 1.3f
        AssistantState.THINKING -> 1.15f
        AssistantState.SPEAKING -> 1.25f
    }
    
    val duration = when(state) {
        AssistantState.IDLE -> 3000
        AssistantState.LISTENING -> 800
        AssistantState.THINKING -> 500
        AssistantState.SPEAKING -> 1000
    }

    val orbScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "target_scale"
    )

    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_pulse"
    )

    val primaryColor = when(state) {
        AssistantState.IDLE -> AccentCyan
        AssistantState.LISTENING -> LiveRed
        AssistantState.THINKING -> PrimaryNeon
        AssistantState.SPEAKING -> Color.Green
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .graphicsLayer {
                scaleX = orbScale * wavePulse
                scaleY = orbScale * wavePulse
            }
            .background(
                brush = Brush.sweepGradient(
                    colors = listOf(primaryColor, ElectricViolet, primaryColor)
                ),
                shape = CircleShape
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onTriggerMedia: (MediaItem) -> Unit,
    onTriggerChannel: (IPTVChannel) -> Unit,
    onToggleAction: (String) -> Unit
) {
    val holdsUser = message.sender == "USER"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (holdsUser) Arrangement.End else Arrangement.Start
    ) {
        if (!holdsUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SmartToy, null, tint = AccentCyan, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (holdsUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (holdsUser) 16.dp else 4.dp,
                            bottomEnd = if (holdsUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (holdsUser) {
                            Brush.linearGradient(listOf(ElectricViolet, PrimaryNeon))
                        } else {
                            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.03f)))
                        }
                    )
                    .border(
                        1.dp,
                        if (holdsUser) Color.Transparent else AccentCyan.copy(alpha = 0.15f),
                        RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp, 
                            bottomStart = if (holdsUser) 16.dp else 4.dp, 
                            bottomEnd = if (holdsUser) 4.dp else 16.dp
                        )
                    )
                    .padding(14.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = if (holdsUser) FontWeight.Bold else FontWeight.Medium
                )
            }

            // Interactive payload attach buttons (recommender anchors)
            message.suggestedMedia?.let { media ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryNeon.copy(alpha = 0.15f))
                        .border(1.dp, PrimaryNeon, RoundedCornerShape(12.dp))
                        .clickable { onTriggerMedia(media) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.PlayCircle, null, tint = PrimaryNeon, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(media.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(media.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                    Text("REPRODUCIR", color = PrimaryNeon, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            message.suggestedChannel?.let { channel ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentCyan.copy(alpha = 0.15f))
                        .border(1.dp, AccentCyan, RoundedCornerShape(12.dp))
                        .clickable { onTriggerChannel(channel) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.LiveTv, null, tint = AccentCyan, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(channel.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(channel.group, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                    Text("SINTONIZAR", color = AccentCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            message.specialAction?.let { action ->
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { onToggleAction(action) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SettingsPower, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when(action) {
                                "EASY_MODE" -> "INTERRUPTOR MODO FÁCIL"
                                "DRIVING_MODE" -> "INTERRUPTOR MODO MANEJO"
                                "FAMILY_MODE" -> "INTERRUPTOR FILTRO FAMILIAR"
                                "WELLNESS_MODE" -> "INICIAR MEDITACIÓN/YOGA"
                                else -> "ACTIVAR ENTORNO"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (holdsUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(PrimaryNeon.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AccountCircle, null, tint = PrimaryNeon, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Clean on-device Natural Language Parser and Cognitive Recommendation Processor.
 * It analyzes both user text structures and SQLite history lists to create responses.
 */
suspend fun processAssistantQuery(
    query: String,
    viewModel: PlayerViewModel,
    history: MutableList<ChatMessage>,
    setAssetState: (AssistantState) -> Unit
) {
    // 1. Add user message
    history.add(ChatMessage(sender = "USER", text = query))
    
    // 2. Animate processing states
    setAssetState(AssistantState.THINKING)
    delay(1000)
    setAssetState(AssistantState.SPEAKING)

    val cleanQuery = query.trim().lowercase()

    // 3. Evaluate Rule Engine & On-Device Cognitive Analysis
    var replyText = ""
    var mediaPayload: MediaItem? = null
    var channelPayload: IPTVChannel? = null
    var actionPayload: String? = null

    when {
        // UNIVERSAL SEARCH COMMAND
        cleanQuery.startsWith("busca ") || cleanQuery.startsWith("buscar ") || cleanQuery.startsWith("encuentra ") || cleanQuery.startsWith("encontrar ") || cleanQuery.startsWith("search ") -> {
            val searchTerm = query.replace("(?i)^(busca|buscar|encuentra|encontrar|search)\\s+".toRegex(), "").trim()
            viewModel.updateSearchQuery(searchTerm)
            viewModel.setSearchOverlayActive(true)
            replyText = "¡Por supuesto! He activado el Buscador Inteligente 'Dulce-Search' buscando **\"$searchTerm\"**. Aquí puedes explorar los resultados locales en tu celular y coincidencias de internet libres de regalías en tiempo real. 🔎🎶"
        }

        // CONCENTRATION / STUDY MODE
        cleanQuery.contains("estudiar") || cleanQuery.contains("concentra") || cleanQuery.contains("estudio") -> {
            // Find a calming instrumental track
            val studyMedia = viewModel.getFilteredMediaList().firstOrNull { 
                it.genre.lowercase().contains("ambient") || it.genre.lowercase().contains("chillout") 
            } ?: viewModel.getFilteredMediaList().firstOrNull()
            
            replyText = "¡Entendido! Activando modo de máxima concentración para potenciar tu productividad en la red. He sintonizado frecuencias de ondas alfa estables con un volumen moderado y sin transiciones bruscas. ¡Mucho éxito en tus tareas! 🧠💻"
            mediaPayload = studyMedia
        }

        // REGIONAL IPTV NEWS CHANNELS
        cleanQuery.contains("noticia") || cleanQuery.contains("news") -> {
            var targetCountry = "Global"
            when {
                cleanQuery.contains("colombia") -> targetCountry = "Colombia"
                cleanQuery.contains("méxico") || cleanQuery.contains("mexico") -> targetCountry = "México"
                cleanQuery.contains("españa") || cleanQuery.contains("espania") -> targetCountry = "España"
                cleanQuery.contains("argentina") -> targetCountry = "Argentina"
            }

            val channel = viewModel.getAllIPTVChannels().firstOrNull { 
                it.group.lowercase().contains("noticias") || it.name.lowercase().contains("telesur") || it.country.lowercase() == targetCountry.lowercase()
            } ?: viewModel.getAllIPTVChannels().firstOrNull()

            replyText = if (targetCountry != "Global") {
                "Sintonizando de inmediato el portal oficial de noticias de $targetCountry en vivo desde nuestro satélite IPTV. Mantente al día con el pulso real de la región. 🇨🇴🛰️"
            } else {
                "Enlazando señal de información continua y noticias globales. Abriendo streaming en vivo. 📡📰"
            }
            channelPayload = channel
        }

        // BEHAVIORAL / HISTORIC COGNITIVE ANALYSIS
        cleanQuery.contains("qué ver") || cleanQuery.contains("recomi") || cleanQuery.contains("sugerencia") || cleanQuery.contains("gusta") -> {
            val userHist = viewModel.playbackHistory.value
            if (userHist.isNotEmpty()) {
                // Read history to extract favorite artist or genre
                val favoriteGenreMap = userHist.groupBy { it.mediaType }.mapValues { it.value.size }
                val dominantType = favoriteGenreMap.maxByOrNull { it.value }?.key ?: "AUDIO"
                
                val lastItem = userHist.first()
                replyText = "Análisis cognitivo de Room completado con éxito. Veo que recientemente estuviste disfrutando de '${lastItem.title}' de '${lastItem.artist}'. En base a este patrón del metaverso, te he preparado una sugerencia especial para complementar tu día. ¿Te animas a escucharla? 🤖🎶"
                
                // Seek recommendation item that matches genre or is premium
                val recoItem = viewModel.getFilteredMediaList().firstOrNull { it.id != lastItem.id } 
                    ?: viewModel.getFilteredMediaList().firstOrNull()
                mediaPayload = recoItem
            } else {
                // Fallback suggestion based on temporal context (Hour profile of the device)
                val cal = Calendar.getInstance()
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                replyText = if (hour >= 18 || hour < 6) {
                    "Como ya es de noche, mi algoritmo de descanso te sugiere sintonizar un flujo ambient o videos de naturaleza para restaurar tus ciclos vitales de descanso de manera óptima: 🌌💤"
                } else {
                    "He analizado el huso horario y te sugiero sintonizar ritmos llenos de energía y música tradicional de Artistas Locales para acompañar tu jornada diurna de forma agradable: ☀️🎵"
                }
                
                mediaPayload = viewModel.getFilteredMediaList().firstOrNull { 
                    it.genre.lowercase().contains("folklore") || it.genre.lowercase().contains("synthwave") 
                } ?: viewModel.getFilteredMediaList().firstOrNull()
            }
        }

        // NIGHT MODE & AUTO SLEEP TIMER
        cleanQuery.contains("noche") || cleanQuery.contains("dormir") || cleanQuery.contains("sueño") -> {
            viewModel.startSleepTimer(30) // set a 30 min timer
            val dreamMedia = viewModel.getFilteredMediaList().firstOrNull { 
                it.genre.lowercase().contains("ambient") || it.title.lowercase().contains("cielo")
            } ?: viewModel.getFilteredMediaList().firstOrNull()

            replyText = "Activando el 'Modo Noche Estelar' en todo tu entorno DulcePlay. He atenuado el brillo virtual del panel, programé el apagado automático y fade-out sonoro en 30 minutos exactos, y sintonizaremos ondas delta relajantes para inducir tu sueño de manera terapéutica. ¡Dulces sueños estelares! 🌙🛌🧬"
            mediaPayload = dreamMedia
        }

        // ARTISTAS LOCALES / FOLKLORE TRADICIONAL
        cleanQuery.contains("local") || cleanQuery.contains("artista") || cleanQuery.contains("cultura") || cleanQuery.contains("folclor") -> {
            val localMedia = viewModel.getFilteredMediaList().firstOrNull { 
                it.genre.lowercase().contains("folklore") || it.genre.lowercase().contains("cumbia") 
            } ?: viewModel.getFilteredMediaList().getOrNull(1) ?: viewModel.getFilteredMediaList().firstOrNull()

            replyText = "Fomentando la descentralización artística y la soberanía cultural del metaverso. He seleccionado la pieza '${localMedia?.title}' de '${localMedia?.artist}', un exponente orgánico ineludible con raíces de gran identidad cultural. ¡Arriba la riqueza regional! 🇨🇴🎻💃"
            mediaPayload = localMedia
        }

        // EASY MODE / SENIOR ACCESSIBILITY ENHANCER
        cleanQuery.contains("fácil") || cleanQuery.contains("facil") || cleanQuery.contains("anciano") || cleanQuery.contains("acces") || cleanQuery.contains("abuelo") -> {
            replyText = "¡Hola! Estoy reprogramando la arquitectura visual del app para ti. \n\nHe activado el **Modo Fácil / Accesible**. A partir de ahora verás fuentes de texto gigantescas, botones táctiles masivos de 64dp, un menú totalmente simplificado de listado rápido y lectura vocal fluida. ¡La tecnología debe ser inclusiva para todos! 🥰👴👵"
            actionPayload = "EASY_MODE"
        }

        // DRIVING MODE FOR METAVERSE VEHICLES
        cleanQuery.contains("mane") || cleanQuery.contains("conduc") || cleanQuery.contains("carro") || cleanQuery.contains("auto") -> {
            replyText = "Entorno reprogramado de forma segura en modo 'Cyber Drive'. He maximizado los botones de toque periféricos, deshabilitado distracciones cinéticas y sintonizaremos un stream dinámico de alta velocidad constante para mantener tu foco activo en la autopista virtual. 🚗🛣️⚡"
            actionPayload = "DRIVING_MODE"
            val energeticSong = viewModel.getFilteredMediaList().firstOrNull { it.genre.lowercase().contains("synthwave") } ?: viewModel.getFilteredMediaList().firstOrNull()
            mediaPayload = energeticSong
        }

        // FAMILIAR / PARENTAL GUARD
        cleanQuery.contains("familia") || cleanQuery.contains("niño") || cleanQuery.contains("hijo") || cleanQuery.contains("infantil") -> {
            replyText = "¡Filtro Familiar DulcePlay activado! He purgado los canales dinámicos IPTV que no están calificados como contenido infantil, bloqueado el reproductor de videos de ciencia ficción oscuros y destacado música folclórica e instrumental alegre perfecta para bailar en familia. ¡Privacidad y seguridad infantil garantizada! 👨👩👧👦🧸💖"
            actionPayload = "FAMILY_MODE"
        }

        // WELLNESS / MEDITATION CENTER
        cleanQuery.contains("bienes") || cleanQuery.contains("medit") || cleanQuery.contains("relaj") || cleanQuery.contains("respir") -> {
            replyText = "Santuario de Bienestar de DulcePlay iniciado. Te propongo una meditación consciente guiada. Inhala hondo sintiendo el flujo estelar en tus pulmones... exhala suave en sintonía con las partículas cuánticas de la app. Iniciar música de relajación profunda de fondo: 🧘🧘‍♂️🌸✨"
            actionPayload = "WELLNESS_MODE"
        }

        // PLAYBACK GENERAL CONTROLS
        cleanQuery.contains("pausa") || cleanQuery.contains("deten") || cleanQuery.contains("para") || cleanQuery.contains("silen") -> {
            viewModel.togglePlay()
            replyText = "Comando de sonido ejecutado con éxito. He pausado la reproducción activa del ExoPlayer de forma inmediata."
        }

        else -> {
            replyText = "He decodificado tu mensaje: '" + query + "'. Me parece fascinante, pero mi núcleo de inteligencia se especializa en gestionar multimedia, activar modos inclusivos y acompañar tu estado de ánimo. Prueba pidiéndome: 'modo fácil', 'música para estudiar', 'modo noche' o 'noticias de colombia'. ¡Te van a encantar! 🤖🌐"
        }
    }

    history.add(
        ChatMessage(
            sender = "DULCE_BOT",
            text = replyText,
            suggestedMedia = mediaPayload,
            suggestedChannel = channelPayload,
            specialAction = actionPayload
        )
    )

    setAssetState(AssistantState.IDLE)
}
