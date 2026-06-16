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

    // Instancias de Invidious ordenadas por confiabilidad
    private val INVIDIOUS_INSTANCES = listOf(
        "https://invidious.fdn.fr",
        "https://inv.nadeko.net",
        "https://invidious.privacydev.net",
        "https://invidious.lunar.icu",
        "https://inv.riverside.rocks",
        "https://invidious.nerdvpn.de"
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
    suspend fun obtenerEnlaces(videoId: String): List<Calidad> = withContext(Dispatchers.IO) {
        val lista = mutableListOf<Calidad>()

        for (instancia in INVIDIOUS_INSTANCES) {
            // 1. Intentamos con local=true para forzar proxy de streams
            val opcionesConProxy = intentarExtraerEnlaces(instancia, videoId, proxy = true)
            if (opcionesConProxy.isNotEmpty()) {
                lista.addAll(opcionesConProxy)
                Log.d("DULCEPLAY_VIDA", "✅ Extraídas y validadas con éxito ${lista.size} opciones de $instancia (con proxy)")
                break
            }
            
            // 2. Si no obtuvo nada, intentamos sin local=true (enlace directo)
            val opcionesSinProxy = intentarExtraerEnlaces(instancia, videoId, proxy = false)
            if (opcionesSinProxy.isNotEmpty()) {
                lista.addAll(opcionesSinProxy)
                Log.d("DULCEPLAY_VIDA", "✅ Extraídas y validadas con éxito ${lista.size} opciones de $instancia (directas)")
                break
            }
        }

        // --- MÉTODO DE RESPALDO DIRECTO DE YOUTUBE ---
        if (lista.isEmpty()) {
            Log.w("DULCEPLAY_VIDA", "⚠️ Todas las instancias de Invidious fallaron. Activando plan B: Extracción directa de YouTube...")
            val opcionesDirectas = extraerDirectoYouTube(videoId)
            if (opcionesDirectas.isNotEmpty()) {
                lista.addAll(opcionesDirectas)
            }
        }

        if (lista.isEmpty()) {
            Log.e("DULCEPLAY_VIDA", "❌ Todas las instancias y el respaldo directo fallaron para $videoId")
        }

        return@withContext lista
    }

    private suspend fun intentarExtraerEnlaces(instancia: String, videoId: String, proxy: Boolean): List<Calidad> = withContext(Dispatchers.IO) {
        val streamsTemporales = mutableListOf<Calidad>()
        var con: HttpURLConnection? = null
        try {
            val queryParams = if (proxy) "local=true&fields=adaptiveFormats,formatStreams" else "fields=adaptiveFormats,formatStreams"
            val apiUrl = "$instancia/api/v1/videos/$videoId?$queryParams"
            Log.d("DULCEPLAY_VIDA", "🔗 Solicitando: $apiUrl")
            
            con = URL(apiUrl).openConnection() as HttpURLConnection
            con.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                connectTimeout = 6000
                readTimeout = 6000
            }

            if (con.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext emptyList<Calidad>()
            }

            val texto = con.inputStream.use { it.bufferedReader().readText() }
            val json = JSONObject(texto)

            // ── 1. formatStreams: videos con audio+video combinados ──
            val formatStreams = json.optJSONArray("formatStreams") ?: JSONArray()
            for (i in 0 until formatStreams.length()) {
                val s = formatStreams.getJSONObject(i)
                val itag = s.optInt("itag", -1)
                val rawUrl = s.optString("url", "")
                if (rawUrl.isBlank()) continue
                
                val url = if (rawUrl.startsWith("http")) rawUrl else {
                    val base = instancia.removeSuffix("/")
                    if (rawUrl.startsWith("/")) "$base$rawUrl" else "$base/$rawUrl"
                }
                
                when (itag) {
                    22 -> streamsTemporales.add(Calidad("Video 720p HD 📹", url, false))
                    18 -> streamsTemporales.add(Calidad("Video 360p 📹", url, false))
                }
            }

            // ── 2. adaptiveFormats: audio separado de alta calidad ──
            val adaptive = json.optJSONArray("adaptiveFormats") ?: JSONArray()
            var audioAgregado = false

            for (i in 0 until adaptive.length()) {
                val s = adaptive.getJSONObject(i)
                val itag = s.optInt("itag", -1)
                val rawUrl = s.optString("url", "")
                if (rawUrl.isBlank()) continue
                
                val url = if (rawUrl.startsWith("http")) rawUrl else {
                    val base = instancia.removeSuffix("/")
                    if (rawUrl.startsWith("/")) "$base$rawUrl" else "$base/$rawUrl"
                }
                
                if (itag == 251 || itag == 140) {
                    streamsTemporales.add(0, Calidad("Audio Alta Calidad 🎧", url, true))
                    audioAgregado = true
                    break
                }
            }

            if (!audioAgregado) {
                for (i in 0 until adaptive.length()) {
                    val s = adaptive.getJSONObject(i)
                    val rawUrl = s.optString("url", "")
                    if (rawUrl.isBlank()) continue
                    
                    val url = if (rawUrl.startsWith("http")) rawUrl else {
                        val base = instancia.removeSuffix("/")
                        if (rawUrl.startsWith("/")) "$base$rawUrl" else "$base/$rawUrl"
                    }
                    
                    val mimeType = s.optString("type", "").ifBlank { s.optString("mimeType", "") }
                    if (mimeType.contains("audio")) {
                        streamsTemporales.add(0, Calidad("Audio Alta Calidad 🎧", url, true))
                        break
                    }
                }
            }

            // Validar que las opciones sean realmente accesibles y no devuelvan error (como 403)
            if (streamsTemporales.isNotEmpty()) {
                return@withContext coroutineScope {
                    val validados = streamsTemporales.map { calidad ->
                        async {
                            if (esUrlValida(calidad.url)) calidad else null
                        }
                    }.awaitAll().filterNotNull()
                    validados
                }
            }

        } catch (e: Exception) {
            Log.e("DULCEPLAY_VIDA", "Error al extraer de $instancia (proxy=$proxy): ${e.message}")
        } finally {
            con?.disconnect()
        }
        return@withContext emptyList<Calidad>()
    }

    private suspend fun esUrlValida(urlStream: String): Boolean = withContext(Dispatchers.IO) {
        var con: HttpURLConnection? = null
        try {
            Log.d("DULCEPLAY_VIDA", "Probandola de forma rapida: $urlStream")
            val url = URL(urlStream)
            con = url.openConnection() as HttpURLConnection
            con.apply {
                requestMethod = "HEAD"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                connectTimeout = 3000
                readTimeout = 3000
            }
            val code = con.responseCode
            Log.d("DULCEPLAY_VIDA", "Respuesta HEAD de URL: $code")
            // Permitir códigos 2xx (éxito) y 3xx (redirecciones como 301, 302, 303, 307, 308)
            return@withContext (code in 200..399)
        } catch (e: Exception) {
            Log.w("DULCEPLAY_VIDA", "Error probando URL con HEAD: ${e.message}. Probando GET corto...")
            var conGet: HttpURLConnection? = null
            try {
                conGet = URL(urlStream).openConnection() as HttpURLConnection
                conGet.apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    setRequestProperty("Range", "bytes=0-1023")
                    connectTimeout = 2500
                    readTimeout = 2500
                }
                val codeGet = conGet.responseCode
                Log.d("DULCEPLAY_VIDA", "Respuesta GET corto de URL: $codeGet")
                return@withContext (codeGet in 200..399)
            } catch (e2: Exception) {
                Log.w("DULCEPLAY_VIDA", "GET corto también falló: ${e2.message}")
                return@withContext false
            } finally {
                conGet?.disconnect()
            }
        } finally {
            con?.disconnect()
        }
    }

    private suspend fun extraerDirectoYouTube(videoId: String): List<Calidad> = withContext(Dispatchers.IO) {
        val lista = mutableListOf<Calidad>()
        val clientes = listOf(
            Pair("ANDROID", "19.08.35"),
            Pair("IOS", "19.45.4")
        )
        val directApiKey = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"

        for ((clientName, clientVersion) in clientes) {
            var con: HttpURLConnection? = null
            try {
                Log.d("DULCEPLAY_VIDA", "Fallback directo: intentando YouTubei con cliente $clientName ($clientVersion)")
                val url = URL("https://www.youtube.com/youtubei/v1/player?key=$directApiKey")
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
                            if (clientName == "ANDROID") {
                                put("androidSdkVersion", 32)
                                put("osName", "Android")
                                put("osVersion", "12")
                                put("platform", "MOBILE")
                            } else if (clientName == "IOS") {
                                put("deviceModel", "iPhone16,2")
                                put("osName", "iOS")
                                put("osVersion", "17.4")
                                put("platform", "MOBILE")
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
                        Log.w("DULCEPLAY_VIDA", "Respuesta vacía de YouTubei para el cliente $clientName")
                        continue
                    }
                    val json = JSONObject(texto)
                    val streamingData = json.optJSONObject("streamingData") ?: continue

                    val formats = streamingData.optJSONArray("formats") ?: JSONArray()
                    val adaptive = streamingData.optJSONArray("adaptiveFormats") ?: JSONArray()

                    var audio140: Calidad? = null
                    var audio251: Calidad? = null
                    var otherAudio: Calidad? = null
                    var video18: Calidad? = null
                    var video22: Calidad? = null

                    // 1. formats (audio + video combinados)
                    for (i in 0 until formats.length()) {
                        val s = formats.getJSONObject(i)
                        val itag = s.optInt("itag", -1)
                        val rawUrl = s.optString("url", "")
                        if (rawUrl.isBlank()) continue
                        when (itag) {
                            18 -> video18 = Calidad("Video 360p 📹 (Directo)", rawUrl, false)
                            22 -> video22 = Calidad("Video 720p HD 📹 (Directo)", rawUrl, false)
                        }
                    }

                    // 2. adaptiveFormats (audio y video separados)
                    for (i in 0 until adaptive.length()) {
                        val s = adaptive.getJSONObject(i)
                        val itag = s.optInt("itag", -1)
                        val rawUrl = s.optString("url", "")
                        if (rawUrl.isBlank()) continue
                        
                        when (itag) {
                            140 -> audio140 = Calidad("Audio Alta Calidad 🎧 (Directo)", rawUrl, true)
                            251 -> audio251 = Calidad("Audio Alta Calidad 🎧 (Directo)", rawUrl, true)
                            18 -> if (video18 == null) video18 = Calidad("Video 360p 📹 (Directo)", rawUrl, false)
                            22 -> if (video22 == null) video22 = Calidad("Video 720p HD 📹 (Directo)", rawUrl, false)
                            else -> {
                                val mimeType = s.optString("type", "").ifBlank { s.optString("mimeType", "") }
                                if (mimeType.contains("audio") && otherAudio == null) {
                                    otherAudio = Calidad("Audio Alta Calidad 🎧 (Directo)", rawUrl, true)
                                }
                            }
                        }
                    }

                    val streamsTemporales = mutableListOf<Calidad>()
                    
                    // Prioridades ordenadas: itag 140 (audio), 251 (alta calidad), 18 (360p), 22 (720p)
                    audio140?.let { streamsTemporales.add(it) }
                    audio251?.let { streamsTemporales.add(it) }
                    otherAudio?.let { streamsTemporales.add(it) }
                    video18?.let { streamsTemporales.add(it) }
                    video22?.let { streamsTemporales.add(it) }

                    // Validar los enlaces obtenidos
                    if (streamsTemporales.isNotEmpty()) {
                        val validados = coroutineScope {
                            streamsTemporales.map { calidad ->
                                async {
                                    if (esUrlValida(calidad.url)) calidad else null
                                }
                            }.awaitAll().filterNotNull()
                        }
                        if (validados.isNotEmpty()) {
                            lista.addAll(validados)
                            Log.d("DULCEPLAY_VIDA", "✅ Extraídos directo de YouTube y validados ${validados.size} streams con $clientName")
                            break // Salir del bucle de clientes si funciona y tiene streams válidos
                        }
                    }
                } else {
                    Log.w("DULCEPLAY_VIDA", "Respuesta HTTP ${con.responseCode} de YouTubei con cliente $clientName")
                }
            } catch (e: Exception) {
                Log.e("DULCEPLAY_VIDA", "Error al extraer directo de YouTube con $clientName: ${e.message}")
            } finally {
                con?.disconnect()
            }
        }
        return@withContext lista
    }

    data class VideoInfo(val id: String, val titulo: String, val canal: String, val imagen: String)
    data class Calidad(val nombre: String, val url: String, val esAudio: Boolean)

    // Mantener para compatibilidad hacia atrás (usada internamente)
    suspend fun listaColombia() = buscar("exitos colombia 2026 musica popular")
    suspend fun listaMexico() = buscar("exitos mexico 2026 musica popular")
}
