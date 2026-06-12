package com.dulce.play.utils

import android.util.Log
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SearchEngine v2 - DulcePlay
 *
 * CAMBIO CLAVE (v3.5.0):
 * - Se abandonó el scraping HTML de Invidious (no funciona: el JSON no está en el HTML renderizado).
 * - Ahora se usa la API JSON oficial de Invidious: GET /api/v1/videos/{videoId}
 *   Esta API devuelve los streams con URLs válidas y firmadas (con expire, sig, etc.)
 *   que ExoPlayer puede reproducir directamente sin ningún procesamiento adicional.
 * - Se implementa fallback automático entre múltiples instancias de Invidious públicas.
 */
class SearchEngine {
    private val API_KEY = "AIzaSyCrzrUscZ5kEW-rQte8yFxmc4E2xUcDm-Q"

    // Instancias de Invidious ordenadas por confiabilidad
    // Si una falla, automáticamente se intenta con la siguiente
    private val INVIDIOUS_INSTANCES = listOf(
        "https://inv.nadeko.net",
        "https://invidious.nerdvpn.de",
        "https://yt.artemislena.eu",
        "https://invidious.privacydev.net"
    )

    // ─────────────────────────────────────────────────────────────
    // BÚSQUEDA en YouTube (sin cambios - funciona perfecto)
    // ─────────────────────────────────────────────────────────────
    suspend fun buscar(consulta: String): List<VideoInfo> = withContext(Dispatchers.IO) {
        Log.d("DULCEPLAY", "Buscando: $consulta")
        val resultados = mutableListOf<VideoInfo>()
        try {
            val textoLimpio = consulta.trim().lowercase()
                .replace("á","a").replace("é","e").replace("í","i")
                .replace("ó","o").replace("ú","u").replace("ñ","n")
            val codificado = URLEncoder.encode(textoLimpio, "UTF-8")
            val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=$codificado&type=video&maxResults=50&key=$API_KEY"

            val con = URL(url).openConnection() as HttpURLConnection
            con.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                connectTimeout = 15000
                readTimeout = 15000
            }

            if (con.responseCode == HttpURLConnection.HTTP_OK) {
                val json = JSONObject(con.inputStream.bufferedReader().readText())
                val items = json.optJSONArray("items") ?: return@withContext emptyList()
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val snippet = item.getJSONObject("snippet")
                    val titulo = snippet.getString("title")
                    val canal = snippet.getString("channelTitle")
                    val id = item.getJSONObject("id").getString("videoId")
                    val thumbnails = snippet.getJSONObject("thumbnails")
                    val imagen = thumbnails.optJSONObject("medium")?.getString("url")
                                ?: thumbnails.getJSONObject("default").getString("url")
                    resultados.add(VideoInfo(id, titulo, canal, imagen))
                }
            }
        } catch (e: Exception) { Log.e("DULCEPLAY", "Busqueda: ${e.message}") }
        return@withContext resultados
    }

    // ─────────────────────────────────────────────────────────────
    // EXTRACCIÓN DE STREAMS — API JSON de Invidious (CORREGIDO)
    // ─────────────────────────────────────────────────────────────
    suspend fun obtenerEnlaces(videoId: String): List<Calidad> = withContext(Dispatchers.IO) {
        val lista = mutableListOf<Calidad>()

        for (instancia in INVIDIOUS_INSTANCES) {
            try {
                Log.d("DULCEPLAY_VIDA", "🔗 Intentando instancia: $instancia")
                val apiUrl = "$instancia/api/v1/videos/$videoId?fields=adaptiveFormats,formatStreams"
                val con = URL(apiUrl).openConnection() as HttpURLConnection
                con.apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                if (con.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.w("DULCEPLAY_VIDA", "⚠️ HTTP ${con.responseCode} en $instancia, probando siguiente...")
                    continue
                }

                val texto = con.inputStream.bufferedReader().readText()
                val json = JSONObject(texto)

                // ── 1. formatStreams: videos con audio+video combinados (mejor compatibilidad) ──
                val formatStreams = json.optJSONArray("formatStreams") ?: JSONArray()
                for (i in 0 until formatStreams.length()) {
                    val s = formatStreams.getJSONObject(i)
                    val itag = s.optInt("itag", -1)
                    val url = s.optString("url", "")
                    val calidad = s.optString("qualityLabel", "")
                    val mimeType = s.optString("mimeType", "")

                    if (url.isBlank()) continue

                    when (itag) {
                        22 -> lista.add(Calidad("Video 720p HD 🎬", url, false))
                        18 -> lista.add(Calidad("Video 360p ▶️", url, false))
                        else -> {
                            if (calidad.isNotBlank() && !mimeType.contains("audio")) {
                                lista.add(Calidad("Video $calidad", url, false))
                            }
                        }
                    }
                }

                // ── 2. adaptiveFormats: audio separado de alta calidad ──
                val adaptive = json.optJSONArray("adaptiveFormats") ?: JSONArray()
                val audioOpciones = mutableListOf<Calidad>()

                for (i in 0 until adaptive.length()) {
                    val s = adaptive.getJSONObject(i)
                    val itag = s.optInt("itag", -1)
                    val url = s.optString("url", "")
                    val mimeType = s.optString("mimeType", "")
                    val bitrate = s.optInt("bitrate", 0)

                    if (url.isBlank()) continue

                    // Solo nos interesan streams de audio
                    if (!mimeType.contains("audio")) continue

                    when (itag) {
                        251 -> audioOpciones.add(0, Calidad("Audio Opus 160kbps 🎵", url, true))   // Mejor calidad
                        140 -> audioOpciones.add(Calidad("Audio AAC 128kbps 🎶", url, true))
                        250 -> audioOpciones.add(Calidad("Audio Opus 70kbps 🔉", url, true))
                        249 -> audioOpciones.add(Calidad("Audio Opus 50kbps 🔈", url, true))
                        else -> {
                            if (bitrate > 0) {
                                val kbps = bitrate / 1000
                                audioOpciones.add(Calidad("Audio ${kbps}kbps", url, true))
                            }
                        }
                    }
                }
                // Añadir audios al inicio de la lista
                lista.addAll(0, audioOpciones)

                Log.d("DULCEPLAY_VIDA", "✅ Extraídas ${lista.size} opciones de $instancia")
                break // Éxito — no seguimos probando instancias

            } catch (e: Exception) {
                Log.e("DULCEPLAY_VIDA", "❌ Error en $instancia: ${e.message}")
                continue
            }
        }

        if (lista.isEmpty()) {
            Log.e("DULCEPLAY_VIDA", "❌ Todas las instancias fallaron para $videoId")
        }

        return@withContext lista
    }

    data class VideoInfo(val id: String, val titulo: String, val canal: String, val imagen: String)
    data class Calidad(val nombre: String, val url: String, val esAudio: Boolean)

    suspend fun listaColombia() = buscar("exitos colombia 2026 musica popular")
    suspend fun listaMexico() = buscar("exitos mexico 2026 musica popular")
}
