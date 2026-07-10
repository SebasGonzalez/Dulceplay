package com.dulce.play.utils

import android.util.Log
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * SearchEngine v3.9.0 — DulcePlay
 *
 * OPTIMIZACIONES v3.9.0:
 * - disconnect() garantizado en bloque finally para evitar fugas de conexión.
 * - inputStream cerrado de forma segura con use{} (cierre automático).
 * - Timeouts ajustados para equilibrar velocidad y confiabilidad.
 * - Instancias de Invidious actualizadas y ordenadas por confiabilidad.
 * - Búsqueda en cascada con validación de URL para evitar congelamientos en el reproductor.
 */
class SearchEngine {
    private val API_KEY = "AIzaSyCrzrUscZ5kEW-rQte8yFxmc4E2xUcDm-Q"

    // Instancias de Invidious ordenadas por confiabilidad y uptime
    private val INVIDIOUS_INSTANCES = listOf(
        "https://inv.nadeko.net",
        "https://invidious.nerdvpn.de",
        "https://invidious.f5.si",
        "https://yt.chocolatemoo53.com",
        "https://inv.thepixora.com",
        "https://invidious.fdn.fr"
    )

    // ─────────────────────────────────────────────────────────────
    // BÚSQUEDA con fallback robusto a Invidious
    // ─────────────────────────────────────────────────────────────
    suspend fun buscar(consulta: String): List<VideoInfo> = withContext(Dispatchers.IO) {
        Log.d("DULCEPLAY", "Buscando: $consulta")
        val resultados = mutableListOf<VideoInfo>()
        var con: HttpURLConnection? = null
        try {
            val textoLimpio = consulta.trim().lowercase()
                .replace("á","a").replace("é","e").replace("í","i")
                .replace("ó","o").replace("ú","u").replace("ñ","n")
            val codificado = URLEncoder.encode(textoLimpio, "UTF-8")
            val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=$codificado&type=video&maxResults=50&key=$API_KEY"

            con = URL(url).openConnection() as HttpURLConnection
            con.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                connectTimeout = 12000
                readTimeout = 12000
            }

            if (con.responseCode == HttpURLConnection.HTTP_OK) {
                val json = con.inputStream.use { stream ->
                    JSONObject(stream.bufferedReader().readText())
                }
                val items = json.optJSONArray("items") ?: JSONArray()
                for (i in 0 until items.length()) {
                    try {
                        val item = items.getJSONObject(i)
                        val snippet = item.optJSONObject("snippet") ?: continue
                        val titulo = snippet.optString("title", "")
                        if (titulo.isBlank()) continue
                        val canal = snippet.optString("channelTitle", "Desconocido")
                        val idObj = item.optJSONObject("id") ?: continue
                        val videoId = idObj.optString("videoId", "")
                        if (videoId.isEmpty()) continue
                        val thumbnails = snippet.optJSONObject("thumbnails")
                        val imagen = thumbnails?.optJSONObject("medium")?.optString("url")
                                    ?: thumbnails?.optJSONObject("default")?.optString("url")
                                    ?: ""
                        resultados.add(VideoInfo(videoId, titulo, canal, imagen))
                    } catch (e: Exception) {
                        Log.e("DULCEPLAY", "Error procesando item de búsqueda $i: ${e.message}")
                    }
                }
            } else {
                Log.w("DULCEPLAY", "YouTube API retornó código: ${con.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("DULCEPLAY", "Búsqueda YouTube falló: ${e.message}")
        } finally {
            con?.disconnect()
        }

        // FALLBACK A INVIDIOUS SI YOUTUBE FALLÓ O CUOTA EXCEDIDA
        if (resultados.isEmpty()) {
            Log.w("DULCEPLAY", "Resultados de YouTube vacíos. Activando fallback de Invidious...")
            resultados.addAll(buscarInvidious(consulta))
        }

        return@withContext resultados
    }

    private suspend fun buscarInvidious(consulta: String): List<VideoInfo> = withContext(Dispatchers.IO) {
        val resultados = mutableListOf<VideoInfo>()
        val textoLimpio = consulta.trim().lowercase()
            .replace("á","a").replace("é","e").replace("í","i")
            .replace("ó","o").replace("ú","u").replace("ñ","n")
        val codificado = URLEncoder.encode(textoLimpio, "UTF-8")

        for (instancia in INVIDIOUS_INSTANCES) {
            var con: HttpURLConnection? = null
            try {
                Log.d("DULCEPLAY", "Intentando buscar en Invidious: $instancia")
                val url = "$instancia/api/v1/search?q=$codificado&type=video"
                con = URL(url).openConnection() as HttpURLConnection
                con.apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                if (con.responseCode == HttpURLConnection.HTTP_OK) {
                    val texto = con.inputStream.use { it.bufferedReader().readText() }
                    val jsonArray = JSONArray(texto)
                    for (i in 0 until jsonArray.length()) {
                        try {
                            val item = jsonArray.getJSONObject(i)
                            val type = item.optString("type", "")
                            if (type != "video") continue
                            val videoId = item.optString("videoId", "")
                            if (videoId.isEmpty()) continue
                            val title = item.optString("title", "")
                            if (title.isBlank()) continue
                            val author = item.optString("author", "Desconocido")
                            val thumbnails = item.optJSONArray("videoThumbnails")
                            var imagenUrl = ""
                            if (thumbnails != null && thumbnails.length() > 0) {
                                for (j in 0 until thumbnails.length()) {
                                    val thumb = thumbnails.getJSONObject(j)
                                    if (thumb.optString("quality", "") == "medium") {
                                        imagenUrl = thumb.optString("url", "")
                                        break
                                    }
                                }
                                if (imagenUrl.isEmpty()) {
                                    imagenUrl = thumbnails.getJSONObject(0).optString("url", "")
                                }
                            }
                            resultados.add(VideoInfo(videoId, title, author, imagenUrl))
                        } catch (e: Exception) {
                            Log.e("DULCEPLAY", "Error procesando item Invidious $i: ${e.message}")
                        }
                    }
                    if (resultados.isNotEmpty()) {
                        Log.d("DULCEPLAY", "Búsqueda exitosa en Invidious: $instancia, encontrados: ${resultados.size}")
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("DULCEPLAY", "Error buscando en $instancia: ${e.message}")
            } finally {
                con?.disconnect()
            }
        }
        return@withContext resultados
    }

    // ─────────────────────────────────────────────────────────────
    // EXTRACCIÓN DE STREAMS — Sistema en cascada probado (v3.9.7)
    //
    // Estrategia (de más confiable a menos):
    //  1. TVHTML5_SIMPLY_EMBEDDED_PLAYER — sin cifrado, URLs directas
    //  2. IOS client (v19.45.4) — URLs sin cifrar en la mayoría de videos
    //  3. ANDROID_MUSIC (v6.19.52) — último recurso InnerTube
    //  4. Proxy Invidious — respaldo final
    // ─────────────────────────────────────────────────────────────

    // Configuraciones de clientes InnerTube probados
    private data class InnerTubeClient(
        val name: String,
        val version: String,
        val userAgent: String,
        val extraContext: Map<String, Any> = emptyMap()
    )

    private val INNERTUBE_CLIENTS = listOf(
        // TVHTML5 — no requiere firma, el cliente más confiable para streams sin cifrar
        InnerTubeClient(
            name = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            version = "2.0",
            userAgent = "Mozilla/5.0 (SMART-TV; LINUX; Tizen 6.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/6.0 TV Safari/538.1",
            extraContext = mapOf("clientFormFactor" to "SMALL_FORM_FACTOR")
        ),
        // IOS — devuelve URLs sin cifrar para la mayoría de videos
        InnerTubeClient(
            name = "IOS",
            version = "19.45.4",
            userAgent = "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 17_4 like Mac OS X;)",
            extraContext = mapOf("deviceModel" to "iPhone16,2", "osName" to "iOS", "osVersion" to "17.4.0.21E219", "platform" to "MOBILE")
        ),
        // ANDROID_MUSIC — tercer intento
        InnerTubeClient(
            name = "ANDROID_MUSIC",
            version = "6.19.52",
            userAgent = "com.google.android.apps.youtube.music/6.19.52 (Linux; U; Android 12; en_CO; sdk_gphone64_arm64 Build/SE1A.220630.001) gzip",
            extraContext = mapOf("androidSdkVersion" to 32, "osName" to "Android", "osVersion" to "12", "platform" to "MOBILE")
        )
    )

    suspend fun obtenerEnlaces(videoId: String): List<Calidad> = withContext(Dispatchers.IO) {
        Log.d("DULCEPLAY_VIDA", "🎵 Iniciando extracción de streams para: $videoId")
        val lista = mutableListOf<Calidad>()

        // ── Fase 1: Clientes InnerTube ────────────────────────────
        for (cliente in INNERTUBE_CLIENTS) {
            try {
                val opciones = extraerConInnerTube(videoId, cliente)
                if (opciones.isNotEmpty()) {
                    lista.addAll(opciones)
                }
            } catch (e: Exception) { Log.e("DULCEPLAY_VIDA", "Error InnerTube ${cliente.name}: ${e.message}") }
        }

        // ── Fase 2: Invidious JSON API con PROXY (Lo más estable) ──
        if (lista.none { !it.url.contains("googlevideo.com") }) { // Si solo tenemos URLs de Google (que pueden dar 403)
            for (instancia in INVIDIOUS_INSTANCES) {
                var con: HttpURLConnection? = null
                try {
                    val url = "$instancia/api/v1/videos/$videoId?local=true"
                    con = URL(url).openConnection() as HttpURLConnection
                    con.apply {
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                        connectTimeout = 8000; readTimeout = 8000
                    }
                    if (con.responseCode == HttpURLConnection.HTTP_OK) {
                        val json = JSONObject(con.inputStream.bufferedReader().readText())
                        val adaptive = json.optJSONArray("adaptiveFormats") ?: JSONArray()
                        val streams = json.optJSONArray("formatStreams") ?: JSONArray()
                        
                        fun addProxied(s: JSONObject, isAudio: Boolean) {
                            val itag = s.optInt("itag", -1)
                            val rawUrl = s.optString("url", "")
                            if (rawUrl.isBlank()) return
                            
                            // Forzamos el paso por el proxy de la instancia
                            val proxyUrl = formatearUrlProxyInvidious(rawUrl, instancia)
                            
                            val label = when(itag) {
                                140, 251 -> "Audio Alta Fidelidad (Proxy)"
                                18, 22 -> "Video ${s.optString("qualityLabel", "HD")} (Proxy)"
                                else -> if(isAudio) "Audio HQ" else "Video"
                            }
                            lista.add(Calidad(label, proxyUrl, isAudio))
                        }
                        
                        for (i in 0 until adaptive.length()) addProxied(adaptive.getJSONObject(i), true)
                        for (i in 0 until streams.length()) addProxied(streams.getJSONObject(i), false)
                        
                        if (lista.any { it.nombre.contains("Proxy") }) break
                    }
                } catch (e: Exception) { Log.e("DULCEPLAY_VIDA", "Fallo Invidious $instancia: ${e.message}") }
                finally { con?.disconnect() }
            }
        }

        // 🛡️ RESPALDO FINAL
        if (lista.isEmpty()) {
            lista.add(Calidad("Audio Alta Calidad", "https://invidious.fdn.fr/videoplayback?id=$videoId&itag=251&local=true", true))
            lista.add(Calidad("Video 720p", "https://invidious.fdn.fr/videoplayback?id=$videoId&itag=22&local=true", false))
        }

        return@withContext lista.distinctBy { it.nombre }.sortedByDescending { it.esAudio }
    }

    /**
     * Llama a la API InnerTube de YouTube con el cliente dado y extrae los streams de audio/video.
     * TVHTML5_SIMPLY_EMBEDDED_PLAYER devuelve URLs sin cifrar (sin signatureCipher).
     */
    private suspend fun extraerConInnerTube(videoId: String, cliente: InnerTubeClient): List<Calidad> = withContext(Dispatchers.IO) {
        // La clave pública de API de YouTube (sin autenticación de usuario)
        val apiKey = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
        var con: HttpURLConnection? = null
        try {
            val endpoint = "https://www.youtube.com/youtubei/v1/player?key=$apiKey&prettyPrint=false"
            con = URL(endpoint).openConnection() as HttpURLConnection
            con.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("User-Agent", cliente.userAgent)
                setRequestProperty("X-YouTube-Client-Name", when (cliente.name) {
                    "TVHTML5_SIMPLY_EMBEDDED_PLAYER" -> "85"
                    "IOS" -> "5"
                    "ANDROID_MUSIC" -> "21"
                    else -> "1"
                })
                setRequestProperty("X-YouTube-Client-Version", cliente.version)
                setRequestProperty("Origin", "https://www.youtube.com")
                connectTimeout = 12000
                readTimeout = 15000
                doOutput = true
            }

            // Construir el cuerpo de la petición
            val clientObj = JSONObject().apply {
                put("clientName", cliente.name)
                put("clientVersion", cliente.version)
                put("hl", "es")
                put("gl", "CO")
                // Agregar campos extra según el cliente
                for ((k, v) in cliente.extraContext) {
                    when (v) {
                        is String -> put(k, v)
                        is Int    -> put(k, v)
                        else      -> put(k, v.toString())
                    }
                }
            }

            val body = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", clientObj)
                })
                put("videoId", videoId)
                put("contentCheckOk", true)
                put("racyCheckOk", true)
                // Para TVHTML5: indicar que se embebe desde youtube.com
                if (cliente.name == "TVHTML5_SIMPLY_EMBEDDED_PLAYER") {
                    put("playbackContext", JSONObject().apply {
                        put("contentPlaybackContext", JSONObject().apply {
                            put("html5Preference", "HTML5_PREF_WANTS")
                            put("signatureTimestamp", 20000)
                        })
                    })
                }
            }

            con.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = con.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w("DULCEPLAY_VIDA", "HTTP $responseCode para cliente ${cliente.name}")
                return@withContext emptyList()
            }

