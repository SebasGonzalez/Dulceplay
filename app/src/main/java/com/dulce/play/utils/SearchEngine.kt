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
    // EXTRACCIÓN DE STREAMS — API JSON de Invidious
    // ─────────────────────────────────────────────────────────────
    // EXTRACCIÓN DE STREAMS — API JSON de YouTube (InnerTube / HTML)
    // ─────────────────────────────────────────────────────────────
    suspend fun obtenerEnlaces(videoId: String): List<Calidad> = withContext(Dispatchers.IO) {
        val lista = mutableListOf<Calidad>()
        
        // 1. Intentar con Invidious API y proxying
        for (instancia in INVIDIOUS_INSTANCES) {
            var con: HttpURLConnection? = null
            try {
                Log.d("DULCEPLAY_VIDA", "Intentando obtener stream en Invidious: $instancia para $videoId")
                val urlString = "$instancia/api/v1/videos/$videoId?local=true"
                val url = URL(urlString)
                con = url.openConnection() as HttpURLConnection
                con.apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                    connectTimeout = 8000
                    readTimeout = 8000
                }

                if (con.responseCode == HttpURLConnection.HTTP_OK) {
                    val texto = con.inputStream.use { it.bufferedReader().readText() }
                    val json = JSONObject(texto)
                    
                    val formats = json.optJSONArray("formatStreams") ?: JSONArray()
                    val adaptive = json.optJSONArray("adaptiveFormats") ?: JSONArray()
                    
                    var audio140: Calidad? = null
                    var video18: Calidad? = null

                    fun procesarStream(s: JSONObject) {
                        val itag = s.optInt("itag", -1)
                        if (itag == 140 || itag == 18) {
                            var streamUrl = s.optString("url", "")
                            if (streamUrl.isNotBlank()) {
                                streamUrl = formatearUrlProxyInvidious(streamUrl, instancia)
                                if (itag == 140) {
                                    audio140 = Calidad("Audio AAC 🎧 (Proxy)", streamUrl, true)
                                } else {
                                    video18 = Calidad("Video 360p 📹 (Proxy)", streamUrl, false)
                                }
                            }
                        }
                    }

                    for (i in 0 until formats.length()) {
                        procesarStream(formats.getJSONObject(i))
                    }
                    for (i in 0 until adaptive.length()) {
                        procesarStream(adaptive.getJSONObject(i))
                    }

                    audio140?.let { lista.add(it) }
                    video18?.let { lista.add(it) }

                    if (lista.isNotEmpty()) {
                        Log.d("DULCEPLAY_VIDA", "✅ Extracción exitosa desde Invidious: $instancia, calidades obtenidas: ${lista.size}")
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("DULCEPLAY_VIDA", "Fallo extracción en $instancia: ${e.message}")
            } finally {
                con?.disconnect()
            }
        }

        // 2. Fallback a extracción directa de YouTube si Invidious falló
        if (lista.isEmpty()) {
            Log.d("DULCEPLAY_VIDA", "⚠️ Invidious falló, activando fallback de extracción directa (ANDROID_MUSIC)")
            try {
                val opciones = extraerDirectoYouTubeConCliente("ANDROID_MUSIC", "6.19.52", videoId)
                lista.addAll(opciones)
            } catch (e: Exception) {
                Log.e("DULCEPLAY_VIDA", "Fallo fallback directo: ${e.message}")
            }
        }

        // Devolver máximo 2 enlaces, ordenados: audio primero, luego video
        val ordenadas = lista.sortedWith(compareByDescending { it.esAudio })
        val calidadesUnicas = ordenadas.distinctBy { it.url }.take(2)
        
        Log.d("DULCEPLAY_VIDA", "Retornando ${calidadesUnicas.size} calidades al reproductor.")
        for (calidad in calidadesUnicas) {
            Log.d("DULCEPLAY_VIDA", "Stream URL: ${calidad.nombre} -> ${calidad.url.take(80)}...")
        }
        return@withContext calidadesUnicas
    }

    private suspend fun extraerDirectoYouTubeConCliente(clientName: String, clientVersion: String, videoId: String): List<Calidad> = withContext(Dispatchers.IO) {
        val fallbackApiKey = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
        var con: HttpURLConnection? = null
        try {
            Log.d("DULCEPLAY_VIDA", "Extrayendo con cliente YouTubei: $clientName ($clientVersion)")
            val url = URL("https://www.youtube.com/youtubei/v1/player?key=$fallbackApiKey")
            con = url.openConnection() as HttpURLConnection
            con.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
            }

            val body = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", clientName)
                        put("clientVersion", clientVersion)
                        if (clientName == "ANDROID" || clientName == "ANDROID_MUSIC") {
                            put("androidSdkVersion", 32)
                            put("osName", "Android")
                            put("osVersion", "12")
                            put("platform", "MOBILE")
                        } else if (clientName == "IOS") {
                            put("deviceModel", "iPhone16,2")
                            put("osName", "iOS")
                            put("osVersion", "17.4")
                            put("platform", "MOBILE")
                        } else if (clientName == "WEB") {
                            put("hl", "es")
                            put("gl", "CO")
                        }
                    })
                })
                put("videoId", videoId)
            }

            con.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            if (con.responseCode == HttpURLConnection.HTTP_OK) {
                val texto = con.inputStream.use { it.bufferedReader().readText() }
                if (texto.isBlank()) {
                    return@withContext emptyList<Calidad>()
                }
                val json = JSONObject(texto)
                val streamingData = json.optJSONObject("streamingData") ?: return@withContext emptyList()
                return@withContext parsearStreamingData(streamingData)
            } else {
                Log.w("DULCEPLAY_VIDA", "HTTP ${con.responseCode} en YouTubei con cliente $clientName")
            }
        } catch (e: Exception) {
            Log.e("DULCEPLAY_VIDA", "Error al extraer con cliente $clientName: ${e.message}")
        } finally {
            con?.disconnect()
        }
        return@withContext emptyList<Calidad>()
    }

    private suspend fun extraerDePaginaHtml(videoId: String): List<Calidad> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Calidad>()
        var con: HttpURLConnection? = null
        try {
            val url = URL("https://www.youtube.com/watch?v=$videoId&bpctr=9999999999&has_verified=1&hl=en")
            con = url.openConnection() as HttpURLConnection
            con.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (con.responseCode == HttpURLConnection.HTTP_OK) {
                val html = con.inputStream.use { it.bufferedReader().readText() }
                // Buscar ytInitialPlayerResponse = { ... };
                val pattern = "ytInitialPlayerResponse\\s*=\\s*(\\{.*?\\});"
                val regex = Regex(pattern)
                val match = regex.find(html)
                var jsonString = match?.groups?.get(1)?.value
                
                if (jsonString == null) {
                    val altPattern = "ytInitialPlayerResponse\\s*=\\s*(\\{.*)"
                    val altRegex = Regex(altPattern)
                    val altMatch = altRegex.find(html)
                    val rawMatch = altMatch?.groups?.get(1)?.value
                    if (rawMatch != null) {
                        val endIdx = findJsonEnd(rawMatch)
                        if (endIdx != -1) {
                            jsonString = rawMatch.substring(0, endIdx)
                        }
                    }
                }
                
                if (jsonString != null) {
                    val json = JSONObject(jsonString)
                    val streamingData = json.optJSONObject("streamingData")
                    if (streamingData != null) {
                        list.addAll(parsearStreamingData(streamingData))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DULCEPLAY_VIDA", "Error al extraer de HTML: ${e.message}")
        } finally {
            con?.disconnect()
        }
        return@withContext list
    }

    private fun findJsonEnd(raw: String): Int {
        var braces = 0
        var inString = false
        var escaped = false
        for (i in raw.indices) {
            val c = raw[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (c == '\\') {
                escaped = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == '{') braces++
                else if (c == '}') {
                    braces--
                    if (braces <= 0) {
                        return i + 1
                    }
                }
            }
        }
        return -1
    }

    private fun parsearStreamingData(streamingData: JSONObject): List<Calidad> {
        val list = mutableListOf<Calidad>()
        val formats = streamingData.optJSONArray("formats") ?: JSONArray()
        val adaptive = streamingData.optJSONArray("adaptiveFormats") ?: JSONArray()

        var audio140: Calidad? = null
        var video18: Calidad? = null

        fun procesarFormato(s: JSONObject) {
            val itag = s.optInt("itag", -1)
            if (itag == 140 || itag == 18) {
                var rawUrl = s.optString("url", "")
                if (rawUrl.isBlank()) {
                    val cipher = s.optString("signatureCipher", "").ifBlank { s.optString("cipher", "") }
                    if (cipher.isNotBlank()) {
                        rawUrl = descifrarCipherSimple(cipher)
                    }
                }
                if (rawUrl.isNotBlank()) {
                    if (itag == 140) {
                        audio140 = Calidad("Audio AAC 🎧 (Directo)", rawUrl, true)
                    } else {
                        video18 = Calidad("Video 360p 📹 (Directo)", rawUrl, false)
                    }
                }
            }
        }

        for (i in 0 until formats.length()) {
            procesarFormato(formats.getJSONObject(i))
        }
        for (i in 0 until adaptive.length()) {
            procesarFormato(adaptive.getJSONObject(i))
        }

        audio140?.let { list.add(it) }
        video18?.let { list.add(it) }

        return list
    }

    private fun descifrarCipherSimple(cipher: String): String {
        try {
            val params = cipher.split("&")
            var url = ""
            var s = ""
            var sp = "sig"
            for (p in params) {
                val parts = p.split("=")
                if (parts.size == 2) {
                    val key = java.net.URLDecoder.decode(parts[0], "UTF-8")
                    val valStr = java.net.URLDecoder.decode(parts[1], "UTF-8")
                    when (key) {
                        "url" -> url = valStr
                        "s" -> s = valStr
                        "sp" -> sp = valStr
                    }
                }
            }
            if (url.isNotBlank() && s.isNotBlank()) {
                val separator = if (url.contains("?")) "&" else "?"
                return "$url$separator$sp=$s"
            }
            return url
        } catch (e: Exception) {
            return ""
        }
    }

    private fun formatearUrlProxyInvidious(url: String, instancia: String): String {
        return try {
            if (url.startsWith("/")) {
                // Es una URL relativa, concatenar directamente con la instancia
                "$instancia$url"
            } else if (url.contains("googlevideo.com/videoplayback")) {
                // Es una URL absoluta de Google Video. Reemplazar el host por la instancia para forzar proxy
                val index = url.indexOf("/videoplayback")
                if (index != -1) {
                    val pathAndQuery = url.substring(index)
                    "$instancia$pathAndQuery"
                } else {
                    url
                }
            } else {
                url
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
