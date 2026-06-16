package com.dulce.play.utils

import android.content.Context
import android.util.Log
import com.dulce.play.domain.model.MediaItem
import com.dulce.play.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * LocalStorage v3.9.0 — DulcePlay
 *
 * Persistencia total de listas y favoritos usando archivos JSON en almacenamiento interno.
 * NO se borran al cerrar la app. Estructura de archivos:
 *   filesDir/dulce_favorites.json      → Lista de favoritos
 *   filesDir/dulce_playlists.json      → Todas las listas de reproducción
 */
object LocalStorage {

    private const val TAG = "DULCE_STORAGE"
    private const val FAVORITES_FILE = "dulce_favorites.json"
    private const val PLAYLISTS_FILE = "dulce_playlists.json"

    // ── Modelos de datos ────────────────────────────────────────────────────────

    data class DulcePlaylist(
        val id: String,
        val name: String,
        val items: List<MediaItem>,
        val createdAt: Long = System.currentTimeMillis()
    )

    // ── Serialización de MediaItem ──────────────────────────────────────────────

    private fun MediaItem.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("artist", artist)
        put("coverUrl", coverUrl)
        put("streamUrl", streamUrl)
        put("mediaType", mediaType.name)
    }

    private fun JSONObject.toMediaItem(): MediaItem? = try {
        MediaItem(
            id = optString("id", ""),
            title = optString("title", ""),
            artist = optString("artist", ""),
            coverUrl = optString("coverUrl", ""),
            streamUrl = optString("streamUrl", ""),
            mediaType = try { MediaType.valueOf(optString("mediaType", "AUDIO")) } catch (e: Exception) { MediaType.AUDIO }
        ).takeIf { it.id.isNotBlank() && it.title.isNotBlank() }
    } catch (e: Exception) {
        Log.w(TAG, "Error parseando MediaItem: ${e.message}")
        null
    }

    // ── Favoritos ────────────────────────────────────────────────────────────────

    suspend fun loadFavorites(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, FAVORITES_FILE)
            if (!file.exists()) return@withContext emptyList()
            val json = JSONArray(file.readText())
            (0 until json.length()).mapNotNull { i ->
                json.getJSONObject(i).toMediaItem()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando favoritos: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveFavorites(context: Context, favorites: List<MediaItem>) = withContext(Dispatchers.IO) {
        try {
            val json = JSONArray()
            favorites.forEach { json.put(it.toJson()) }
            File(context.filesDir, FAVORITES_FILE).writeText(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando favoritos: ${e.message}")
        }
    }

    suspend fun addToFavorites(context: Context, item: MediaItem): List<MediaItem> {
        val current = loadFavorites(context).toMutableList()
        if (current.none { it.id == item.id }) {
            current.add(0, item)
            saveFavorites(context, current)
        }
        return current
    }

    suspend fun removeFromFavorites(context: Context, itemId: String): List<MediaItem> {
        val current = loadFavorites(context).filter { it.id != itemId }
        saveFavorites(context, current)
        return current
    }

    suspend fun isFavorite(context: Context, itemId: String): Boolean {
        return loadFavorites(context).any { it.id == itemId }
    }

    // ── Listas de Reproducción ───────────────────────────────────────────────────

    suspend fun loadPlaylists(context: Context): List<DulcePlaylist> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, PLAYLISTS_FILE)
            if (!file.exists()) return@withContext emptyList()
            val json = JSONArray(file.readText())
            (0 until json.length()).mapNotNull { i ->
                try {
                    val obj = json.getJSONObject(i)
                    val items = obj.getJSONArray("items")
                    DulcePlaylist(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        items = (0 until items.length()).mapNotNull { j ->
                            items.getJSONObject(j).toMediaItem()
                        }
                    ).takeIf { it.id.isNotBlank() && it.name.isNotBlank() }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parseando playlist: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando playlists: ${e.message}")
            emptyList()
        }
    }

    suspend fun savePlaylists(context: Context, playlists: List<DulcePlaylist>) = withContext(Dispatchers.IO) {
        try {
            val json = JSONArray()
            playlists.forEach { pl ->
                val obj = JSONObject()
                obj.put("id", pl.id)
                obj.put("name", pl.name)
                obj.put("createdAt", pl.createdAt)
                val items = JSONArray()
                pl.items.forEach { items.put(it.toJson()) }
                obj.put("items", items)
                json.put(obj)
            }
            File(context.filesDir, PLAYLISTS_FILE).writeText(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando playlists: ${e.message}")
        }
    }

    suspend fun createPlaylist(context: Context, name: String): DulcePlaylist {
        val playlists = loadPlaylists(context).toMutableList()
        val newPlaylist = DulcePlaylist(
            id = "pl_${System.currentTimeMillis()}",
            name = name.trim(),
            items = emptyList()
        )
        playlists.add(newPlaylist)
        savePlaylists(context, playlists)
        return newPlaylist
    }

    suspend fun deletePlaylist(context: Context, playlistId: String) {
        val playlists = loadPlaylists(context).filter { it.id != playlistId }
        savePlaylists(context, playlists)
    }

    suspend fun addToPlaylist(context: Context, playlistId: String, item: MediaItem): DulcePlaylist? {
        val playlists = loadPlaylists(context).toMutableList()
        val idx = playlists.indexOfFirst { it.id == playlistId }
        if (idx == -1) return null
        val pl = playlists[idx]
        if (pl.items.any { it.id == item.id }) return pl // ya está
        val updated = pl.copy(items = pl.items + item)
        playlists[idx] = updated
        savePlaylists(context, playlists)
        return updated
    }

    suspend fun removeFromPlaylist(context: Context, playlistId: String, itemId: String): DulcePlaylist? {
        val playlists = loadPlaylists(context).toMutableList()
        val idx = playlists.indexOfFirst { it.id == playlistId }
        if (idx == -1) return null
        val updated = playlists[idx].copy(items = playlists[idx].items.filter { it.id != itemId })
        playlists[idx] = updated
        savePlaylists(context, playlists)
        return updated
    }
}