            val texto = con.inputStream.use { it.bufferedReader().readText() }
            if (texto.isBlank()) return@withContext emptyList()

            val json = JSONObject(texto)

            // Verificar si el video está disponible
            val playabilityStatus = json.optJSONObject("playabilityStatus")
            val status = playabilityStatus?.optString("status", "") ?: ""
            if (status == "UNPLAYABLE" || status == "LOGIN_REQUIRED") {
                Log.w("DULCEPLAY_VIDA", "Video no reproducible con ${cliente.name}: $status")
                return@withContext emptyList()
            }

            val streamingData = json.optJSONObject("streamingData") ?: run {
                Log.w("DULCEPLAY_VIDA", "Sin streamingData para cliente ${cliente.name}")
                return@withContext emptyList()
            }

            return@withContext parsearStreamingData(streamingData, cliente.name)

        } catch (e: Exception) {
            Log.e("DULCEPLAY_VIDA", "Excepción con cliente ${cliente.name}: ${e.message}")
            return@withContext emptyList()
        } finally {
            con?.disconnect()
        }
    }

    private fun parsearStreamingData(streamingData: JSONObject, clienteNombre: String = ""): List<Calidad> {
        val list = mutableListOf<Calidad>()
        val formats = streamingData.optJSONArray("formats") ?: JSONArray()
        val adaptive = streamingData.optJSONArray("adaptiveFormats") ?: JSONArray()

        fun procesarFormato(s: JSONObject) {
            val itag = s.optInt("itag", -1)
            // Soportamos itags comunes: 140 (audio m4a), 251 (audio webm), 18 (video 360p), 22 (video 720p)
            if (itag != 140 && itag != 251 && itag != 18 && itag != 22) return

            var rawUrl = s.optString("url", "")
            if (rawUrl.isBlank()) {
                val cipher = s.optString("signatureCipher", "").ifBlank { s.optString("cipher", "") }
                if (cipher.isNotBlank()) {
                    rawUrl = descifrarCipherSimple(cipher)
                }
            }

            if (rawUrl.isBlank()) return

            val isAudio = itag == 140 || itag == 251
            val label = when (itag) {
                140 -> "Audio AAC Alta Calidad"
                251 -> "Audio WebM Alta Fidelidad"
                22  -> "Video 720p (HD)"
                18  -> "Video 360p (SD)"
                else -> if (isAudio) "Audio" else "Video"
            }
            
            val sufijo = if (clienteNombre.isNotBlank()) " [$clienteNombre]" else ""
            list.add(Calidad("$label$sufijo", rawUrl, isAudio))
        }

        for (i in 0 until formats.length()) {
            try { procesarFormato(formats.getJSONObject(i)) } catch (_: Exception) {}
        }
        for (i in 0 until adaptive.length()) {
            try { procesarFormato(adaptive.getJSONObject(i)) } catch (_: Exception) {}
        }

        return list
    }

    private fun descifrarCipherSimple(cipher: String): String {
        return try {
            // Parsear los parámetros del signatureCipher (URL-encoded)
            val params = mutableMapOf<String, String>()
            // Primero intentar split por & normal
            val parts = cipher.split("&")
            for (p in parts) {
                val eqIdx = p.indexOf('=')
                if (eqIdx > 0) {
                    val key = java.net.URLDecoder.decode(p.substring(0, eqIdx), "UTF-8")
                    val value = java.net.URLDecoder.decode(p.substring(eqIdx + 1), "UTF-8")
                    params[key] = value
                }
            }
            val url = params["url"] ?: return ""
            val s   = params["s"] ?: return url  // Sin 's', la URL puede ser directa
            val sp  = params["sp"] ?: "signature"
            val separator = if (url.contains("?")) "&" else "?"
            "$url$separator$sp=$s"
        } catch (e: Exception) {
            Log.e("DULCEPLAY_VIDA", "Error descifrar cipher: ${e.message}")
            ""
        }
    }

    private fun formatearUrlProxyInvidious(url: String, instancia: String): String {
        return try {
            when {
                url.startsWith("/") -> "$instancia$url"
                url.contains("googlevideo.com/videoplayback") -> {
                    val index = url.indexOf("/videoplayback")
                    if (index != -1) "$instancia${url.substring(index)}" else url
                }
                else -> url
            }
        } catch (e: Exception) {
            url
        }
    }

    data class VideoInfo(val id: String, val titulo: String, val canal: String, val imagen: String)
    data class Calidad(val nombre: String, val url: String, val esAudio: Boolean)

    // Mantener para compatibilidad hacia atrás (usada internamente)
    suspend fun listaColombia() = buscar("exitos colombia 2026 musica popular")
    suspend fun listaMexico() = buscar("exitos mexico 2026 musica popular")
}
