package com.dulce.play.utils

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class SearchEngine {

    private val API_KEY = "AIzaSyCwX5kK8H0sR7kD9FZJ9X7qW8rT6vYbN3m"
    private val BASE_URL = "https://www.googleapis.com/youtube/v3/search"
    private val cliente = OkHttpClient()

    // 🔍 FUNCIÓN PRINCIPAL — Busca en YouTube y devuelve lista de (titulo, url)
    fun buscar(textoUsuario: String): List<Pair<String, String>> {
        val resultados = mutableListOf<Pair<String, String>>()
        // ⚠️ NO añadir sufijo aquí — el ViewModel ya lo añade si quiere
        val consulta = textoUsuario.trim().replace(" ", "+")
        val url = "$BASE_URL?part=snippet&q=$consulta&type=video&videoDuration=medium&maxResults=15&key=$API_KEY"

        Log.d("MOTOR", "🔍 Buscando en YouTube: $textoUsuario")
        Log.d("MOTOR", "🌐 URL: $url")

        try {
            val peticion = Request.Builder().url(url).build()
            val respuesta = cliente.newCall(peticion).execute()

            Log.d("MOTOR", "📡 Código HTTP: ${respuesta.code}")

            if (respuesta.isSuccessful) {
                val cuerpo = respuesta.body?.string() ?: ""
                Log.d("MOTOR", "📦 Respuesta (primeros 300 chars): ${cuerpo.take(300)}")

                val json = JSONObject(cuerpo)

                // ✅ VERIFICAR si hay error en la respuesta de la API
                if (json.has("error")) {
                    val error = json.getJSONObject("error")
                    Log.e("MOTOR", "❌ Error de API YouTube: ${error.optString("message")}")
                    return resultados
                }

                val items = json.optJSONArray("items")
                if (items == null) {
                    Log.e("MOTOR", "❌ JSON no tiene campo 'items'")
                    return resultados
                }

                Log.d("MOTOR", "✅ Items recibidos: ${items.length()}")

                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val idObj = item.optJSONObject("id") ?: continue

                    // ⚠️ PROTECCIÓN CRÍTICA: solo procesar si es un video (tiene videoId)
                    val videoId = idObj.optString("videoId", "")
                    if (videoId.isEmpty()) {
                        Log.w("MOTOR", "⚠️ Item $i no tiene videoId, tipo: ${idObj.optString("kind")}")
                        continue
                    }

                    val snippet = item.optJSONObject("snippet") ?: continue
                    val titulo = snippet.optString("title", "Sin título")
                    val youtubeUrl = "https://www.youtube.com/watch?v=$videoId"

                    resultados.add(Pair(titulo, youtubeUrl))
                    Log.d("MOTOR", "  [$i] ✅ $titulo → $youtubeUrl")
                }
            } else {
                val errorBody = respuesta.body?.string() ?: ""
                Log.e("MOTOR", "❌ HTTP ${respuesta.code}: $errorBody")
            }
        } catch (e: Exception) {
            Log.e("MOTOR", "❌ Excepción: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }

        Log.d("MOTOR", "📋 Total resultados devueltos: ${resultados.size}")
        return resultados
    }

    // 🇨🇴 FUNCIÓN LISTA COLOMBIA
    fun listaColombia(): List<Pair<String, String>> {
        Log.d("MOTOR", "🇨🇴 Cargando éxitos Colombia...")
        return buscarDirecto("exitos+colombia+2026+oficial", 20)
    }

    // 🇲🇽 FUNCIÓN LISTA MÉXICO
    fun listaMexico(): List<Pair<String, String>> {
        Log.d("MOTOR", "🇲🇽 Cargando éxitos México...")
        return buscarDirecto("exitos+mexico+2026+oficial", 20)
    }

    // 🔧 Función interna reutilizable para consultas directas (ya codificadas)
    private fun buscarDirecto(consultaCodificada: String, maxResults: Int): List<Pair<String, String>> {
        val resultados = mutableListOf<Pair<String, String>>()
        val url = "$BASE_URL?part=snippet&q=$consultaCodificada&type=video&videoDuration=medium&maxResults=$maxResults&key=$API_KEY"

        Log.d("MOTOR", "🌐 URL directa: $url")

        try {
            val peticion = Request.Builder().url(url).build()
            val respuesta = cliente.newCall(peticion).execute()

            Log.d("MOTOR", "📡 Código HTTP: ${respuesta.code}")

            if (respuesta.isSuccessful) {
                val cuerpo = respuesta.body?.string() ?: ""
                val json = JSONObject(cuerpo)

                if (json.has("error")) {
                    Log.e("MOTOR", "❌ Error de API: ${json.getJSONObject("error").optString("message")}")
                    return resultados
                }

                val items = json.optJSONArray("items") ?: return resultados
                Log.d("MOTOR", "✅ Items recibidos: ${items.length()}")

                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val idObj = item.optJSONObject("id") ?: continue
                    val videoId = idObj.optString("videoId", "")
                    if (videoId.isEmpty()) continue

                    val titulo = item.optJSONObject("snippet")?.optString("title", "Sin título") ?: "Sin título"
                    val youtubeUrl = "https://www.youtube.com/watch?v=$videoId"
                    resultados.add(Pair(titulo, youtubeUrl))
                    Log.d("MOTOR", "  [$i] $titulo")
                }
            }
        } catch (e: Exception) {
            Log.e("MOTOR", "❌ Excepción directa: ${e.message}")
        }

        Log.d("MOTOR", "📋 Total: ${resultados.size}")
        return resultados
    }
}
