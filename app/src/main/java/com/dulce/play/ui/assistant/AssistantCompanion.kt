package com.dulce.play.ui.assistant

import android.content.Intent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.os.Bundle
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
import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import com.google.firebase.vertexai.type.content
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

enum class AssistantState { IDLE, LISTENING, THINKING, SPEAKING }
data class ChatMessage(val id: String = UUID.randomUUID().toString(), val sender: String, val text: String, val suggestedMedia: MediaItem? = null, val suggestedChannel: IPTVChannel? = null)

@Composable
fun AssistantFloatingButton(viewModel: PlayerViewModel, modifier: Modifier = Modifier, onNavigateToPlayer: () -> Unit = {}) {
    var showAssistantDialog by remember { mutableStateOf(false) }
    val voiceEnabled by viewModel.voiceActivationEnabled.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    val tts = remember { var instance: TextToSpeech? = null; instance = TextToSpeech(context) { if (it == TextToSpeech.SUCCESS) instance?.language = Locale("es", "ES") }; instance }
    DisposableEffect(Unit) { onDispose { tts?.stop(); tts?.shutdown() } }

    val pLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (!it && voiceEnabled) { Toast.makeText(context, "⚠️ Permiso de Micrófono necesario", Toast.LENGTH_SHORT).show(); viewModel.toggleVoiceActivation() } }
    LaunchedEffect(voiceEnabled) { if (voiceEnabled) pLauncher.launch(android.Manifest.permission.RECORD_AUDIO) }

    DisposableEffect(voiceEnabled) {
        var recognizer: SpeechRecognizer? = null
        if (voiceEnabled) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES"); putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) }
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(r: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(e: Int) { if (voiceEnabled) scope.launch { delay(1000); try { recognizer?.startListening(intent) } catch(x:Exception){} } }
                override fun onResults(r: Bundle?) { r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { if (it.lowercase().contains("dulce")) showAssistantDialog = true }; if (voiceEnabled) recognizer?.startListening(intent) }
                override fun onPartialResults(r: Bundle?) { r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { if (it.lowercase().contains("dulce")) showAssistantDialog = true } }
                override fun onEvent(et: Int, p: Bundle?) {}
            })
            try { recognizer.startListening(intent) } catch(x:Exception){}
        }
        onDispose { recognizer?.stopListening(); recognizer?.destroy() }
    }

    val scale by rememberInfiniteTransition(label="").animateFloat(1f, 1.15f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label="")
    Box(modifier = modifier.padding(bottom = 100.dp, end = 16.dp).size(60.dp).graphicsLayer { scaleX = scale; scaleY = scale }.background(Brush.radialGradient(if (voiceEnabled) listOf(Color.Red, accentColor, Color.Transparent) else listOf(accentColor, accentColor.copy(alpha = 0.8f), Color.Transparent)), CircleShape).border(2.dp, if (voiceEnabled) Color.Red else Color.White.copy(alpha = 0.6f), CircleShape).clickable { showAssistantDialog = true }, contentAlignment = Alignment.Center) {
        Icon(if (voiceEnabled) Icons.Rounded.Mic else Icons.Rounded.SmartToy, null, tint = Color.White, modifier = Modifier.size(28.dp))
    }
    if (showAssistantDialog) IntelligenceCenterDialog(viewModel, { showAssistantDialog = false }, onNavigateToPlayer, tts)
}

