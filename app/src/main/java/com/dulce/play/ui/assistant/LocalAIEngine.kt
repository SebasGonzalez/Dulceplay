package com.dulce.play.ui.assistant

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * LocalAIEngine — Motor de IA Local para DulcePlay v3.6.0
 *
 * Usa Google MediaPipe Tasks GenAI para ejecutar Gemma 2B Instruct
 * directamente en el dispositivo Android, sin conexión a internet.
 *
 * Flujo:
 *  1. checkModelReady() → ¿modelo descargado?
 *  2. Si no → downloadModelIfWifi() → descarga solo por Wi-Fi
 *  3. loadModel() → carga en memoria con MediaPipe LlmInference
 *  4. generate(prompt) → respuesta de IA real
 */
object LocalAIEngine {

    private const val TAG = "DULCE_AI"

    // Modelo: Gemma 2B Instruct cuantizado 4 bits (formato .bin compatible MediaPipe)
    // Fuente oficial de Google AI Edge para MediaPipe
    private const val MODEL_URL =
        "https://storage.googleapis.com/mediapipe-models/llm_inference/gemma-2b-it-cpu-int4/float16/1/gemma-2b-it-cpu-int4.bin"
    private const val MODEL_FILENAME = "dulce_ai_gemma2b.bin"
    private const val MODEL_SIZE_BYTES_APPROX = 1_479_000_000L // ~1.4 GB

    // System prompt: convierte Gemma 2B en DULCE-BOT musical
    private const val SYSTEM_PROMPT = """Eres DULCE-BOT, el asistente de inteligencia artificial integrado en DulcePlay, una aplicación de música y video para Android.

Tu personalidad:
- Eres amable, entusiasta y experto en música latinoamericana, vallenato, cumbia, salsa, reggaeton, pop y rock en español.
- Hablas siempre en español de manera natural y cercana.
- Eres experto en recomendaciones musicales: conoces artistas, géneros, ritmos y canciones populares.
- Puedes ayudar con: recomendaciones de música, información sobre artistas, configuración de la app, y conversación general.
- NUNCA generes contenido inapropiado, violento, sexual explícito o que promueva odio.
- Si te preguntan algo fuera de tu área, responde amablemente que estás especializado en música.
- Tus respuestas son concisas (máximo 3-4 párrafos), útiles y siempre termina con una recomendación musical cuando sea relevante.

Recuerda: eres parte de DulcePlay, una app colombiana de música. Prioriza artistas colombianos y latinoamericanos en tus respuestas."""

    // ── Estados ────────────────────────────────────────────────────────────────

    enum class EngineState {
        UNINITIALIZED,     // No iniciado
        CHECKING,          // Verificando si el modelo existe
        DOWNLOADING,       // Descargando modelo
        WAITING_WIFI,      // Esperando conexión Wi-Fi
        LOADING,           // Cargando modelo en memoria
        READY,             // Listo para inferencia
        ERROR              // Error irrecuperable
    }

