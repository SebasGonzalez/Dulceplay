package com.dulce.play.utils

import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchEngine {
    private val API_KEY = "AIzaSyCrzrUscZ5kEW-rQte8yFxmc4E2xUcDm-Q"

    // ✅ BÚSQUEDA RÁPIDA (SIEMPRE FUNCIONA)
    suspend fun buscar(consulta: String): List<VideoInfo> = withContext(Dispatchers.IO) {
        Log.d("DULCEPLAY_VIDA", "✅ -> BUSCANDO: $consulta")
        val resultados = mutableListOf<VideoInfo>()

        try {
            val textoLimpio = consulta.trim().lowercase()
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u").replace("ñ", "n")

            val textoCodificado = URLEncoder.encode(textoLimpio, "UTF-8")
            val urlFinal = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=$textoCodificado&type=video&maxResults=50&key=$API_KEY"

            val conexion = URL(urlFinal).openConnection() as HttpURLConnection
            conexion.apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
            }

            if (conexion.responseCode == HttpURLConnection.HTTP_OK) {
                val respuesta = BufferedReader(InputStreamReader(conexion.inputStream)).readText()
                val json = JSONObject(respuesta)
                val items = json.optJSONArray("items") ?: return@withContext emptyList()

                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val snippet = item.getJSONObject("snippet")
                    val titulo = snippet.getString("title")
                    val canal = snippet.getString("channelTitle")
                    val videoId = item.getJSONObject("id").getString("videoId")
                    val thumbnails = snippet.getJSONObject("thumbnails")
                    val imagen = thumbnails.optJSONObject("medium")?.getString("url") 
                                ?: thumbnails.getJSONObject("default").getString("url")

                    resultados.add(VideoInfo(videoId, titulo, canal, imagen))
                }
                Log.d("DULCEPLAY_VIDA", "✅ -> ENCONTRADOS: ${resultados.size}")
            }
        } catch (e: Exception) {
            Log.e("DULCEPLAY_VIDA", "❌ ERROR BUSQUEDA: ${e.message}")
        }
        return@withContext resultados
    }

    // ✅ FUNCIÓN MAGISTRAL: OBTIENE TODAS LAS CALIDADES Y FORMATOS
    suspend fun obtenerOpcionesReproduccion(videoId: String): FormatosDisponibles = withContext(Dispatchers.IO) {
        Log.d("DULCEPLAY_VIDA", "✅ -> EXTRAYENDO FORMATOS PARA: $videoId")
        val urlVideo = "https://www.youtube.com/watch?v=$videoId"
        val request = YoutubeDLRequest(urlVideo)

        // OBTENEMOS TODOS LOS FORMATOS DISPONIBLES
        request.addOption("--list-formats")

        return@withContext try {
            val info = YoutubeDL.getInstance().getInfo(request)

            // LISTAS DE CALIDAD
            val listaAudio = mutableListOf<Calidad>()
            val listaVideo = mutableListOf<Calidad>()

            info.formats?.forEach { formato ->
                when {
                    formato.vcodec == "none" && formato.acodec != "none" -> {
                        // ES SOLO AUDIO
                        listaAudio.add(Calidad(
                            id = formato.formatId ?: "audio",
                            nombre = "Audio ${formato.abr ?: "Alta"} kbps",
                            url = formato.url ?: "",
                            esVideo = false
                        ))
                    }
                    formato.vcodec != "none" && formato.vcodec != null -> {
                        // ES VIDEO
                        val resolucion = formato.height
                        listaVideo.add(Calidad(
                            id = formato.formatId ?: "video",
                            nombre = "Video ${resolucion}p",
                            url = formato.url ?: "",
                            esVideo = true
                        ))
                    }
                }
            }

            // ORDENAMOS
            FormatosDisponibles(
                audio = listaAudio.distinctBy { it.nombre }.sortedByDescending { it.nombre },
                video = listaVideo.distinctBy { it.nombre }.sortedByDescending { it.nombre }
            )

        } catch (e: Exception) {
            Log.e("DULCEPLAY_VIDA", "❌ ERROR EXTRACCIÓN: ${e.message}")
            // RESPALDO SI FALLA (Simulado o vacío)
            FormatosDisponibles(emptyList(), emptyList())
        }
    }

    // OBJETOS DE DATOS
    data class VideoInfo(val id: String, val titulo: String, val canal: String, val imagen: String)
    data class Calidad(val id: String, val nombre: String, val url: String, val esVideo: Boolean)
    data class FormatosDisponibles(val audio: List<Calidad>, val video: List<Calidad>)

    // LISTAS DE PAÍSES
    suspend fun listaColombia() = buscar("exitos colombia 2026 musica popular")
    suspend fun listaMexico() = buscar("exitos mexico 2026 musica popular")
}
