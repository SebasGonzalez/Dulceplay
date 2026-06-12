package com.dulce.play.ui.assistant

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
import com.dulce.play.domain.model.IPTVChannel
import com.dulce.play.domain.model.MediaItem
import com.dulce.play.domain.model.MediaType
import com.dulce.play.ui.components.GlassBox
import com.dulce.play.ui.player.PlayerViewModel
import com.dulce.play.ui.theme.*
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
                text = "¡Hola ${profile.name}! Soy DULCE-BOT, tu copiloto multimedia con Inteligencia Artificial local.\n\nPuedo responderte preguntas de música, recomendar canciones y ayudarte con la app. Por ejemplo:\n\n* 'Recomïiéndame 3 canciones de vallenato'\n* 'Quién es Carlos Vives?'\n* 'Música para estudiar'\n* 'Activa modo fácil'"
            )
        )
    }

    // Estado del motor de IA
    val aiEngineState by LocalAIEngine.state.collectAsState()
    val aiDownloadProgress by LocalAIEngine.downloadProgress.collectAsState()
    val aiStatusMessage by LocalAIEngine.statusMessage.collectAsState()

    // Iniciar el motor de IA al abrir el chat
    LaunchedEffect(Unit) {
        LocalAIEngine.initialize(context)
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

                        // --- Panel de Estado de IA Local ---
                        when (aiEngineState) {
                            LocalAIEngine.EngineState.DOWNLOADING -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0D1B2A))
                                        .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = AccentCyan
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = aiStatusMessage,
                                            color = AccentCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { aiDownloadProgress },
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                        color = AccentCyan,
                                        trackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                    Text(
                                        text = "${(aiDownloadProgress * 100).toInt()}% — Solo por Wi-Fi, en segundo plano",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            LocalAIEngine.EngineState.WAITING_WIFI -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1A0D00))
                                        .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .clickable { scope.launch { LocalAIEngine.retryIfWifi(context) } }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📡", fontSize = 18.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Conecta Wi-Fi para descargar DULCE-MIND",
                                            color = Color(0xFFFF9800),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Modelo IA: ~1.4 GB — Toca aquí para reintentar",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                            LocalAIEngine.EngineState.LOADING -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = PrimaryNeon)
                                    Spacer(Modifier.width(8.dp))
                                    Text("🧠 Cargando DULCE-MIND en memoria...", color = PrimaryNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            LocalAIEngine.EngineState.READY -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF001A0D))
                                        .border(1.dp, Color.Green.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🟢", fontSize = 12.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "DULCE-MIND activo — IA Local encendida",
                                        color = Color.Green,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            LocalAIEngine.EngineState.ERROR -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1A0000))
                                        .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .clickable { scope.launch { LocalAIEngine.initialize(context) } }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("❌", fontSize = 12.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text("Error en DULCE-MIND", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("Toca para reintentar", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                                    }
                                }
                            }
                            else -> { /* UNINITIALIZED o CHECKING: no mostrar nada */ }
                        }

                        if (aiEngineState != LocalAIEngine.EngineState.UNINITIALIZED &&
                            aiEngineState != LocalAIEngine.EngineState.CHECKING) {
                            Spacer(Modifier.height(8.dp))
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
 * processAssistantQuery v2 — Motor híbrido: IA Real + Reglas de fallback
 *
 * Prioridad:
 *  1. Si el motor LocalAIEngine está READY → usa Gemma 2B para generar respuesta real
 *  2. Si no está listo → usa las reglas programadas como fallback (comandos de acción)
 *  3. Los comandos de acción (modo fácil, pausa, buscar) SIEMPRE se ejecutan sin importar el modo
 */
suspend fun processAssistantQuery(
    query: String,
    viewModel: PlayerViewModel,
    history: MutableList<ChatMessage>,
    setAssetState: (AssistantState) -> Unit
) {
    // 1. Añadir mensaje del usuario
    history.add(ChatMessage(sender = "USER", text = query))
    setAssetState(AssistantState.THINKING)
    delay(300) // Breve pausa visual
    setAssetState(AssistantState.SPEAKING)

    val cleanQuery = query.trim().lowercase()

    // ── COMANDOS DE ACCIÓN (siempre se ejecutan, con o sin IA) ─────────────────
    // Estos comandos ejecutan funciones reales de la app primero,
    // luego la IA genera la respuesta o se usa texto predefinido.

    var actionPayload: String? = null
    var mediaPayload: MediaItem? = null
    var channelPayload: IPTVChannel? = null
    var forceRuleText: String? = null // Si se establece, se usa en vez de la IA

    when {
        // Comandos de búsqueda → siempre ejecutar la acción
        cleanQuery.startsWith("busca ") || cleanQuery.startsWith("buscar ") || cleanQuery.startsWith("search ") -> {
            val term = query.replace("(?i)^(busca|buscar|search)\\s+".toRegex(), "").trim()
            viewModel.updateSearchQuery(term)
            viewModel.setSearchOverlayActive(true)
            forceRuleText = "🔎 Buscando \"$term\" en YouTube ahora mismo..."
        }
        // Pausa/play → ejecutar inmediatamente
        cleanQuery.contains("pausa") || cleanQuery.contains("para la música") || cleanQuery.contains("silencio") -> {
            viewModel.togglePlay()
            forceRuleText = "⏸️ Reproducción pausada."
        }
        // Modo fácil → activar y notificar
        cleanQuery.contains("modo fácil") || cleanQuery.contains("modo facil") -> {
            actionPayload = "EASY_MODE"
        }
        // Modo conducción
        cleanQuery.contains("modo manejo") || cleanQuery.contains("modo conduccion") || cleanQuery.contains("modo carro") -> {
            actionPayload = "DRIVING_MODE"
        }
        // Modo familiar
        cleanQuery.contains("modo familia") || cleanQuery.contains("modo niños") -> {
            actionPayload = "FAMILY_MODE"
        }
        // Modo bienestar
        cleanQuery.contains("modo bienestar") || cleanQuery.contains("meditación") -> {
            actionPayload = "WELLNESS_MODE"
        }
        // Temporizador de sueño
        cleanQuery.contains("modo noche") || cleanQuery.contains("dormir en") -> {
            viewModel.startSleepTimer(30)
            forceRuleText = "🌙 Temporizador de sueño activado: la música se apagará en 30 minutos."
        }
    }

    // ── GENERAR RESPUESTA DE TEXTO ──────────────────────────────────────────────

    val replyText: String

    if (forceRuleText != null) {
        // Comando de acción simple → usar texto predefinido
        replyText = forceRuleText
    } else if (LocalAIEngine.state.value == LocalAIEngine.EngineState.READY) {
        // ✅ IA REAL disponible → generar respuesta con Gemma 2B
        // Construir historial de conversación para contexto
        val conversationContext = history
            .takeLast(6) // Últimos 3 intercambios
            .filter { it.sender != "USER" || it.text != query } // excluir el mensaje actual
            .chunked(2)
            .mapNotNull { pair ->
                val userMsg = pair.firstOrNull { it.sender == "USER" }?.text
                val botMsg = pair.firstOrNull { it.sender == "DULCE_BOT" }?.text
                if (userMsg != null && botMsg != null) userMsg to botMsg else null
            }

        val aiResponse = LocalAIEngine.generate(
            userMessage = query,
            conversationHistory = conversationContext
        )

        replyText = aiResponse ?: "¡Hola! Estoy aquí para ayudarte con música y ms. ¿En qué te puedo asistir?"
    } else {
        // ⚠️ IA no disponible → fallback con reglas tradicionales
        replyText = generateRuleBasedResponse(
            cleanQuery = cleanQuery,
            query = query,
            viewModel = viewModel,
            aiState = LocalAIEngine.state.value
        )
    }

    // Añadir respuesta del bot al historial
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

/**
 * Fallback: sistema de reglas original para cuando la IA no está disponible.
 * También informa al usuario sobre el estado del motor de IA.
 */
private fun generateRuleBasedResponse(
    cleanQuery: String,
    query: String,
    viewModel: PlayerViewModel,
    aiState: LocalAIEngine.EngineState
): String {
    val aiStatusPrefix = when (aiState) {
        LocalAIEngine.EngineState.DOWNLOADING -> "📥 [Descargando DULCE-MIND... Respondiendo con modo básico]\n\n"
        LocalAIEngine.EngineState.WAITING_WIFI -> "📡 [Sin Wi-Fi para descargar IA. Modo básico activo]\n\n"
        LocalAIEngine.EngineState.LOADING -> "🧠 [Cargando IA en memoria... Respondiendo con modo básico]\n\n"
        LocalAIEngine.EngineState.ERROR -> "❌ [Error en IA local. Modo básico activo]\n\n"
        else -> ""
    }

    val response = when {
        cleanQuery.contains("estudiar") || cleanQuery.contains("concentra") -> {
            val m = viewModel.getFilteredMediaList().firstOrNull()
            "¿Quieres música para concentrarte? Activando modo estudio. ¿Necesitas algo más?"
        }
        cleanQuery.contains("noticia") -> {
            val ch = viewModel.getAllIPTVChannels().firstOrNull { it.name.lowercase().contains("noticia") }
            if (ch != null) "Buscando noticias en IPTV..." else "No encontré canales de noticias configurados."
        }
        cleanQuery.contains("vallenato") || cleanQuery.contains("cumbia") || cleanQuery.contains("salsa") -> {
            "Me encantaría recomendarte música, pero mi motor de IA necesita descargarse primero para darte recomendaciones personalizadas. Una vez que DULCE-MIND se descargue por Wi-Fi, podré responder preguntas musicales con inteligencia real. 🎙️"
        }
        cleanQuery.contains("colombia") -> {
            "Colombia tiene música increible: Carlos Vives, Shakira, J Balvin, Maluma... ¿Busco alguno en YouTube?"
        }
        else -> {
            "Hola! Soy DULCE-BOT en modo básico. Puedo ayudarte con: buscar canciones, pausar/reproducir, y activar modos especiales. Mi motor de IA completo (DULCE-MIND) se activará cuando haya Wi-Fi disponible. 🤖"
        }
    }

    return aiStatusPrefix + response
}
