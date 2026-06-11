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

    private val _searchFilter = MutableStateFlow(SearchFilter.TODO)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()
    fun setSearchFilter(f: SearchFilter) { _searchFilter.value = f; if (_searchQuery.value.isNotBlank()) buscarEnYouTube(_searchQuery.value) }

    private val _currentAccount = MutableStateFlow<UserAccountEntity?>(null)
    val currentAccount: StateFlow<UserAccountEntity?> = _currentAccount.asStateFlow()

    var profiles by mutableStateOf<List<UserProfile>>(emptyList())

    @OptIn(androidx.media3.common.util.UnstableApi::class)
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build().apply { 
        repeatMode = Player.REPEAT_MODE_OFF 
        playWhenReady = true
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

    private val _formatosDisponibles = MutableStateFlow<FormatosDisponibles?>(null)
    val formatosDisponibles: StateFlow<FormatosDisponibles?> = _formatosDisponibles.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatEnabled = MutableStateFlow(false)
    val repeatEnabled: StateFlow<Boolean> = _repeatEnabled.asStateFlow()

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

    init {
        val pi = PendingIntent.getActivity(application, 0, Intent(application, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        mediaSession = MediaSession.Builder(application, exoPlayer).setSessionActivity(pi).build()
        PlaybackService.activeSession = mediaSession
        application.startService(Intent(application, PlaybackService::class.java))
        initializeAccountAndProfiles(); fetchTopCharts(); fetchRadioStations(); startVisualizerLoop(); generateParticles()
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying; if (isPlaying) startProgressPolling() else progressJob?.cancel() }
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) next() }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("DULCEPLAY_VIDA", "❌ ERROR EXOPLAYER: ${error.message}")
                _mediaError.value = "Error de reproducción"
            }
        })
    }

    fun buscarEnYouTube(texto: String) {
        if (texto.isBlank()) { _onlineSearchResults.value = emptyList(); return }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _onlineSearchResults.value = emptyList()
            try {
                val datosRecibidos = motor.buscar(texto)
                _onlineSearchResults.value = datosRecibidos.map { 
                    com.dulce.play.domain.model.MediaItem(
                        id = it.id,
                        title = it.titulo,
                        artist = it.canal,
                        coverUrl = it.imagen,
                        streamUrl = it.id // Usamos ID como URL temporal
                    ) 
                }
            } catch (e: Exception) {
                Log.e("DULCEPLAY_VIDA", "❌ ERROR VM: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun cargarOpcionesParaReproducir(videoId: String) {
        viewModelScope.launch {
            _isSearching.value = true
            val opciones = motor.obtenerOpcionesReproduccion(videoId)
            _formatosDisponibles.value = opciones
            _isSearching.value = false
        }
    }

    fun reproducirConCalidad(urlEnlace: String) {
        if (urlEnlace.isBlank()) return
        try {
            Log.d("DULCEPLAY_VIDA", "✅ -> REPRODUCIENDO URL: $urlEnlace")
            val mediaItem = MediaItem.fromUri(urlEnlace)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            Log.e("DULCEPLAY_VIDA", "❌ ERROR REPRODUCCIÓN: ${e.message}")
        }
    }

    fun updateSearchQuery(q: String) { _searchQuery.value = q; buscarEnYouTube(q) }
    fun buscarAhora(q: String) { buscarEnYouTube(q) }
    fun executeSearch(q: String) { buscarEnYouTube(q) }
    
    fun playMedia(m: com.dulce.play.domain.model.MediaItem) {
        if (m.streamUrl.isBlank()) return
        _currentMedia.value = m
        // Si la URL no empieza con http, probablemente sea un videoId de YouTube
        if (!m.streamUrl.startsWith("http")) {
            cargarOpcionesParaReproducir(m.streamUrl)
            // Aquí el usuario tendrá que elegir calidad en la UI o auto-elegimos
        } else {
            reproducirConCalidad(m.streamUrl)
        }
    }
    
    fun playIPTVChannel(c: IPTVChannel) { playMedia(com.dulce.play.domain.model.MediaItem(title = c.name, streamUrl = c.streamUrl, mediaType = MediaType.IPTV)) }
    fun togglePlay() { if (exoPlayer.isPlaying) exoPlayer.pause() else { if (exoPlayer.playbackState == Player.STATE_IDLE) exoPlayer.prepare(); exoPlayer.play() } }
    fun next() { /* logic */ }
    fun prev() { /* logic */ }
    fun seekPercent(p: Float) { val dur = exoPlayer.duration; if (dur > 0) exoPlayer.seekTo((p * dur).toLong()) }
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
    fun createUserPlaylist(n: String) {}
    fun deleteUserPlaylist(id: String) {}
    fun removeTrackFromUserPlaylist(pid: String, mid: String) {}
    fun addTrackToUserPlaylist(pid: String, m: com.dulce.play.domain.model.MediaItem) {}
    fun importM3UPlaylist(name: String, text: String) {}
    fun loadM3UFromUrl(name: String, urlString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {}
    fun importXtreamCodes(serverUrl: String, username: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {}
    fun selectTheme(t: VisualTheme) { _activeTheme.value = t }
    fun stopCasting() { _isCasting.value = false }
    fun startCasting(d: String) { _isCasting.value = true }
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
        viewModelScope.launch { 
            try {
                val res = motor.listaColombia()
                _topColombia.value = res.map { com.dulce.play.domain.model.MediaItem(id = it.id, title = it.titulo, artist = it.canal, coverUrl = it.imagen, streamUrl = it.id) }
            } catch (e: Exception) {}
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
                    _playbackProgress.value = exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat() 
                }
                delay(500) 
            } 
        } 
    }
    fun initializeAccountAndProfiles() { 
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { 
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
        } 
    }
}