@Composable
fun IntelligenceCenterDialog(viewModel: PlayerViewModel, onDismiss: () -> Unit, onNavigateToPlayer: () -> Unit, tts: TextToSpeech?) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }; var state by remember { mutableStateOf(AssistantState.IDLE) }
    val profile by viewModel.currentProfile.collectAsState(); val accentColor by viewModel.accentColor.collectAsState()
    val chat = remember { mutableStateListOf(ChatMessage(sender = "DULCE", text = "¡Hola ${profile.name}! Soy Dulce. ¿Qué sintonizamos? 🔊")) }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res -> if (res.resultCode == android.app.Activity.RESULT_OK) res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { scope.launch { processAssistantQuery(it, viewModel, chat, { state = it }, onNavigateToPlayer, tts) } } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), color = Color.Transparent) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                GlassBox(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).border(1.dp, accentColor.copy(0.3f), RoundedCornerShape(28.dp)), cornerRadius = 28.dp) {
                    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AnimatedOrbVisual(state, accentColor)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column { Text("DULCE", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black); Text(if (state == AssistantState.LISTENING) "Escuchando..." else "Activa", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            }
                            IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, null, tint = Color.White) }
                        }
                        Divider(color = Color.White.copy(0.1f), modifier = Modifier.padding(vertical = 12.dp))
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) { LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(chat) { msg -> ChatBubble(msg, { viewModel.playMedia(it); onDismiss(); onNavigateToPlayer() }, { viewModel.playIPTVChannel(it); onDismiss(); onNavigateToPlayer() }, accentColor) } } }
                        Row(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(Color.Black).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(28.dp)), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { val it = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES") }; state = AssistantState.LISTENING; try { speechLauncher.launch(it) } catch(e:Exception){} }, modifier = Modifier.padding(start = 6.dp).background(accentColor, CircleShape).size(42.dp)) { Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                            TextField(value = input, onValueChange = { input = it }, placeholder = { Text("Dime algo...", color = Color.White.copy(0.3f), fontSize = 13.sp) }, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), modifier = Modifier.weight(1f))
                            IconButton(onClick = { if (input.isNotBlank()) { val q = input; input = ""; scope.launch { processAssistantQuery(q, viewModel, chat, { state = it }, onNavigateToPlayer, tts) } } }) { Icon(Icons.Default.Send, null, tint = accentColor) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedOrbVisual(s: AssistantState, acc: Color) {
    val p by rememberInfiniteTransition(label="").animateFloat(0.8f, 1.2f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label="")
    val c = when(s) { AssistantState.LISTENING -> Color.Red; AssistantState.THINKING -> PremiumGold; else -> acc }
    Box(modifier = Modifier.size(34.dp).graphicsLayer { scaleX = p; scaleY = p }.background(Brush.sweepGradient(listOf(c, DeepPurple, c)), CircleShape).border(1.5.dp, Color.White.copy(0.8f), CircleShape), contentAlignment = Alignment.Center) { Box(modifier = Modifier.size(12.dp).background(Color.White, CircleShape)) }
}

@Composable
fun ChatBubble(m: ChatMessage, tm: (MediaItem) -> Unit, tc: (IPTVChannel) -> Unit, acc: Color) {
    val user = m.sender == "USER"
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Column(modifier = Modifier.fillMaxWidth(0.85f), horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
            Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (user) Brush.linearGradient(listOf(DeepPurple, acc)) else Brush.linearGradient(listOf(Color.White.copy(0.08f), Color.White.copy(0.03f)))).padding(12.dp)) { Text(m.text, color = Color.White, fontSize = 13.sp, fontWeight = if (user) FontWeight.Bold else FontWeight.Medium) }
            m.suggestedMedia?.let { s -> Button(onClick = { tm(s) }, modifier = Modifier.padding(top = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = acc)) { Text("PONER ${s.title}", fontSize = 10.sp) } }
            m.suggestedChannel?.let { c -> Button(onClick = { tc(c) }, modifier = Modifier.padding(top = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = PremiumGold)) { Text("SINTONIZAR ${c.name}", fontSize = 10.sp, color = Color.Black) } }
        }
    }
}

suspend fun processAssistantQuery(q: String, vm: PlayerViewModel, hist: MutableList<ChatMessage>, ss: (AssistantState) -> Unit, nav: () -> Unit, tts: TextToSpeech?) {
    val cq = q.trim().lowercase(); val hasD = cq.contains("dulce"); val aq = if (hasD) cq.substringAfter("dulce").trim() else cq
    hist.add(ChatMessage(sender = "USER", text = q)); ss(AssistantState.THINKING); delay(1000)
    var reply = ""; var speak = true
    
    if (hasD || !vm.voiceActivationEnabled.value) {
        reply = "🔊 Te escucho... "
        when {
            aq.contains("vallenato") -> { reply += "¡Claro! Vallenato puro para ti 🪗"; vm.executeSearch("Vallenato") }
            aq.contains("rap") -> { reply += "Activando el mejor Rap 🎤"; vm.executeSearch("Rap") }
            aq.contains("salsa") -> { reply += "Poniendo Salsa brava 💃"; vm.executeSearch("Salsa") }
            aq.contains("relajante") || aq.contains("suave") || aq.contains("dormir") -> { reply += "Buscando algo suave y relajante para ti"; vm.executeSearch("Relaxing Acoustic") }
            aq.contains("entrenar") || aq.contains("ejercic") || aq.contains("fuerte") -> { reply += "¡Vamos con toda! Poniendo música movida"; vm.executeSearch("Workout") }
            aq.contains("sube el volumen") -> { vm.adjustVolume(0.2f); reply += "Volumen subido" }
            aq.contains("baja el volumen") -> { vm.adjustVolume(-0.2f); reply += "Volumen bajado" }
            aq.contains("siguiente") -> { vm.next(); reply += "Poniendo la siguiente" }
            aq.contains("pausa") -> { vm.togglePlay(); reply += "Pausado" }
            aq.contains("quién te creó") -> reply = "Fui creada por un genio colombiano 🇨🇴💙"
            else -> {
                try {
                    val res = Firebase.vertexAI.generativeModel("gemini-1.5-flash").generateContent(content { text("Eres DULCE. Responde muy breve en español a: '$aq'. Si quiere música/video, pon [BUSCAR] seguido del tema. IMPORTANTE: NO menciones duración.") })
                    val txt = res.text ?: ""
                    if (txt.contains("[BUSCAR]")) { val t = txt.substringAfter("[BUSCAR]").trim(); vm.executeSearch(t); reply += "Buscando '$t'..." } else reply += txt
                } catch (e: Exception) { reply += "Procesando '$aq'..." }
            }
        }
    } else reply = "Dime 'Dulce' antes de tu orden"

    if (speak && tts != null && vm.voiceType.value != "silent") tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null)
    hist.add(ChatMessage(sender = "DULCE", text = reply))
    ss(AssistantState.IDLE)
}