    private val _state = MutableStateFlow(EngineState.UNINITIALIZED)
    val state: StateFlow<EngineState> = _state.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)  // 0.0 a 1.0
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow("Asistente IA en reposo")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var llmInference: LlmInference? = null

    // ── Archivo del modelo ─────────────────────────────────────────────────────

    private fun getModelFile(context: Context): File =
        File(context.filesDir, MODEL_FILENAME)

    fun isModelDownloaded(context: Context): Boolean {
        val f = getModelFile(context)
        return f.exists() && f.length() > MODEL_SIZE_BYTES_APPROX / 2
    }

    // ── Verificación de Wi-Fi ──────────────────────────────────────────────────

    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    // ── Inicialización principal ───────────────────────────────────────────────

    /**
     * Punto de entrada principal. Llama esto al abrir el chat.
     * Gestiona automáticamente descarga → carga → listo.
     */
    suspend fun initialize(context: Context) {
        if (_state.value == EngineState.READY || _state.value == EngineState.DOWNLOADING) return

        _state.value = EngineState.CHECKING
        _statusMessage.value = "Verificando DULCE-MIND..."

        if (isModelDownloaded(context)) {
            loadModel(context)
        } else {
            if (isWifiConnected(context)) {
                downloadModel(context)
            } else {
                _state.value = EngineState.WAITING_WIFI
                _statusMessage.value = "📶 Conecta a Wi-Fi para descargar DULCE-MIND (~1.4 GB)"
                Log.w(TAG, "Sin Wi-Fi. Esperando para descargar el modelo.")
            }
        }
    }

    /**
     * Reintentar inicialización (cuando el usuario conecta Wi-Fi manualmente)
     */
    suspend fun retryIfWifi(context: Context) {
        if (_state.value == EngineState.WAITING_WIFI && isWifiConnected(context)) {
            initialize(context)
        }
    }

    // ── Descarga del modelo ────────────────────────────────────────────────────

    private suspend fun downloadModel(context: Context) = withContext(Dispatchers.IO) {
        _state.value = EngineState.DOWNLOADING
        _statusMessage.value = "⬇️ Descargando DULCE-MIND... (esto solo ocurre una vez)"
        Log.d(TAG, "Iniciando descarga del modelo Gemma 2B")

        val destFile = getModelFile(context)
        val tempFile = File(context.filesDir, "$MODEL_FILENAME.tmp")

        try {
            val connection = URL(MODEL_URL).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 30000
                readTimeout = 60000
                setRequestProperty("User-Agent", "DulcePlay/3.6 Android")
            }

            val totalBytes = connection.contentLengthLong
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(8192)
            var downloadedBytes = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead

                val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0f
                _downloadProgress.value = progress

                val mb = downloadedBytes / (1024 * 1024)
                val totalMb = if (totalBytes > 0) totalBytes / (1024 * 1024) else 1400
                _statusMessage.value = "⬇️ Descargando DULCE-MIND... ${mb}MB / ${totalMb}MB"
            }

            outputStream.close()
            inputStream.close()
            connection.disconnect()

            // Renombrar temp → archivo final
            tempFile.renameTo(destFile)
            Log.d(TAG, "✅ Modelo descargado correctamente: ${destFile.length()} bytes")

            // Cargar en memoria
            loadModel(context)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error descargando modelo: ${e.message}")
            tempFile.delete()
            _state.value = EngineState.ERROR
            _statusMessage.value = "❌ Error en descarga. Verifica tu conexión Wi-Fi."
        }
    }

    // ── Carga del modelo en memoria ────────────────────────────────────────────

    private suspend fun loadModel(context: Context) = withContext(Dispatchers.IO) {
        _state.value = EngineState.LOADING
        _statusMessage.value = "🧠 Cargando DULCE-MIND en memoria..."
        Log.d(TAG, "Cargando modelo MediaPipe LLM...")

        try {
            val modelFile = getModelFile(context)
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .setResultListener { partialResult, done ->
                    // Para inferencia en streaming (si se usa el modo async)
                    Log.d(TAG, "Partial: $partialResult | Done: $done")
                }
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            _state.value = EngineState.READY
            _statusMessage.value = "✅ DULCE-MIND activo — IA local encendida"
            _downloadProgress.value = 1f
            Log.d(TAG, "✅ Motor de IA listo para inferencia")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cargando modelo: ${e.message}")
            _state.value = EngineState.ERROR
            _statusMessage.value = "❌ Error cargando el motor de IA. Intenta reiniciar la app."
        }
    }

    // ── Inferencia ─────────────────────────────────────────────────────────────

    /**
     * Genera una respuesta de la IA local.
     * @param userMessage El mensaje del usuario
     * @param conversationHistory Historial reciente del chat (últimos N mensajes)
     * @return Respuesta generada por el modelo, o null si el motor no está listo
     */
    suspend fun generate(
        userMessage: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): String? = withContext(Dispatchers.Default) {
        val engine = llmInference ?: run {
            Log.w(TAG, "Motor no inicializado, no se puede generar respuesta")
            return@withContext null
        }

        try {
            // Construir el prompt completo con el formato de Gemma Instruct
            val fullPrompt = buildGemmaPrompt(userMessage, conversationHistory)
            Log.d(TAG, "Generando respuesta para: '${userMessage.take(50)}...'")

            val response = engine.generateResponse(fullPrompt)
            Log.d(TAG, "✅ Respuesta generada: ${response?.take(100)}...")

            return@withContext response?.trim() ?: "No pude generar una respuesta. Inténtalo de nuevo."

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en inferencia: ${e.message}")
            return@withContext "Ocurrió un error procesando tu solicitud. DULCE-MIND está recuperándose."
        }
    }

    /**
     * Construye el prompt en el formato de instrucción de Gemma:
     * <start_of_turn>user\n{mensaje}<end_of_turn>\n<start_of_turn>model\n
     */
    private fun buildGemmaPrompt(
        userMessage: String,
        history: List<Pair<String, String>>
    ): String {
        val sb = StringBuilder()

        // System context (integrado como parte del primer turno de usuario)
        sb.append("<start_of_turn>user\n")
        sb.append(SYSTEM_PROMPT)
        sb.append("\n\n---\n\n")

        // Historial reciente (últimos 3 intercambios para contexto)
        val recentHistory = history.takeLast(3)
        for ((histUser, histBot) in recentHistory) {
            sb.append("Usuario: $histUser\n")
            sb.append("DULCE-BOT: $histBot\n\n")
        }

        // Mensaje actual
        sb.append("Usuario: $userMessage")
        sb.append("<end_of_turn>\n")
        sb.append("<start_of_turn>model\n")
        sb.append("DULCE-BOT:")

        return sb.toString()
    }

    // ── Limpieza de recursos ───────────────────────────────────────────────────

    fun release() {
        llmInference?.close()
        llmInference = null
        _state.value = EngineState.UNINITIALIZED
        Log.d(TAG, "Motor de IA liberado")
    }

    /**
     * Elimina el modelo del almacenamiento (para liberar espacio)
     */
    fun deleteModel(context: Context): Boolean {
        val f = getModelFile(context)
        return if (f.exists()) {
            f.delete().also { deleted ->
                if (deleted) {
                    _state.value = EngineState.UNINITIALIZED
                    _statusMessage.value = "Modelo eliminado del almacenamiento"
                    Log.d(TAG, "Modelo eliminado")
                }
            }
        } else false
    }

    /**
     * Retorna el tamaño del modelo descargado en MB
     */
    fun getModelSizeMb(context: Context): Long {
        val f = getModelFile(context)
        return if (f.exists()) f.length() / (1024 * 1024) else 0L
    }
}
