package com.dulce.play.ui.player

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.dulce.play.MainActivity
import com.dulce.play.data.local.entity.*
import com.dulce.play.domain.model.*
import com.dulce.play.service.PlaybackService
import com.dulce.play.utils.LocalStorage
import com.dulce.play.utils.SearchEngine
import com.dulce.play.utils.SearchEngine.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    enum class SearchFilter { MUSICA, VIDEO_IPTV, TODO }
    enum class CastState { DISCONNECTED, SEARCHING, CONNECTED }
    enum class VisualTheme { CYBER_NEON, CLASSIC_DARK, ELECTRIC_BLUE, NATURE_GREEN }
    enum class RepeatMode { NONE, ALL, ONE }

    private val _searchFilter = MutableStateFlow(SearchFilter.TODO)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()
    fun setSearchFilter(f: SearchFilter) { _searchFilter.value = f; if (_searchQuery.value.isNotBlank()) buscarEnYouTube(_searchQuery.value) }

    private val _currentAccount = MutableStateFlow<UserAccountEntity?>(null)
    val currentAccount: StateFlow<UserAccountEntity?> = _currentAccount.asStateFlow()

    var profiles by mutableStateOf<List<UserProfile>>(emptyList())

    @OptIn(androidx.media3.common.util.UnstableApi::class)
    val exoPlayer: ExoPlayer = run {
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("com.google.android.youtube.music/6.19.52 (Linux; Android 14)")
            .setDefaultRequestProperties(mapOf(
                "Referer" to "https://music.youtube.com/"
            ))
            .setConnectTimeoutMs(12000)
            .setReadTimeoutMs(12000)
            .setAllowCrossProtocolRedirects(true)
        
        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(application, httpDataSourceFactory)
        
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(application)
            .setDataSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(application)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF 
                playWhenReady = true
            }
    }
    private var mediaSession: MediaSession? = null

    private val _currentProfile = MutableStateFlow(UserProfile("p1", "Usuario", "avatar_0", true))
    val currentProfile: StateFlow<UserProfile> = _currentProfile.asStateFlow()

    private val _selectedCountry = MutableStateFlow("Global")
    val selectedCountry: StateFlow<String> = _selectedCountry.asStateFlow()

    private val _currentMedia = MutableStateFlow(com.dulce.play.domain.model.MediaItem(title = "Sintonizando DulcePlay", streamUrl = ""))
    val currentMedia: StateFlow<com.dulce.play.domain.model.MediaItem> = _currentMedia.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f) 
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _currentTimeSeconds = MutableStateFlow(0)
    val currentTimeSeconds: StateFlow<Int> = _currentTimeSeconds.asStateFlow()

    private val _topColombia = MutableStateFlow<List<com.dulce.play.domain.model.MediaItem>>(emptyList())
    val topColombia: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _topColombia.asStateFlow()
    private val _topMexico = MutableStateFlow<List<com.dulce.play.domain.model.MediaItem>>(emptyList())
    val topMexico: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _topMexico.asStateFlow()
    private val _topGlobal = MutableStateFlow<List<com.dulce.play.domain.model.MediaItem>>(emptyList())
    val topGlobal: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _topGlobal.asStateFlow()

    // ── Secciones de Exploración por Género (v3.8.0) ──────────────────────────
    private val _top50YouTube = MutableStateFlow<List<com.dulce.play.domain.model.MediaItem>>(emptyList())
    val top50YouTube: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _top50YouTube.asStateFlow()
    private val _tendenciasColombia = MutableStateFlow<List<com.dulce.play.domain.model.MediaItem>>(emptyList())
    val tendenciasColombia: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _tendenciasColombia.asStateFlow()
    private val _loMasEscuchado = MutableStateFlow<List<com.dulce.play.domain.model.MediaItem>>(emptyList())
    val loMasEscuchado: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _loMasEscuchado.asStateFlow()
    private val _seccionVallenato = MutableStateFlow<List<com.dulce.play.domain.model.MediaItem>>(emptyList())
    val seccionVallenato: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _seccionVallenato.asStateFlow()
    private val _seccionSalsa = MutableStateFlow<List<com.dulce.play.domain.model.MediaItem>>(emptyList())
    val seccionSalsa: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _seccionSalsa.asStateFlow()
    private val _seccionUrbano = MutableStateFlow<List<com.dulce.play.domain.model.MediaItem>>(emptyList())
    val seccionUrbano: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _seccionUrbano.asStateFlow()
    private val _seccionPopular = MutableStateFlow<List<com.dulce.play.domain.model.MediaItem>>(emptyList())
    val seccionPopular: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _seccionPopular.asStateFlow()

    // ── Estado de carga por sección ───────────────────────────────────────────
    private val _sectionLoadingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val sectionLoadingStates: StateFlow<Map<String, Boolean>> = _sectionLoadingStates.asStateFlow()
    private val _sectionErrorStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val sectionErrorStates: StateFlow<Map<String, Boolean>> = _sectionErrorStates.asStateFlow()

    private val _favorites = MutableStateFlow<List<com.dulce.play.domain.model.MediaItem>>(emptyList())
    val favorites: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _favorites.asStateFlow()

    private val _recentPlayed = MutableStateFlow<List<com.dulce.play.domain.model.MediaItem>>(emptyList())
    val recentPlayed: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _recentPlayed.asStateFlow()

    private val _radioStations = MutableStateFlow<List<IPTVChannel>>(emptyList())
    val radioStations: StateFlow<List<IPTVChannel>> = _radioStations.asStateFlow()

    private val _accentColor = MutableStateFlow(Color.Cyan)
    val accentColor: StateFlow<Color> = _accentColor.asStateFlow()

    private val _voiceType = MutableStateFlow("female") 
    val voiceType: StateFlow<String> = _voiceType.asStateFlow()

    private val _appBrightness = MutableStateFlow(1.0f)
    val appBrightness: StateFlow<Float> = _appBrightness.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    val cargando: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchOverlayActive = MutableStateFlow(false)
    val searchOverlayActive: StateFlow<Boolean> = _searchOverlayActive.asStateFlow()

    private val _onlineSearchResults = MutableStateFlow<List<com.dulce.play.domain.model.MediaItem>>(emptyList())
    val onlineSearchResults: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _onlineSearchResults.asStateFlow()
    val resultadosBusqueda: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _onlineSearchResults.asStateFlow()

    private val _listaCalidades = MutableStateFlow<List<Calidad>>(emptyList())
    val listaCalidades: StateFlow<List<Calidad>> = _listaCalidades.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    val repeatEnabled: StateFlow<Boolean> = _repeatMode.map { it != RepeatMode.NONE }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _playQueue = MutableStateFlow<List<com.dulce.play.domain.model.MediaItem>>(emptyList())
    val playQueue: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _playQueue.asStateFlow()
    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _persistedFavorites = MutableStateFlow<List<com.dulce.play.domain.model.MediaItem>>(emptyList())
    val persistedFavorites: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _persistedFavorites.asStateFlow()
    private val _persistedPlaylists = MutableStateFlow<List<LocalStorage.DulcePlaylist>>(emptyList())
    val persistedPlaylists: StateFlow<List<LocalStorage.DulcePlaylist>> = _persistedPlaylists.asStateFlow()

    private val _totalDurationSeconds = MutableStateFlow(0)
    val totalDurationSeconds: StateFlow<Int> = _totalDurationSeconds.asStateFlow()

    private val _visualizerBars = MutableStateFlow(List(32) { 0.1f })
    val visualizerBars: StateFlow<List<Float>> = _visualizerBars.asStateFlow()

    private val _mediaError = MutableStateFlow<String?>(null)
    val mediaError: StateFlow<String?> = _mediaError.asStateFlow()

    private val _activeTheme = MutableStateFlow(VisualTheme.CYBER_NEON)
    val activeTheme: StateFlow<VisualTheme> = _activeTheme.asStateFlow()

    private val _castState = MutableStateFlow(CastState.DISCONNECTED)
    val castState: StateFlow<CastState> = _castState.asStateFlow()

    private val _castDevice = MutableStateFlow<String?>(null)
    val castDevice: StateFlow<String?> = _castDevice.asStateFlow()

    private val _availableCastDevices = MutableStateFlow<List<String>>(emptyList())
    val availableCastDevices: StateFlow<List<String>> = _availableCastDevices.asStateFlow()

    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()

    private val _localMediaList = MutableStateFlow<List<com.dulce.play.domain.model.MediaItem>>(emptyList())
    val localMediaList: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _localMediaList.asStateFlow()
    val listaLocal: StateFlow<List<com.dulce.play.domain.model.MediaItem>> = _localMediaList.asStateFlow() 

    private val _particles = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val particles: StateFlow<List<Pair<Float, Float>>> = _particles.asStateFlow()

    private val _isEasyMode = MutableStateFlow(false)
    val isEasyMode: StateFlow<Boolean> = _isEasyMode.asStateFlow()

    private val _isDrivingMode = MutableStateFlow(false)
    val isDrivingMode: StateFlow<Boolean> = _isDrivingMode.asStateFlow()

    private val _isFamilyMode = MutableStateFlow(false)
    val isFamilyMode: StateFlow<Boolean> = _isFamilyMode.asStateFlow()

    private val _isWellnessMode = MutableStateFlow(false)
    val isWellnessMode: StateFlow<Boolean> = _isWellnessMode.asStateFlow()

    private val _sleepTimerRemainingSeconds = MutableStateFlow(0)
    val sleepTimerRemainingSeconds: StateFlow<Int> = _sleepTimerRemainingSeconds.asStateFlow()

    private val _eqEnabled = MutableStateFlow(true)
    val eqEnabled: StateFlow<Boolean> = _eqEnabled.asStateFlow()

    private val _eqPreset = MutableStateFlow("Personalizado")
    val eqPreset: StateFlow<String> = _eqPreset.asStateFlow()

    private val _eqBands = MutableStateFlow(listOf(50, 50, 50, 50, 50))
    val eqBands: StateFlow<List<Int>> = _eqBands.asStateFlow()

    private val _assistantVoiceEnabled = MutableStateFlow(true)
    val assistantVoiceEnabled: StateFlow<Boolean> = _assistantVoiceEnabled.asStateFlow()

    private val _assistantAutoLearn = MutableStateFlow(true)
    val assistantAutoLearn: StateFlow<Boolean> = _assistantAutoLearn.asStateFlow()

    private val _profileLocks = MutableStateFlow<Map<String, String>>(emptyMap())
    val profileLocks: StateFlow<Map<String, String>> = _profileLocks.asStateFlow()

    private val _userPlaylists = MutableStateFlow<List<UserPlaylistEntity>>(emptyList())
    val userPlaylists: StateFlow<List<UserPlaylistEntity>> = _userPlaylists.asStateFlow()

    private val _userPlaylistItems = MutableStateFlow<Map<String, List<UserPlaylistItemEntity>>>(emptyMap())
    val userPlaylistItems: StateFlow<Map<String, List<UserPlaylistItemEntity>>> = _userPlaylistItems.asStateFlow()

    private val _playbackHistory = MutableStateFlow<List<PlaybackHistoryEntity>>(emptyList())
    val playbackHistory: StateFlow<List<PlaybackHistoryEntity>> = _playbackHistory.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(listOf("Cumbia", "Vallenato", "Salsa"))
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _searchSuggestion = MutableStateFlow<String?>(null)
    val searchSuggestion: StateFlow<String?> = _searchSuggestion.asStateFlow()

    private val _customIPTVPlaylists = MutableStateFlow<List<Pair<String, List<IPTVChannel>>>>(emptyList())
    val customIPTVPlaylists: StateFlow<List<Pair<String, List<IPTVChannel>>>> = _customIPTVPlaylists.asStateFlow()

    private val _activeIPTVChannel = MutableStateFlow<IPTVChannel?>(null)
    val activeIPTVChannel: StateFlow<IPTVChannel?> = _activeIPTVChannel.asStateFlow()

    private var progressJob: Job? = null
    private var searchJob: Job? = null
    private var visualizerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private val db = androidx.room.Room.databaseBuilder(application, AppDatabase::class.java, "dulce_database").fallbackToDestructiveMigration().build()

    private val motor = SearchEngine()
    private var intentandoIndiceCalidad = 0
    private var reintentosCalidadActual = 0

    init {
        val pi = PendingIntent.getActivity(application, 0, Intent(application, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        mediaSession = MediaSession.Builder(application, exoPlayer).setSessionActivity(pi).build()
        PlaybackService.activeSession = mediaSession
        application.startService(Intent(application, PlaybackService::class.java))
        initializeAccountAndProfiles(); fetchTopCharts(); fetchRadioStations(); startVisualizerLoop(); generateParticles()
        loadPersistedData()
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startProgressPolling() else progressJob?.cancel()
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    when (_repeatMode.value) {
                        RepeatMode.ONE -> { exoPlayer.seekTo(0); exoPlayer.play() }
                        else -> next()
                    }
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("DULCEPLAY_VIDA", "❌ ERROR EXOPLAYER: ${error.message} (código: ${error.errorCode})")
                val opciones = _listaCalidades.value
                if (opciones.isNotEmpty()) {
                    if (reintentosCalidadActual < 1) {
                        reintentosCalidadActual++
                        Log.w("DULCEPLAY_VIDA", "ExoPlayer falló. Reintentando calidad actual index $intentandoIndiceCalidad (intento $reintentosCalidadActual/1)...")
                        reproducirCalidadActual()
                    } else if (intentandoIndiceCalidad + 1 < opciones.size) {
                        intentandoIndiceCalidad++
                        reintentosCalidadActual = 0
                        Log.w("DULCEPLAY_VIDA", "ExoPlayer falló tras reintentar. Intentando siguiente calidad: index $intentandoIndiceCalidad")
                        reproducirCalidadActual()
                    } else {
                        _mediaError.value = "No se pudo reproducir este contenido en este momento"
                    }
                } else {
                    _mediaError.value = "No se pudo reproducir este contenido en este momento"
                }
            }
        })
    }

    private fun loadPersistedData() {
        viewModelScope.launch {
            try {
                _persistedFavorites.value = LocalStorage.loadFavorites(getApplication())
                _persistedPlaylists.value = LocalStorage.loadPlaylists(getApplication())
                Log.d("DULCEPLAY_VM", "✅ Datos persistidos: ${_persistedFavorites.value.size} fav, ${_persistedPlaylists.value.size} playlists")
            } catch (e: Exception) { Log.e("DULCEPLAY_VM", "Error cargando datos: ${e.message}") }
        }
    }

    fun buscarEnYouTube(texto: String) {
        if (texto.isBlank()) { _onlineSearchResults.value = emptyList(); return }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _onlineSearchResults.value = emptyList()
            try {
                val datosRecibidos = motor.buscar(texto)
                val items = datosRecibidos.mapNotNull { info ->
                    if (info.id.isBlank() || info.titulo.isBlank()) null
                    else com.dulce.play.domain.model.MediaItem(
                        id = info.id, title = info.titulo,
                        artist = info.canal.ifBlank { "Desconocido" },
                        coverUrl = info.imagen, streamUrl = info.id
                    )
                }
                _onlineSearchResults.value = items
                // Cola automática: todos los resultados se agregan a la cola
                if (items.isNotEmpty()) _playQueue.value = items
            } catch (e: Exception) {
                Log.e("DULCEPLAY_VIDA", "❌ ERROR VM: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun cargarOpcionesParaReproducir(videoId: String, autoPlay: Boolean = true) {
        viewModelScope.launch {
            _isSearching.value = true
            _listaCalidades.value = emptyList() // Limpiar opciones anteriores mientras carga
            _mediaError.value = null
            reintentosCalidadActual = 0
            Log.d("DULCEPLAY_VIDA", "🔍 Extrayendo streams para: $videoId")
            val opciones = motor.obtenerEnlaces(videoId)
            _listaCalidades.value = opciones
            _isSearching.value = false
            if (opciones.isEmpty()) {
                Log.e("DULCEPLAY_VIDA", "❌ Sin streams disponibles para $videoId")
                _mediaError.value = "No se pudo reproducir este contenido en este momento"
            } else {
                Log.d("DULCEPLAY_VIDA", "✅ ${opciones.size} opciones cargadas para $videoId")
                if (autoPlay) {
                    intentandoIndiceCalidad = 0
                    reproducirCalidadActual()
                }
            }
        }
    }

    fun reproducirCalidadActual() {
        val opciones = _listaCalidades.value
        if (opciones.isEmpty() || intentandoIndiceCalidad >= opciones.size) {
            _mediaError.value = "No se pudo reproducir este contenido en este momento"
            return
        }
        val calidad = opciones[intentandoIndiceCalidad]
        Log.d("DULCEPLAY_VIDA", "Intentando reproducir calidad index $intentandoIndiceCalidad (${calidad.nombre}): ${calidad.url.take(120)}...")
        reproducirSeleccionado(calidad.url)
    }

    fun reproducirSeleccionado(urlEnlace: String) {
        if (urlEnlace.isBlank()) return
        viewModelScope.launch {
            try {
                Log.d("DULCEPLAY_VIDA", "▶️ Reproduciendo: ${urlEnlace.take(80)}...")
                _mediaError.value = null
                
                // Construir MediaItem con metadatos para la notificación
                val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(_currentMedia.value.title)
                    .setArtist(_currentMedia.value.artist)
                    .setArtworkUri(android.net.Uri.parse(_currentMedia.value.coverUrl))
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(android.net.Uri.parse(urlEnlace))
                    .setMediaMetadata(mediaMetadata)
                    .build()

                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()

                // Iniciar/actualizar servicio en primer plano
                val intent = Intent(getApplication(), PlaybackService::class.java)
                getApplication<Application>().startService(intent)
            } catch (e: Exception) {
                Log.e("DULCEPLAY_VIDA", "❌ ERROR AL REPRODUCIR: ${e.message}")
                _mediaError.value = "Error al iniciar reproducción: ${e.message}"
            }
        }
    }

    fun updateSearchQuery(q: String) { _searchQuery.value = q; buscarEnYouTube(q) }
    fun buscarAhora(q: String) { buscarEnYouTube(q) }
    fun executeSearch(q: String) { buscarEnYouTube(q) }
    
    fun playMedia(m: com.dulce.play.domain.model.MediaItem, autoPlay: Boolean = true) {
        if (m.streamUrl.isBlank()) return
        _currentMedia.value = m
        val qIdx = _playQueue.value.indexOfFirst { it.id == m.id }
        if (qIdx >= 0) _currentQueueIndex.value = qIdx
        if (!m.streamUrl.startsWith("http")) {
            cargarOpcionesParaReproducir(m.streamUrl, autoPlay)
        } else {
            intentandoIndiceCalidad = 0
            reintentosCalidadActual = 0
            _listaCalidades.value = emptyList()
            reproducirSeleccionado(m.streamUrl)
        }
    }

    fun playPlaylist(playlist: LocalStorage.DulcePlaylist, startIndex: Int = 0) {
        if (playlist.items.isEmpty()) return
        _playQueue.value = playlist.items
        _currentQueueIndex.value = startIndex
        playMedia(playlist.items[startIndex], autoPlay = true)
    }

    fun addToQueue(item: com.dulce.play.domain.model.MediaItem) {
        val q = _playQueue.value.toMutableList()
        if (q.none { it.id == item.id }) {
            val insertAt = (_currentQueueIndex.value + 1).coerceAtMost(q.size)
            q.add(insertAt, item)
            _playQueue.value = q
        }
    }

    fun removeFromQueue(itemId: String) {
        val q = _playQueue.value.toMutableList()
        val idx = q.indexOfFirst { it.id == itemId }
        if (idx < 0) return
        q.removeAt(idx)
        _playQueue.value = q
        if (idx < _currentQueueIndex.value) _currentQueueIndex.value = (_currentQueueIndex.value - 1).coerceAtLeast(0)
    }
    
    fun playIPTVChannel(c: IPTVChannel) { playMedia(com.dulce.play.domain.model.MediaItem(title = c.name, streamUrl = c.streamUrl, mediaType = MediaType.IPTV)) }
    fun togglePlay() { if (exoPlayer.isPlaying) exoPlayer.pause() else { if (exoPlayer.playbackState == Player.STATE_IDLE) exoPlayer.prepare(); exoPlayer.play() } }
    
    fun next() {
        val queue = _playQueue.value
        if (queue.isEmpty()) {
            val list = getFilteredMediaList(); if (list.isEmpty()) return
            val idx = list.indexOfFirst { it.id == _currentMedia.value.id }
            playMedia(if (idx >= 0) list[(idx + 1) % list.size] else list.first(), autoPlay = true); return
        }
        val nextIdx = if (_shuffleEnabled.value) {
            var rand = Random.nextInt(queue.size)
            if (queue.size > 1) while (rand == _currentQueueIndex.value) rand = Random.nextInt(queue.size)
            rand
        } else {
            val candidate = _currentQueueIndex.value + 1
            when {
                candidate < queue.size -> candidate
                _repeatMode.value == RepeatMode.ALL -> 0
                else -> return
            }
        }
        _currentQueueIndex.value = nextIdx
        playMedia(queue[nextIdx], autoPlay = true)
    }

    fun prev() {
        val queue = _playQueue.value
        if (queue.isEmpty()) {
            val list = getFilteredMediaList(); if (list.isEmpty()) return
            val idx = list.indexOfFirst { it.id == _currentMedia.value.id }
            playMedia(list[if (idx > 0) idx - 1 else list.size - 1], autoPlay = true); return
        }
        if (exoPlayer.currentPosition > 3000) { exoPlayer.seekTo(0); return }
        val prevIdx = if (_currentQueueIndex.value > 0) _currentQueueIndex.value - 1 else queue.size - 1
        _currentQueueIndex.value = prevIdx
        playMedia(queue[prevIdx], autoPlay = true)
    }

    fun setVolume(vol: Float) {
        exoPlayer.volume = vol.coerceIn(0f, 1f)
    }

    fun getVolume(): Float {
        return exoPlayer.volume
    }

    fun seekPercent(p: Float) { val dur = exoPlayer.duration; if (dur > 0) exoPlayer.seekTo((p * dur).toLong()) }
    fun seekToSeconds(s: Int) { val ms = s * 1000L; if (ms <= exoPlayer.duration) exoPlayer.seekTo(ms) }
    fun toggleCast() { _isCasting.value = !_isCasting.value }
    fun addLocalMedia(uri: Uri, name: String, video: Boolean) {
        val newItem = com.dulce.play.domain.model.MediaItem(id = UUID.randomUUID().toString(), title = name, streamUrl = uri.toString(), mediaType = if (video) MediaType.VIDEO else MediaType.AUDIO)
        _localMediaList.value = _localMediaList.value + newItem
        playMedia(newItem)
    }
    
    fun selectProfile(p: UserProfile) { _currentProfile.value = p }
    fun selectCountry(c: String) { _selectedCountry.value = c }
    fun toggleEqualizer() { _eqEnabled.value = !_eqEnabled.value }
    fun updateEqBand(idx: Int, value: Int) { val cur = _eqBands.value.toMutableList(); cur[idx] = value; _eqBands.value = cur }
    fun applyEqualizerPreset(preset: String) { _eqPreset.value = preset }
    fun toggleAssistantVoice() { _assistantVoiceEnabled.value = !_assistantVoiceEnabled.value }
    fun toggleAssistantAutoLearn() { _assistantAutoLearn.value = !_assistantAutoLearn.value }
    fun setProfileLockPin(pid: String, pin: String) { /* logic */ }
    fun createJSONBackup() = "{}"
    fun restoreFromBackupJSON(j: String) = true
    fun getFilteredMediaList() = if (_onlineSearchResults.value.isNotEmpty()) _onlineSearchResults.value else _localMediaList.value
    fun getAllIPTVChannels() = _radioStations.value
    fun scanLocalMedia() {}
    fun addSearchQueryToHistory(q: String) {}
    fun clearSearchHistory() {}
    fun setSearchOverlayActive(a: Boolean) { _searchOverlayActive.value = a }
    fun toggleFavorite(item: com.dulce.play.domain.model.MediaItem) {
        viewModelScope.launch {
            val isFav = _persistedFavorites.value.any { it.id == item.id }
            _persistedFavorites.value = if (isFav) LocalStorage.removeFromFavorites(getApplication(), item.id)
                                        else LocalStorage.addToFavorites(getApplication(), item)
        }
    }
    fun isFavorite(itemId: String): Boolean = _persistedFavorites.value.any { it.id == itemId }
    fun createUserPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            LocalStorage.createPlaylist(getApplication(), name)
            _persistedPlaylists.value = LocalStorage.loadPlaylists(getApplication())
        }
    }
    fun deleteUserPlaylist(id: String) {
        viewModelScope.launch {
            LocalStorage.deletePlaylist(getApplication(), id)
            _persistedPlaylists.value = LocalStorage.loadPlaylists(getApplication())
        }
    }
    fun addTrackToUserPlaylist(playlistId: String, item: com.dulce.play.domain.model.MediaItem) {
        viewModelScope.launch {
            LocalStorage.addToPlaylist(getApplication(), playlistId, item)
            _persistedPlaylists.value = LocalStorage.loadPlaylists(getApplication())
        }
    }
    fun removeTrackFromUserPlaylist(playlistId: String, itemId: String) {
        viewModelScope.launch {
            LocalStorage.removeFromPlaylist(getApplication(), playlistId, itemId)
            _persistedPlaylists.value = LocalStorage.loadPlaylists(getApplication())
        }
    }
    fun importM3UPlaylist(name: String, text: String) {}
    fun loadM3UFromUrl(name: String, urlString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {}
    fun importXtreamCodes(serverUrl: String, username: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {}
    fun selectTheme(t: VisualTheme) { _activeTheme.value = t }
    fun stopCasting() { _isCasting.value = false }
    fun startCasting(d: String) { _isCasting.value = true }
    fun toggleShuffle() { _shuffleEnabled.value = !_shuffleEnabled.value }
    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL  -> RepeatMode.ONE
            RepeatMode.ONE  -> RepeatMode.NONE
        }
    }
    fun searchCastDevices() {}
    fun toggleEasyMode() { _isEasyMode.value = !_isEasyMode.value }
    fun toggleDrivingMode() { _isDrivingMode.value = !_isDrivingMode.value }
    fun toggleFamilyMode() { _isFamilyMode.value = !_isFamilyMode.value }
    fun toggleWellnessMode() { _isWellnessMode.value = !_isWellnessMode.value }
    fun startSleepTimer(m: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingSeconds.value = m * 60
        sleepTimerJob = viewModelScope.launch {
            while (_sleepTimerRemainingSeconds.value > 0) { delay(1000); _sleepTimerRemainingSeconds.value-- }
            if (exoPlayer.isPlaying) exoPlayer.pause()
        }
    }
    fun createProfile(name: String, avatarUrl: String, favoriteGenre: String, isPremium: Boolean) {}
    fun deleteProfile(id: String) {}
    fun logout() {}
    fun login(email: String, password: String, callback: (Boolean, String) -> Unit) {}
    fun register(email: String, password: String, name: String, callback: (Boolean, String) -> Unit) {}
    fun registerOrLoginOAuth(email: String, displayName: String, provider: String, callback: (Boolean) -> Unit) {}

    private fun fetchTopCharts() {
        // Carga en paralelo todas las secciones para eficiencia máxima
        fetchSection("topColombia", "exitos colombia 2026 musica popular") { _topColombia.value = it }
        fetchSection("topMexico", "exitos mexico 2026 musica popular") { _topMexico.value = it }
        fetchSection("topGlobal", "top hits global 2026 best music") { _topGlobal.value = it }
        fetchSection("top50YouTube", "top 50 youtube musica 2026") { _top50YouTube.value = it }
        fetchSection("tendenciasColombia", "tendencias colombia musica 2026 viral") { _tendenciasColombia.value = it }
        fetchSection("loMasEscuchado", "lo mas escuchado 2026 latin hits") { _loMasEscuchado.value = it }
        fetchSection("vallenato", "vallenato exitos 2026 carlos vives binomio") { _seccionVallenato.value = it }
        fetchSection("salsa", "salsa exitos 2026 cali colombia marc anthony") { _seccionSalsa.value = it }
        fetchSection("urbano", "musica urbana reggaeton 2026 bad bunny colombia") { _seccionUrbano.value = it }
        fetchSection("popular", "musica popular colombiana 2026 grupo niche") { _seccionPopular.value = it }
    }

    private fun fetchSection(
        key: String,
        query: String,
        onResult: (List<com.dulce.play.domain.model.MediaItem>) -> Unit
    ) {
        viewModelScope.launch {
            // Marcar sección como cargando
            _sectionLoadingStates.value = _sectionLoadingStates.value + (key to true)
            _sectionErrorStates.value = _sectionErrorStates.value + (key to false)
            try {
                val res = motor.buscar(query)
                val items = res.mapNotNull { info ->
                    try {
                        // Null-safe mapping — ignorar items con datos inválidos
                        if (info.id.isBlank() || info.titulo.isBlank()) null
                        else com.dulce.play.domain.model.MediaItem(
                            id = info.id,
                            title = info.titulo,
                            artist = info.canal.ifBlank { "Artista desconocido" },
                            coverUrl = info.imagen,
                            streamUrl = info.id
                        )
                    } catch (e: Exception) {
                        Log.w("DULCEPLAY_VM", "Item inválido en sección $key: ${e.message}")
                        null
                    }
                }
                onResult(items)
                Log.d("DULCEPLAY_VM", "✅ Sección '$key' cargada: ${items.size} items")
            } catch (e: Exception) {
                Log.e("DULCEPLAY_VM", "❌ Error cargando sección '$key': ${e.message}")
                _sectionErrorStates.value = _sectionErrorStates.value + (key to true)
                onResult(emptyList())
            } finally {
                _sectionLoadingStates.value = _sectionLoadingStates.value + (key to false)
            }
        }
    }
    private fun fetchRadioStations() {}
    private fun startVisualizerLoop() { 
        visualizerJob = viewModelScope.launch { 
            while(true) { 
                _visualizerBars.value = if (_isPlaying.value) List(32) { Random.nextFloat() } else List(32) { 0.1f }
                delay(100) 
            } 
        } 
    }
    private fun generateParticles() {
        viewModelScope.launch {
            _particles.value = List(20) { Random.nextFloat() to Random.nextFloat() }
            while(true) { delay(100); _particles.value = _particles.value.map { (it.first + 0.01f) % 1f to (it.second + 0.01f) % 1f } }
        }
    }
    private fun startProgressPolling() { 
        progressJob = viewModelScope.launch { 
            while(true) { 
                if (exoPlayer.duration > 0) { 
                    _currentTimeSeconds.value = (exoPlayer.currentPosition / 1000).toInt()
                    _totalDurationSeconds.value = (exoPlayer.duration / 1000).toInt()
                    _playbackProgress.value = exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat() 
                }
                delay(500) 
            } 
        } 
    }
    fun initializeAccountAndProfiles() { 
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { 
            try {
                var session = db.userAccountDao().getActiveSession()
                if (session == null) {
                    val email = "usuario@dulceplay.com"
                    val acc = UserAccountEntity(email, "hash", "Usuario", true)
                    db.userAccountDao().insertAccount(acc); session = acc
                    db.userProfileDao().insertProfile(UserProfileEntity("p1", email, "Usuario", "avatar_0", true, "Salsa"))
                }
                _currentAccount.value = session
                val profEntities = db.userProfileDao().getProfilesForAccountDirect(session.email)
                profiles = profEntities.map { UserProfile(it.id, it.name, it.avatarUrl, it.isPremium, it.favoriteGenre) }
            } catch (e: Exception) {
                Log.e("DULCEPLAY_VM", "Error inicializando perfiles: ${e.message}")
            }
        } 
    }

    override fun onCleared() {
        super.onCleared()
        // Liberar recursos del ExoPlayer y MediaSession correctamente
        try {
            progressJob?.cancel()
            searchJob?.cancel()
            visualizerJob?.cancel()
            sleepTimerJob?.cancel()
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            mediaSession?.release()
            mediaSession = null
            exoPlayer.release()
            Log.d("DULCEPLAY_VM", "✅ Recursos liberados correctamente")
        } catch (e: Exception) {
            Log.w("DULCEPLAY_VM", "Error al liberar recursos: ${e.message}")
        }
    }
}
