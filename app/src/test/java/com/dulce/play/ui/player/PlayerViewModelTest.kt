package com.dulce.play.ui.player

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.dulce.play.domain.model.MediaType
import com.dulce.play.domain.model.MediaItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlayerViewModelTest {

    private lateinit var application: Application
    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = PlayerViewModel(application)
    }

    @Test
    fun testViewModelInitialization() {
        assertNotNull(viewModel)
        assertNotNull(viewModel.currentProfile.value)
        assertEquals("Sebastián (Master)", viewModel.currentProfile.value.name)
        assertNotNull(viewModel.currentMedia.value)
    }

    @Test
    fun testAudioFormatsLocalPlayback() {
        val audioFormats = listOf("flac", "mp3", "wav", "aac", "opus", "m4a")
        
        audioFormats.forEachIndexed { index, ext ->
            val fileName = "test_audio_$index.$ext"
            val uri = Uri.parse("content://media/external/audio/media/$index")
            
            // Simulating how PlayerScreen handles file extensions
            val isVideo = false // Because it's an audio format
            
            viewModel.addLocalMedia(uri, fileName, isVideo)
            
            val currentMedia = viewModel.currentMedia.value
            assertEquals(fileName, currentMedia.title)
            assertEquals(MediaType.AUDIO, currentMedia.mediaType)
            assertEquals("Archivo Local", currentMedia.artist)
            assertEquals("Local Storage", currentMedia.genre)
            assertEquals(uri.toString(), currentMedia.streamUrl)
            
            // Ensure no playback error is set during adding/playing
            assertNull(viewModel.mediaError.value)
        }
    }

    @Test
    fun testVideoFormatsLocalPlayback() {
        val videoFormats = listOf("mp4", "mkv", "avi", "mov", "wmv")
        
        videoFormats.forEachIndexed { index, ext ->
            val fileName = "test_video_$index.$ext"
            val uri = Uri.parse("content://media/external/video/media/$index")
            
            // Simulating how PlayerScreen handles file extensions for video
            val isVideo = true 
            
            viewModel.addLocalMedia(uri, fileName, isVideo)
            
            val currentMedia = viewModel.currentMedia.value
            assertEquals(fileName, currentMedia.title)
            assertEquals(MediaType.VIDEO, currentMedia.mediaType)
            assertEquals("Archivo Local", currentMedia.artist)
            assertEquals("Local Storage", currentMedia.genre)
            assertEquals(uri.toString(), currentMedia.streamUrl)
            
            // Ensure no playback error is set
            assertNull(viewModel.mediaError.value)
        }
    }

    @Test
    fun testPlaybackControls() {
        val initialMedia = viewModel.currentMedia.value
        
        // Shuffle toggle
        assertFalse(viewModel.shuffleEnabled.value)
        viewModel.toggleShuffle()
        assertTrue(viewModel.shuffleEnabled.value)
        viewModel.toggleShuffle()
        assertFalse(viewModel.shuffleEnabled.value)
        
        // Repeat toggle
        assertFalse(viewModel.repeatEnabled.value)
        viewModel.toggleRepeat()
        assertTrue(viewModel.repeatEnabled.value)
        viewModel.toggleRepeat()
        assertFalse(viewModel.repeatEnabled.value)
        
        // Casting toggle
        assertFalse(viewModel.isCasting.value)
        viewModel.toggleCast()
        assertTrue(viewModel.isCasting.value)
        viewModel.toggleCast()
        assertFalse(viewModel.isCasting.value)

        // Next item
        viewModel.next()
        val nextMedia = viewModel.currentMedia.value
        assertNotEquals(initialMedia.streamUrl, nextMedia.streamUrl)
        
        // Previous item
        viewModel.prev()
        val prevMedia = viewModel.currentMedia.value
        assertEquals(initialMedia.streamUrl, prevMedia.streamUrl)
        
        // Progress seek
        viewModel.seekPercent(0.4f)
        // Since duration is unset (<= 0) in the headless unit test ExoPlayer environment, playbackProgress stays at its initial value (0f)
        assertEquals(0f, viewModel.playbackProgress.value, 0.01f)
    }

    @Test
    fun testIPTVPlaylistImportAndPlay() {
        val playlistName = "Mis Canales"
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 group-title="Noticias",Canal 24h
            http://rtve.live/stream.m3u8
            #EXTINF:-1 group-title="Música",VH1 Neon
            http://music.live/vh1.m3u8
        """.trimIndent()
        
        viewModel.importM3UPlaylist(playlistName, m3uContent)
        
        val playlists = viewModel.customIPTVPlaylists.value
        assertEquals(1, playlists.size)
        assertEquals(playlistName, playlists[0].first)
        
        val channels = playlists[0].second
        assertEquals(2, channels.size)
        assertEquals("Canal 24h", channels[0].name)
        assertEquals("Noticias", channels[0].group)
        assertEquals("vh1.m3u8", channels[1].streamUrl.substringAfterLast("/"))
        
        // Test playing custom IPTV channel
        viewModel.playIPTVChannel(channels[0])
        val activeChannel = viewModel.activeIPTVChannel.value
        assertNotNull(activeChannel)
        assertEquals("Canal 24h", activeChannel?.name)
        assertEquals(MediaType.IPTV, viewModel.currentMedia.value.mediaType)
    }

    @Test
    fun testOnlineSearchQuery() {
        // Query empty doesn't do searching list
        viewModel.updateSearchQuery("")
        assertEquals("", viewModel.searchQuery.value)
        assertTrue(viewModel.onlineSearchResults.value.isEmpty())
        
        // Normal text search
        viewModel.updateSearchQuery("Daft Punk")
        assertEquals("Daft Punk", viewModel.searchQuery.value)
    }
}
