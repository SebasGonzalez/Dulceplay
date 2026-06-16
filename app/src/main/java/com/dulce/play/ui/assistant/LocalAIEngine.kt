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
 * LocalAIEngine — Motor de IA Local para DulcePlay v3.8.0 (VERSIÓN DE ORO)
 *
 * OPTIMIZACIÓN CRÍTICA v3.8.0:
 *  - Carga ÚNICA: El motor se inicializa UNA SOLA VEZ y permanece en memoria.
 *  - NUNCA se recarga al cambiar de pantalla, minimizar o volver a la app.
 *  - Se libera SOLO cuando la app se cierra completamente (onDestroy de Application).
 *  - isInitializing flag evita llamadas concurrentes de initialize().
 *  - Al volver al chat, si ya está READY, muestra "Cerebro Activo" 🟢 instantáneamente.
 */
object LocalAIEngine {

    private const val TAG = "DULCE_AI"

    private const val MODEL_URL =
        "https://huggingface.co/autoocrat0413/gemma-2b-it-gpu-int4-mediapipe/resolve/main/gemma-2b-it-gpu-int4.bin"
    private const val MODEL_FILENAME = "dulce_ai_gemma2b.bin"
    private const val MODEL_SIZE_BYTES_APPROX = 1_354_301_440L

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
        UNINITIALIZED,
        CHECKING,
        DOWNLOADING,
        WAITING_WIFI,
        LOADING,
        READY,
        ERROR
    }

    private val _state = MutableStateFlow(EngineState.UNINITIALIZED)
    val state: StateFlow<EngineState> = _state.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow("Asistente IA en reposo")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var llmInference: LlmInference? = null

    /**
     * FLAG CRÍTICO: Evita que initialize() se ejecute múltiples veces simultáneamente.
     * Esto resuelve el problema de "Cargando cerebro..." cada vez que se entra al chat.
     */
    @Volatile
    private var isInitializing = false

    // ── Archivo del modelo ─────────────────────────────────────────────────────

    private fun getModelFile(context: Context): File =
        File(context.filesDir, MODEL_FILENAME)

    fun isModelDownloaded(context: Context): Boolean {
        val f = getModelFile(context)
        return f.exists() && f.length() > MODEL_SIZE_BYTES_APPROX / 2
    }

    // ── Verificación de Wi-Fi ──────────────────────────────────────────────────

    fun isWifiConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (e: Exception) {
            Log.w(TAG, "Error verificando Wi-Fi: ${e.message}")
            false
        }
    }

    // ── Inicialización principal ───────────────────────────────────────────────

    /**
     * OPTIMIZACIÓN CLAVE v3.8.0:
     * - Si ya está READY → NO hace nada. Retorna inmediatamente. ✅
     * - Si ya está DOWNLOADING/LOADING → NO interrumpe. Retorna inmediatamente. ✅
     * - Si isInitializing == true → NO inicia segunda carga. Retorna inmediatamente. ✅
     * - Solo carga desde cero si UNINITIALIZED, WAITING_WIFI o ERROR.
     */
    suspend fun initialize(context: Context) {
        // GUARD 1: Si ya está listo, no hacer nada en absoluto
        if (_state.value == EngineState.READY) {
            Log.d(TAG, "✅ Motor ya está LISTO. Sin necesidad de recarga.")
            return
        }

        // GUARD 2: Si ya está en proceso (descargando o cargando), no interrumpir
        if (_state.value == EngineState.DOWNLOADING || _state.value == EngineState.LOADING) {
            Log.d(TAG, "⏳ Motor en proceso (${_state.value}). Esperando...")
            return
        }

        // GUARD 3: Evitar llamadas concurrentes
        if (isInitializing) {
            Log.d(TAG, "🔒 Ya hay una inicialización en curso. Ignorando llamada duplicada.")
            return
        }

        isInitializing = true
        try {
            _state.value = EngineState.CHECKING
            _statusMessage.value = "Verificando DULCE-MIND..."

            if (isModelDownloaded(context)) {
                // El modelo ya existe — solo cargar en memoria
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
        } finally {
            isInitializing = false
        }
    }

    /**
     * Reintentar solo si está en estado WAITING_WIFI y hay conexión.
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
            var connection = URL(MODEL_URL).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 30000
                readTimeout = 60000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "DulcePlay/3.8 Android")
            }

            var responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == 307 || responseCode == 308) {
                val newUrl = connection.getHeaderField("Location")
                Log.d(TAG, "Redireccionando descarga a: $newUrl")
                connection = URL(newUrl).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 30000
                    readTimeout = 60000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "DulcePlay/3.8 Android")
                }
                responseCode = connection.responseCode
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Error HTTP de descarga: $responseCode")
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

            tempFile.renameTo(destFile)
            Log.d(TAG, "✅ Modelo descargado correctamente: ${destFile.length()} bytes")

            loadModel(context)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error descargando modelo: ${e.message}")
            tempFile.delete()
            _state.value = EngineState.ERROR
            _statusMessage.value = "❌ Error en descarga. Verifica tu conexión Wi-Fi."
        }
    }

    // ── Carga del modelo en memoria ────────────────────────────────────────────

    /**
     * OPTIMIZACIÓN: Verifica una vez más si ya está listo antes de cargar.
     * Esto evita cargas duplicadas en condiciones de carrera.
     */
    private suspend fun loadModel(context: Context) = withContext(Dispatchers.IO) {
        // Double-check: si ya está READY (por condición de carrera), salir
        if (_state.value == EngineState.READY && llmInference != null) {
            Log.d(TAG, "✅ Motor ya cargado (double-check). Omitiendo carga.")
            return@withContext
        }

        _state.value = EngineState.LOADING
        _statusMessage.value = "🧠 Cargando DULCE-MIND en memoria..."
        Log.d(TAG, "Cargando modelo MediaPipe LLM...")

        try {
            val modelFile = getModelFile(context)
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            _state.value = EngineState.READY
            _statusMessage.value = "🟢 Cerebro Activo — IA encendida"
            _downloadProgress.value = 1f
            Log.d(TAG, "✅ Motor de IA listo para inferencia")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cargando modelo: ${e.message}")
            _state.value = EngineState.ERROR
            _statusMessage.value = "❌ Error cargando el motor de IA. Intenta reiniciar la app."
        }
    }

    // ── Inferencia ─────────────────────────────────────────────────────────────

    suspend fun generate(
        userMessage: String,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        currentMediaInfo: String = "",
        recentMediaList: List<String> = emptyList()
    ): String? = withContext(Dispatchers.Default) {
        val engine = llmInference ?: run {
            Log.w(TAG, "Motor no inicializado, no se puede generar respuesta")
            return@withContext null
        }

        try {
            val fullPrompt = buildGemmaPrompt(userMessage, conversationHistory, currentMediaInfo, recentMediaList)
            Log.d(TAG, "Generando respuesta para: '${userMessage.take(50)}...'")

            val response = engine.generateResponse(fullPrompt)
            Log.d(TAG, "✅ Respuesta generada: ${response?.take(100)}...")

            return@withContext response?.trim() ?: "No pude generar una respuesta. Inténtalo de nuevo."

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en inferencia: ${e.message}")
            return@withContext "Ocurrió un error procesando tu solicitud. DULCE-MIND está recuperándose."
        }
    }

    private fun buildGemmaPrompt(
        userMessage: String,
        history: List<Pair<String, String>>,
        currentMediaInfo: String,
        recentMediaList: List<String>
    ): String {
        val sb = StringBuilder()

        sb.append("<start_of_turn>user\n")
        sb.append(SYSTEM_PROMPT)

        if (currentMediaInfo.isNotBlank()) {
            sb.append("\n\n[REPRODUCTOR ACTUAL] Actualmente el usuario escucha: $currentMediaInfo")
        }
        if (recentMediaList.isNotEmpty()) {
            sb.append("\n[HISTORIAL DE SESIÓN] Temas reproducidos recientemente: ${recentMediaList.joinToString(", ")}")
        }

        sb.append("\n\n---\n\n")

        val recentHistory = history.takeLast(3)
        for ((histUser, histBot) in recentHistory) {
            sb.append("Usuario: $histUser\n")
            sb.append("DULCE-BOT: $histBot\n\n")
        }

        sb.append("Usuario: $userMessage")
        sb.append("<end_of_turn>\n")
        sb.append("<start_of_turn>model\n")
        sb.append("DULCE-BOT:")

        return sb.toString()
    }

    // ── Limpieza de recursos ───────────────────────────────────────────────────

    /**
     * SOLO llamar cuando la Application se destruye completamente.
     * NUNCA llamar al minimizar, cambiar de pantalla o volver al chat.
     */
    fun release() {
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error cerrando LlmInference: ${e.message}")
        }
        llmInference = null
        isInitializing = false
        _state.value = EngineState.UNINITIALIZED
        Log.d(TAG, "Motor de IA liberado (cierre completo de app)")
    }

    fun deleteModel(context: Context): Boolean {
        val f = getModelFile(context)
        return if (f.exists()) {
            f.delete().also { deleted ->
                if (deleted) {
                    llmInference?.close()
                    llmInference = null
                    _state.value = EngineState.UNINITIALIZED
                    _statusMessage.value = "Modelo eliminado del almacenamiento"
                    Log.d(TAG, "Modelo eliminado")
                }
            }
        } else false
    }

    fun getModelSizeMb(context: Context): Long {
        val f = getModelFile(context)
        return if (f.exists()) f.length() / (1024 * 1024) else 0L
    }
}
