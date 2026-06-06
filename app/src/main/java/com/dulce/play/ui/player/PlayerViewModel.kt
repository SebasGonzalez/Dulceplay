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
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    enum class SearchFilter { MUSICA, VIDEO_IPTV, TODO }

    private val _searchFilter = MutableStateFlow(SearchFilter.TODO)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()
    fun setSearchFilter(f: SearchFilter) { _searchFilter.value = f; if (_searchQuery.value.isNotBlank()) executeSearch(_searchQuery.value) }

    private val baseIptvList = listOf(IPTVChannel("ch_def_1", "DulcePlay TV", "General", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "globe_logo"))

    var profiles by mutableStateOf<List<UserProfile>>(emptyList())
    private val _currentAccount = MutableStateFlow<UserAccountEntity?>(null)
    val currentAccount: StateFlow<UserAccountEntity?> = _currentAccount.asStateFlow()

    @OptIn(androidx.media3.common.util.UnstableApi::class)
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build().apply { 
        repeatMode = Player.REPEAT_MODE_OFF
        playWhenReady = true
    }
    private var mediaSession: MediaSession? = null

    private val _currentProfile = MutableStateFlow<UserProfile>(UserProfile("p1", "Usuario", "avatar_0", isPremium = true))
    val currentProfile: StateFlow<UserProfile> = _currentProfile.asStateFlow()

    private val _currentMedia = MutableStateFlow<MediaItem>(MediaItem("init", "Sintonizando DulcePlay", "Oficial", "Sistema", "00:00", 0, "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17", "", MediaType.AUDIO))
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

    private val _accentColor = MutableStateFlow(ElectricBlue)
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

    private val _particles = MutableStateFlow<List<Pair<Float, Float>>>(List(20) { Random.nextFloat() to Random.nextFloat() })
    val particles: StateFlow<List<Pair<Float, Float>>> = _particles.asStateFlow()

    private val _mediaError = MutableStateFlow<String?>(null)
    val mediaError: StateFlow<String?> = _mediaError.asStateFlow()

    private val _activeTheme = MutableStateFlow(VisualTheme.CYBER_NEON)
    val activeTheme: StateFlow<VisualTheme> = _activeTheme.asStateFlow()

    private var progressJob: Job? = null; private var searchJob: Job? = null
    private val db = androidx.room.Room.databaseBuilder(application, AppDatabase::class.java, "dulce_database").fallbackToDestructiveMigration().build()

    init {
        val pi = PendingIntent.getActivity(application, 0, Intent(application, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        mediaSession = MediaSession.Builder(application, exoPlayer).setSessionActivity(pi).build()
        PlaybackService.activeSession = mediaSession
        application.startService(Intent(application, PlaybackService::class.java))
        initializeAccountAndProfiles(); fetchTopCharts(); fetchRadioStations()
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
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { _accentColor.value = listOf(ElectricBlue, DeepPurple, PremiumGold)[cIdx % 3]; _voiceType.value = vT }
        }
    }

    private fun fetchRadioStations() { _radioStations.value = listOf(IPTVChannel("r_co_1", "Caracol Radio", "Colombia", "https://26623.live.streamtheworld.com/CARACOL_RADIO_SC", "caracol"), IPTVChannel("r_co_2", "Olímpica Stereo", "Colombia", "https://18493.live.streamtheworld.com/OLIMPICA_STEREOSC", "olimpica")) }
    
    fun playMedia(m: MediaItem) {
        if (m.streamUrl.isBlank()) return
        _mediaError.value = null; _currentMedia.value = m
        val cr = _recentPlayed.value.toMutableList(); cr.remove(m); cr.add(0, m); _recentPlayed.value = cr.take(10)
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try { 
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                
                // 🔴 1. LIMPIEZA PROFUNDA DE ENLACES (IRON RULE)
                val uri = Uri.parse(m.streamUrl)
                val cleanUri = Uri.Builder()
                    .scheme(uri.scheme)
                    .encodedAuthority(uri.authority)
                    .encodedPath(uri.path) // Rule: No queries, no fragments
                    .build()
                
                val mediaItem3 = Media3Item.Builder().setUri(cleanUri).setMediaId(m.id).build()
                
                exoPlayer.setMediaItem(mediaItem3)
                
                // 🔴 2. CONFIGURACIÓN FORZADA (INICIO EN 00:00)
                exoPlayer.prepare()
                exoPlayer.seekTo(0) // FORCE JUMP TO ZERO ABSOLUTE
                exoPlayer.playWhenReady = true
                exoPlayer.play() 
            } catch (e: Exception) { _mediaError.value = "Fallo de conexión" }
        }
    }

    fun playIPTVChannel(c: IPTVChannel) { playMedia(MediaItem(c.id, c.name, "En Vivo", "IPTV", "00:00", 0, c.logoUrl, c.streamUrl, MediaType.IPTV)) }
    fun togglePlay() { if (_childLockEnabled.value) return; if (exoPlayer.isPlaying) exoPlayer.pause() else { if (exoPlayer.playbackState == Player.STATE_IDLE) exoPlayer.prepare(); exoPlayer.play() } }
    fun next() { val l = getFilteredMediaList(); if (l.isNotEmpty()) { val i = l.indexOfFirst { it.id == _currentMedia.value.id }; if (i != -1 && i + 1 < l.size) playMedia(l[i + 1]) } }
    fun prev() { val l = getFilteredMediaList(); if (l.isNotEmpty()) { val i = l.indexOfFirst { it.id == _currentMedia.value.id }; if (i > 0) playMedia(l[i - 1]) } }
    fun skip15(f: Boolean) { if (_childLockEnabled.value) return; val d = if (f) 15000L else -15000L; exoPlayer.seekTo((exoPlayer.currentPosition + d).coerceIn(0, exoPlayer.duration)) }
    fun seekPercent(p: Float) { if (_childLockEnabled.value) return; val dur = exoPlayer.duration; if (dur > 0) exoPlayer.seekTo((p * dur).toLong()) }
    fun toggleFavorite(item: MediaItem) { val cur = _favorites.value.toMutableList(); if (cur.any { it.id == item.id }) cur.removeAll { it.id == item.id } else cur.add(0, item); _favorites.value = cur }
    fun isFavorite(id: String) = _favorites.value.any { it.id == id }
    fun setAccentColor(c: Color) { _accentColor.value = c; val idx = if (c == ElectricBlue) 0 else if (c == DeepPurple) 1 else 2; viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { db.appSettingsDao().saveSetting(AppSettingsEntity("accent_color_idx", idx.toString())) } }
    fun setVoiceType(t: String) { _voiceType.value = t; viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { db.appSettingsDao().saveSetting(AppSettingsEntity("voice_type", t)) } }
    fun setAppBrightness(v: Float) { _appBrightness.value = v }
    fun adjustVolume(d: Float) { if (_childLockEnabled.value) return; exoPlayer.volume = (exoPlayer.volume + d).coerceIn(0f, 1f) }
    fun toggleVoiceActivation() { _voiceActivationEnabled.value = !_voiceActivationEnabled.value }
    fun toggleChildLock() { _childLockEnabled.value = !_childLockEnabled.value }
    fun toggleExtremeSaver() { _extremeBatterySaver.value = !_extremeBatterySaver.value }
    fun toggleEasyMode() { _isEasyMode.value = !_isEasyMode.value }
    fun toggleShuffle() { if (_childLockEnabled.value) return; _shuffleEnabled.value = !_shuffleEnabled.value }
    fun toggleRepeat() { if (_childLockEnabled.value) return; _repeatEnabled.value = !_repeatEnabled.value }
    fun toggleLyrics() { _showLyrics.value = !_showLyrics.value }
    fun playList(list: List<MediaItem>) { if (list.isNotEmpty()) playMedia(list[0]) }

    private fun fetchTopCharts() { 
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { 
            _topColombia.value = performPublicSearch("Vallenato Salsa Urbano Colombia éxitos oficiales", "music").take(10)
            _topMexico.value = performPublicSearch("Corridos Banda Regional Mexicano oficial", "music").take(10)
            _topGlobal.value = performPublicSearch("Top Global Hits official complete", "music").take(10)
        } 
    }

    fun updateSearchQuery(q: String) { _searchQuery.value = q }

    fun executeSearch(q: String) {
        if (q.isBlank()) return
        searchJob?.cancel(); _isSearching.value = true
        searchJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val res = mutableListOf<MediaItem>(); val filter = _searchFilter.value
                val stableQuery = if (!q.lowercase().contains("oficial")) "$q canción oficial completo" else q
                
                if (filter == SearchFilter.MUSICA || filter == SearchFilter.TODO) res.addAll(performPublicSearch(stableQuery, "music"))
                if (filter == SearchFilter.VIDEO_IPTV || filter == SearchFilter.TODO) res.addAll(performPublicSearch(stableQuery, "video"))
                
                _onlineSearchResults.value = res.distinctBy { it.title + it.artist }
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
                val durationSec = (obj.optLong("trackTimeMillis", 0) / 1000).toInt()
                
                if (durationSec < 90 || durationSec > 600) continue
                
                val rawStream = obj.optString("previewUrl", "")
                if (rawStream.isNotEmpty()) {
                    val cleanStream = Uri.parse(rawStream).buildUpon().clearQuery().fragment("").build().toString()
                    items.add(MediaItem(UUID.randomUUID().toString(), obj.optString("trackName"), obj.optString("artistName"), "Oficial", String.format("%02d:%02d", durationSec / 60, durationSec % 60), durationSec, obj.optString("artworkUrl100").replace("100x100", "600x600"), cleanStream, if (type == "video") MediaType.VIDEO else MediaType.AUDIO))
                }
            }
            items
        } catch (e: Exception) { emptyList() }
    }

    fun setSearchOverlayActive(a: Boolean) { _searchOverlayActive.value = a }
    fun getFilteredMediaList() = if (_onlineSearchResults.value.isNotEmpty()) _onlineSearchResults.value else if (_topColombia.value.isNotEmpty()) _topColombia.value else listOf(_currentMedia.value)
    fun getAllIPTVChannels() = radioStations.value + baseIptvList
    private fun startProgressPolling() { progressJob = viewModelScope.launch { while(true) { if (exoPlayer.duration > 0) { _currentTimeSeconds.value = (exoPlayer.currentPosition / 1000).toInt(); _playbackProgress.value = exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat() }; delay(500) } } }
    private fun initializeAccountAndProfiles() { viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { var session = db.userAccountDao().getActiveSession(); if (session == null) { val email = "usuario@dulceplay.com"; val acc = UserAccountEntity(email, "hash", "Usuario", true); db.userAccountDao().insertAccount(acc); session = acc; db.userProfileDao().insertProfile(UserProfileEntity("p1", email, "Usuario", "avatar_0", true, "Salsa")) }; _currentAccount.value = session } }
    fun logout() { viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { db.userAccountDao().clearActiveSessions(); _currentAccount.value = null } }
    fun login(e: String, p: String, cb: (Boolean, String) -> Unit) { viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { db.userAccountDao().clearActiveSessions(); db.userAccountDao().setActiveSession(e); initializeAccountAndProfiles(); cb(true, "Ok") } }
    fun register(e: String, p: String, n: String, cb: (Boolean, String) -> Unit) { viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { db.userAccountDao().insertAccount(UserAccountEntity(e, "hash", n, true)); initializeAccountAndProfiles(); cb(true, "Ok") } }
    fun registerOrLoginOAuth(e: String, n: String, pr: String, cb: (Boolean) -> Unit) { viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { db.userAccountDao().insertAccount(UserAccountEntity(e, "hash", n, true)); initializeAccountAndProfiles(); cb(true) } }
    fun scanLocalMedia() {}
}
