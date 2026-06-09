package com.dulce.play.ui.player

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import com.dulce.play.ui.theme.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.dulce.play.MainActivity
import com.dulce.play.domain.model.*
import com.dulce.play.service.PlaybackService
import com.dulce.play.data.local.entity.*
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random
import kotlin.math.sin

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    enum class SearchFilter { MUSICA, VIDEO_IPTV, TODO }
    enum class CastState { DISCONNECTED, SEARCHING, CONNECTED }
    enum class VisualTheme { CYBER_NEON, CLASSIC_DARK, ELECTRIC_BLUE, NATURE_GREEN }

    private val _searchFilter = MutableStateFlow(SearchFilter.TODO)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()
    fun setSearchFilter(f: SearchFilter) { _searchFilter.value = f; if (_searchQuery.value.isNotBlank()) executeSearch(_searchQuery.value) }

    private val _currentAccount = MutableStateFlow<UserAccountEntity?>(null)
    val currentAccount: StateFlow<UserAccountEntity?> = _currentAccount.asStateFlow()

    var profiles by mutableStateOf<List<UserProfile>>(emptyList())

    @OptIn(androidx.media3.common.util.UnstableApi::class)
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build().apply { repeatMode = Player.REPEAT_MODE_OFF }
    private var mediaSession: MediaSession? = null

    private val _currentProfile = MutableStateFlow(UserProfile("p1", "Usuario", "avatar_0", true))
    val currentProfile: StateFlow<UserProfile> = _currentProfile.asStateFlow()

    private val _selectedCountry = MutableStateFlow("Global")
    val selectedCountry: StateFlow<String> = _selectedCountry.asStateFlow()

    private val _currentMedia = MutableStateFlow(MediaItem(title = "Sintonizando DulcePlay", streamUrl = ""))
    val currentMedia: StateFlow<MediaItem> = _currentMedia.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f) 
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _currentTimeSeconds = MutableStateFlow(0)
    val currentTimeSeconds: StateFlow<Int> = _currentTimeSeconds.asStateFlow()

    private val _topColombia = MutableStateFlow<List<MediaItem>>(emptyList())
    val topColombia: StateFlow<List<MediaItem>> = _topColombia.asStateFlow()
    private val _topMexico = MutableStateFlow<List<MediaItem>>(emptyList())
    val topMexico: StateFlow<List<MediaItem>> = _topMexico.asStateFlow()
    private val _topGlobal = MutableStateFlow<List<MediaItem>>(emptyList())
    val topGlobal: StateFlow<List<MediaItem>> = _topGlobal.asStateFlow()

    private val _favorites = MutableStateFlow<List<MediaItem>>(emptyList())
    val favorites: StateFlow<List<MediaItem>> = _favorites.asStateFlow()

    private val _recentPlayed = MutableStateFlow<List<MediaItem>>(emptyList())
    val recentPlayed: StateFlow<List<MediaItem>> = _recentPlayed.asStateFlow()

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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchOverlayActive = MutableStateFlow(false)
    val searchOverlayActive: StateFlow<Boolean> = _searchOverlayActive.asStateFlow()

    private val _voiceActivationEnabled = MutableStateFlow(false)
    val voiceActivationEnabled: StateFlow<Boolean> = _voiceActivationEnabled.asStateFlow()

    private val _childLockEnabled = MutableStateFlow(false)
    val childLockEnabled: StateFlow<Boolean> = _childLockEnabled.asStateFlow()

    private val _extremeBatterySaver = MutableStateFlow(false)
    val extremeBatterySaver: StateFlow<Boolean> = _extremeBatterySaver.asStateFlow()

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

    private val _onlineSearchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val onlineSearchResults: StateFlow<List<MediaItem>> = _onlineSearchResults.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatEnabled = MutableStateFlow(false)
    val repeatEnabled: StateFlow<Boolean> = _repeatEnabled.asStateFlow()

    private val _showLyrics = MutableStateFlow(false)
    val showLyrics: StateFlow<Boolean> = _showLyrics.asStateFlow()

    private val _currentLyrics = MutableStateFlow("Disfruta la música completa...")
    val currentLyrics: StateFlow<String> = _currentLyrics.asStateFlow()

    private val _visualizerBars = MutableStateFlow(List(32) { 0.1f })
    val visualizerBars: StateFlow<List<Float>> = _visualizerBars.asStateFlow()

    private val _particles = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val particles: StateFlow<List<Pair<Float, Float>>> = _particles.asStateFlow()

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

    private val _localMediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val localMediaList: StateFlow<List<MediaItem>> = _localMediaList.asStateFlow()

    private val _customIPTVPlaylists = MutableStateFlow<List<Pair<String, List<IPTVChannel>>>>(emptyList())
    val customIPTVPlaylists: StateFlow<List<Pair<String, List<IPTVChannel>>>> = _customIPTVPlaylists.asStateFlow()

    private val _activeIPTVChannel = MutableStateFlow<IPTVChannel?>(null)
    val activeIPTVChannel: StateFlow<IPTVChannel?> = _activeIPTVChannel.asStateFlow()

    private val baseIptvList = listOf(IPTVChannel("ch_def_1", "DulcePlay TV", "General", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "globe_logo"))

    private var progressJob: Job? = null
    private var searchJob: Job? = null
    private var visualizerJob: Job? = null
    private var particlesJob: Job? = null
    private var sleepTimerJob: Job? = null
    private val db = androidx.room.Room.databaseBuilder(application, AppDatabase::class.java, "dulce_database").fallbackToDestructiveMigration().build()

    init {
        val pi = PendingIntent.getActivity(application, 0, Intent(application, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        mediaSession = MediaSession.Builder(application, exoPlayer).setSessionActivity(pi).build()
        PlaybackService.activeSession = mediaSession
        application.startService(Intent(application, PlaybackService::class.java))
        initializeAccountAndProfiles(); fetchTopCharts(); fetchRadioStations(); startVisualizerLoop(); generateParticles()
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying; if (isPlaying) startProgressPolling() else progressJob?.cancel() }
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) next() }
        })
        loadPersistentSettings()
    }

    private fun loadPersistentSettings() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val cIdx = db.appSettingsDao().getSettingValue("accent_color_idx")?.toIntOrNull() ?: 0
            val vT = db.appSettingsDao().getSettingValue("voice_type") ?: "female"
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { 
                _accentColor.value = Color.Cyan
                _voiceType.value = vT 
            }
        }
    }

    private fun fetchRadioStations() { _radioStations.value = listOf(IPTVChannel("r_co_1", "Caracol Radio", "Colombia", "https://26623.live.streamtheworld.com/CARACOL_RADIO_SC", "caracol"), IPTVChannel("r_co_2", "Olímpica Stereo", "Colombia", "https://18493.live.streamtheworld.com/OLIMPICA_STEREOSC", "olimpica")) }
    
    fun playMedia(m: MediaItem) {
        if (m.streamUrl.isBlank()) return
        _mediaError.value = null; _currentMedia.value = m
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try { 
                exoPlayer.stop(); exoPlayer.clearMediaItems()
                val uri = Uri.parse(m.streamUrl)
                val cleanUri = Uri.Builder().scheme(uri.scheme).encodedAuthority(uri.authority).encodedPath(uri.path).build()
                exoPlayer.setMediaItem(Media3Item.fromUri(cleanUri))
                exoPlayer.prepare(); exoPlayer.seekTo(0); exoPlayer.playWhenReady = true; exoPlayer.play() 
            } catch (e: Exception) { _mediaError.value = "Fallo de conexión" }
        }
    }

    fun playIPTVChannel(c: IPTVChannel) { playMedia(MediaItem(id = c.id, title = c.name, artist = "En Vivo", album = "IPTV", coverUrl = c.logoUrl, streamUrl = c.streamUrl, mediaType = MediaType.IPTV)) }
    fun togglePlay() { if (_childLockEnabled.value) return; if (exoPlayer.isPlaying) exoPlayer.pause() else { if (exoPlayer.playbackState == Player.STATE_IDLE) exoPlayer.prepare(); exoPlayer.play() } }
    fun next() { val l = getFilteredMediaList(); if (l.isNotEmpty()) { val i = l.indexOfFirst { it.streamUrl == _currentMedia.value.streamUrl }; if (i != -1 && i + 1 < l.size) playMedia(l[i + 1]) } }
    fun prev() { val l = getFilteredMediaList(); if (l.isNotEmpty()) { val i = l.indexOfFirst { it.streamUrl == _currentMedia.value.streamUrl }; if (i > 0) playMedia(l[i - 1]) } }
    fun skip15(f: Boolean) { if (_childLockEnabled.value) return; val d = if (f) 15000L else -15000L; exoPlayer.seekTo((exoPlayer.currentPosition + d).coerceIn(0, exoPlayer.duration)) }
    fun seekPercent(p: Float) { val dur = exoPlayer.duration; if (dur > 0) exoPlayer.seekTo((p * dur).toLong()) }
    fun toggleFavorite(item: MediaItem) { val cur = _favorites.value.toMutableList(); if (cur.any { it.id == item.id }) cur.removeAll { it.id == item.id } else cur.add(0, item); _favorites.value = cur }
    fun isFavorite(id: String) = _favorites.value.any { it.id == id }
    fun setAccentColor(c: Color) { _accentColor.value = c; viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { db.appSettingsDao().saveSetting(AppSettingsEntity("accent_color_idx", "0")) } }
    fun setVoiceType(t: String) { _voiceType.value = t; viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { db.appSettingsDao().saveSetting(AppSettingsEntity("voice_type", t)) } }
    fun setAppBrightness(v: Float) { _appBrightness.value = v }
    fun adjustVolume(d: Float) { if (_childLockEnabled.value) return; exoPlayer.volume = (exoPlayer.volume + d).coerceIn(0f, 1f) }
    fun toggleVoiceActivation() { _voiceActivationEnabled.value = !_voiceActivationEnabled.value }
    fun toggleChildLock() { _childLockEnabled.value = !_childLockEnabled.value }
    fun toggleExtremeSaver() { _extremeBatterySaver.value = !_extremeBatterySaver.value }
    fun toggleEasyMode() { _isEasyMode.value = !_isEasyMode.value }
    fun toggleDrivingMode() { _isDrivingMode.value = !_isDrivingMode.value }
    fun toggleFamilyMode() { _isFamilyMode.value = !_isFamilyMode.value }
    fun toggleWellnessMode() { _isWellnessMode.value = !_isWellnessMode.value }
    fun toggleShuffle() { if (_childLockEnabled.value) return; _shuffleEnabled.value = !_shuffleEnabled.value }
    fun toggleRepeat() { if (_childLockEnabled.value) return; _repeatEnabled.value = !_repeatEnabled.value }
    fun toggleLyrics() { _showLyrics.value = !_showLyrics.value }
    fun playList(list: List<MediaItem>) { if (list.isNotEmpty()) playMedia(list[0]) }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingSeconds.value = minutes * 60
        sleepTimerJob = viewModelScope.launch {
            while (_sleepTimerRemainingSeconds.value > 0) { delay(1000); _sleepTimerRemainingSeconds.value-- }
            if (exoPlayer.isPlaying) { exoPlayer.pause(); _isPlaying.value = false }
        }
    }
    fun stopSleepTimer() { sleepTimerJob?.cancel(); _sleepTimerRemainingSeconds.value = 0 }

    fun selectTheme(theme: VisualTheme) { _activeTheme.value = theme }

    fun searchCastDevices() {
        _castState.value = CastState.SEARCHING
        viewModelScope.launch { delay(1500); _availableCastDevices.value = listOf("SmartTV Sala 📺", "Nest Hub 📡") }
    }
    fun startCasting(device: String) { _castState.value = CastState.CONNECTED; _castDevice.value = device; _isCasting.value = true }
    fun stopCasting() { _castState.value = CastState.DISCONNECTED; _castDevice.value = null; _isCasting.value = false }
    fun toggleCast() { _isCasting.value = !_isCasting.value }

    fun toggleEqualizer() { _eqEnabled.value = !_eqEnabled.value }
    fun updateEqBand(idx: Int, value: Int) { val cur = _eqBands.value.toMutableList(); cur[idx] = value; _eqBands.value = cur }
    fun applyEqualizerPreset(preset: String) { _eqPreset.value = preset; _eqBands.value = listOf(50, 50, 50, 50, 50) }

    fun toggleAssistantVoice() { _assistantVoiceEnabled.value = !_assistantVoiceEnabled.value }
    fun toggleAssistantAutoLearn() { _assistantAutoLearn.value = !_assistantAutoLearn.value }
    fun setProfileLockPin(pid: String, pin: String) { val cur = _profileLocks.value.toMutableMap(); if (pin.isBlank()) cur.remove(pid) else cur[pid] = pin; _profileLocks.value = cur }

    fun createJSONBackup(): String = "{ \"version\": 1 }"
    fun restoreFromBackupJSON(json: String): Boolean = true

    private fun fetchTopCharts() { 
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { 
            cargarSeccionColombia()
            cargarSeccionMexico()
            val motor = com.dulce.play.utils.SearchEngine()
            val results = motor.buscar("Top Global Hits official complete")
            _topGlobal.value = results.map { (t, u) -> MediaItem(title = t, streamUrl = u, duration = "") }
        } 
    }

    fun updateSearchQuery(q: String) { _searchQuery.value = q }
    fun executeSearch(q: String) {
        if (q.isBlank()) return
        searchJob?.cancel(); _isSearching.value = true
        searchJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val motor = com.dulce.play.utils.SearchEngine()
                val stableQuery = if (!q.lowercase().contains("oficial")) "$q oficial completo" else q
                val resultados = motor.buscar(stableQuery)
                val items = resultados.map { (titulo, url) ->
                    MediaItem(
                        title = titulo,
                        streamUrl = url,
                        mediaType = if (_searchFilter.value == SearchFilter.VIDEO_IPTV) MediaType.VIDEO else MediaType.AUDIO,
                        duration = ""
                    )
                }
                _onlineSearchResults.value = items
            } catch (e: Exception) { e.printStackTrace() } finally { _isSearching.value = false }
        }
    }

    private fun performPublicSearch(q: String, type: String): List<MediaItem> {
        return try {
            val enc = java.net.URLEncoder.encode(q, "UTF-8")
            val entity = if (type == "video") "musicVideo" else "song"
            val url = java.net.URL("https://itunes.apple.com/search?media=music&limit=40&term=$enc&entity=$entity")
            val json = org.json.JSONObject(url.readText()); val array = json.getJSONArray("results"); val items = mutableListOf<MediaItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val dSec = (obj.optLong("trackTimeMillis", 0) / 1000).toInt()
                if (dSec < 30) continue
                val raw = obj.optString("previewUrl", "")
                if (raw.isNotEmpty()) {
                    val clean = Uri.parse(raw).buildUpon().clearQuery().fragment("").build().toString()
                    items.add(MediaItem(id = UUID.randomUUID().toString(), title = obj.optString("trackName"), artist = obj.optString("artistName"), streamUrl = clean, mediaType = if (type == "video") MediaType.VIDEO else MediaType.AUDIO, durationText = String.format("%02d:%02d", dSec / 60, dSec % 60), durationSeconds = dSec, coverUrl = obj.optString("artworkUrl100").replace("100x100", "600x600")))
                }
            }
            items
        } catch (e: Exception) { emptyList() }
    }

    fun cargarSeccionColombia() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val motor = com.dulce.play.utils.SearchEngine()
            val resultados = motor.listaColombia()
            _topColombia.value = resultados.map { (titulo, url) ->
                MediaItem(title = titulo, streamUrl = url, duration = "")
            }
        }
    }
    fun cargarSeccionMexico() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val motor = com.dulce.play.utils.SearchEngine()
            val resultados = motor.listaMexico()
            _topMexico.value = resultados.map { (titulo, url) ->
                MediaItem(title = titulo, streamUrl = url, duration = "")
            }
        }
    }

    fun setSearchOverlayActive(a: Boolean) { _searchOverlayActive.value = a }
    fun getFilteredMediaList() = if (_onlineSearchResults.value.isNotEmpty()) _onlineSearchResults.value else if (_topColombia.value.isNotEmpty()) _topColombia.value else listOf(_currentMedia.value)
    fun getAllIPTVChannels() = _radioStations.value + baseIptvList
    fun selectProfile(p: UserProfile) { _currentProfile.value = p; viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { db.appSettingsDao().saveSetting(AppSettingsEntity("last_profile_id", p.id)); loadAllDataFromRoom() } }
    fun selectCountry(c: String) { _selectedCountry.value = c }
    fun isMediaAvailableLocally(t: String, a: String): Boolean = false
    fun addLocalMedia(uri: Uri, name: String, video: Boolean) {
        val newItem = MediaItem(id = UUID.randomUUID().toString(), title = name, streamUrl = uri.toString(), mediaType = if (video) MediaType.VIDEO else MediaType.AUDIO)
        _localMediaList.value = _localMediaList.value + newItem
        playMedia(newItem)
    }
    fun scanLocalMedia() {}
    fun addSearchQueryToHistory(q: String) { val cur = _searchHistory.value.toMutableList(); cur.remove(q); cur.add(0, q); _searchHistory.value = cur.take(10) }
    fun clearSearchHistory() { _searchHistory.value = emptyList() }
    fun buscarAhora(q: String) { executeSearch(q) }
    fun getMoodPlaylists(): List<Pair<String, String>> = listOf("Cyberpunk Workout" to "Synthwave de alta velocidad", "Lluvia de Neón" to "Melancolía digital")

    private fun startVisualizerLoop() { 
        visualizerJob = viewModelScope.launch { 
            while(true) { 
                _visualizerBars.value = if (_isPlaying.value) List(32) { Random.nextFloat() } else List(32) { 0.1f }
                delay(100) 
            } 
        } 
    }

    private fun generateParticles() {
        particlesJob = viewModelScope.launch {
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

    fun loadAllDataFromRoom() { /* load room data */ }

    fun logout() { viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { db.userAccountDao().clearActiveSessions(); _currentAccount.value = null } }
    fun login(e: String, p: String, cb: (Boolean, String) -> Unit) { viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { db.userAccountDao().setActiveSession(e); initializeAccountAndProfiles(); cb(true, "Ok") } }
    fun register(e: String, p: String, n: String, cb: (Boolean, String) -> Unit) { viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { db.userAccountDao().insertAccount(UserAccountEntity(e, "hash", n, true)); initializeAccountAndProfiles(); cb(true, "Ok") } }
    fun registerOrLoginOAuth(email: String, displayName: String, provider: String, onResult: (Boolean) -> Unit) { 
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { 
            db.userAccountDao().insertAccount(UserAccountEntity(email, "hash", displayName, true))
            initializeAccountAndProfiles()
            onResult(true) 
        } 
    }
    
    fun createProfile(name: String, avatarUrl: String, favoriteGenre: String, isPremium: Boolean) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val email = _currentAccount.value?.email ?: return@launch
            db.userProfileDao().insertProfile(UserProfileEntity(UUID.randomUUID().toString(), email, name, avatarUrl, isPremium, favoriteGenre))
            initializeAccountAndProfiles()
        }
    }
    fun deleteProfile(id: String) { viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { db.userProfileDao().deleteProfile(id); initializeAccountAndProfiles() } }
    fun removeTrackFromUserPlaylist(pid: String, mid: String) {}
    fun addTrackToUserPlaylist(pid: String, m: MediaItem) {}
    fun createUserPlaylist(name: String) {}
    fun deleteUserPlaylist(id: String) {}
    fun importM3UPlaylist(name: String, text: String) {}
    fun loadM3UFromUrl(name: String, urlString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {}
    fun importXtreamCodes(serverUrl: String, username: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {}
}
