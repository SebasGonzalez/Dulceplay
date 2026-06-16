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
    // ─────────────────────────────────────────────────────────────
    // EXTRACCIÓN DE STREAMS — API JSON de YouTube (InnerTube)
    // ─────────────────────────────────────────────────────────────
    suspend fun obtenerEnlaces(videoId: String): List<Calidad> = withContext(Dispatchers.IO) {
        val candidatos = mutableListOf<Calidad>()
        val maxIntentos = 3

        for (intento in 1..maxIntentos) {
            Log.d("DULCEPLAY_VIDA", "🔄 Intento $intento de extracción directa para $videoId")
            
            // 1. Cliente WEB
            try {
                val opcionesWeb = extraerDirectoYouTubeConCliente("WEB", "2.20260615.01.00", videoId)
                if (opcionesWeb.isNotEmpty()) {
                    candidatos.addAll(opcionesWeb)
                }
            } catch (e: Exception) {
                Log.e("DULCEPLAY_VIDA", "Fallo cliente WEB: ${e.message}")
            }

            // 2. Cliente ANDROID
            try {
                val opcionesAndroid = extraerDirectoYouTubeConCliente("ANDROID", "19.08.35", videoId)
                if (opcionesAndroid.isNotEmpty()) {
                    candidatos.addAll(opcionesAndroid)
                }
            } catch (e: Exception) {
                Log.e("DULCEPLAY_VIDA", "Fallo cliente ANDROID: ${e.message}")
            }

            // 3. Cliente IOS
            try {
                val opcionesIos = extraerDirectoYouTubeConCliente("IOS", "19.45.4", videoId)
                if (opcionesIos.isNotEmpty()) {
                    candidatos.addAll(opcionesIos)
                }
            } catch (e: Exception) {
                Log.e("DULCEPLAY_VIDA", "Fallo cliente IOS: ${e.message}")
            }
            
            if (candidatos.isNotEmpty()) {
                break
            }
            
            // Espera corta entre reintentos si no se obtuvo ningún enlace
            if (intento < maxIntentos) {
                kotlinx.coroutines.delay(500)
            }
        }

        if (candidatos.isEmpty()) {
            Log.e("DULCEPLAY_VIDA", "❌ Todos los métodos de extracción fallaron para $videoId tras $maxIntentos intentos")
            return@withContext emptyList()
        }

        // Validación global y ordenamiento de candidatos (removiendo duplicados de URL)
        val candidatosUnicos = candidatos.distinctBy { it.url }
        
        Log.d("DULCEPLAY_VIDA", "Validando ${candidatosUnicos.size} candidatos únicos...")
        val validas = mutableListOf<Calidad>()
        val noValidas = mutableListOf<Calidad>()
        
        coroutineScope {
            val resultadosValidacion = candidatosUnicos.map { calidad ->
                async {
                    Pair(calidad, esUrlValida(calidad.url))
                }
            }.awaitAll()

            for ((calidad, valida) in resultadosValidacion) {
                if (valida) {
                    validas.add(calidad)
                } else {
                    noValidas.add(calidad)
                }
            }
        }

        Log.d("DULCEPLAY_VIDA", "Validación completada: ${validas.size} válidas, ${noValidas.size} no válidas")
        return@withContext validas + noValidas
    }

    private suspend fun extraerDirectoYouTubeConCliente(clientName: String, clientVersion: String, videoId: String): List<Calidad> = withContext(Dispatchers.IO) {
        val streamsTemporales = mutableListOf<Calidad>()
        val fallbackApiKey = "AIzaSy8Bv6O8gHxRqZbNn3mKpQrStUvWxYz123"
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
                    Log.d("DULCEPLAY_VIDA", "Extracted URL formats itag $itag: $rawUrl")
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
                    Log.d("DULCEPLAY_VIDA", "Extracted URL adaptive itag $itag: $rawUrl")
                    
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

                val candidatos = mutableListOf<Calidad>()
                audio140?.let { candidatos.add(it) }
                audio251?.let { candidatos.add(it) }
                otherAudio?.let { candidatos.add(it) }
                video18?.let { candidatos.add(it) }
                video22?.let { candidatos.add(it) }

                return@withContext candidatos
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

    private suspend fun esUrlValida(urlStream: String): Boolean = withContext(Dispatchers.IO) {
        var con: HttpURLConnection? = null
        try {
            val url = URL(urlStream)
            con = url.openConnection() as HttpURLConnection
            con.apply {
                requestMethod = "HEAD"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                connectTimeout = 2000
                readTimeout = 2000
            }
            val code = con.responseCode
            // 200..399 cubre éxitos y redirecciones válidas
            return@withContext (code in 200..399)
        } catch (e: Exception) {
            return@withContext false
        } finally {
            con?.disconnect()
        }
    }

    data class VideoInfo(val id: String, val titulo: String, val canal: String, val imagen: String)
    data class Calidad(val nombre: String, val url: String, val esAudio: Boolean)

    // Mantener para compatibilidad hacia atrás (usada internamente)
    suspend fun listaColombia() = buscar("exitos colombia 2026 musica popular")
    suspend fun listaMexico() = buscar("exitos mexico 2026 musica popular")
}
