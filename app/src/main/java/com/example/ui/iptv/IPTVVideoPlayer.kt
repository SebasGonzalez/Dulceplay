package com.example.ui.iptv

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.PlayerView

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun IPTVVideoPlayer(
    streamUrl: String,
    isPlaying: Boolean,
    onPlaybackStateChanged: (isPlaying: Boolean, isBuffering: Boolean, error: String?, availableQualities: List<String>) -> Unit,
    modifier: Modifier = Modifier,
    selectedQuality: String = "Auto"
) {
    if (streamUrl.isEmpty()) return

    val context = LocalContext.current

    // Set up bandwidth meter and adaptive track selector for automatic and manual quality changes
    val bandwidthMeter = remember { DefaultBandwidthMeter.getSingletonInstance(context) }
    val trackSelector = remember {
        DefaultTrackSelector(context)
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setBandwidthMeter(bandwidthMeter)
            .build()
            .apply {
                playWhenReady = isPlaying
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // Synchronize play state updates from outside controls
    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    // Handle stream switching when selected channel URL changes
    LaunchedEffect(streamUrl) {
        val uri = Uri.parse(streamUrl)
        val mediaItemBuilder = MediaItem.Builder().setUri(uri)

        // Enhance with Widevine DRM if premium tags or protected indicators are detected in stream
        if (streamUrl.contains("drm") || streamUrl.contains("license") || streamUrl.contains("widevine")) {
            mediaItemBuilder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri("https://proxy.uat.widevine.com/proxy?provider=widevine_test")
                    .setMultiSession(true)
                    .build()
            )
        }

        val mediaItem = mediaItemBuilder.build()
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        // Setup specialized adaptive media sources for HLS / DASH or fallback progressive MP4
        val mediaSource: MediaSource = when {
            streamUrl.endsWith(".m3u8") || streamUrl.contains("m3u8") || streamUrl.contains("index.m3u8") -> {
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            }
            streamUrl.endsWith(".mpd") || streamUrl.contains("mpd") -> {
                DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            else -> {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
    }

    // Adjust resolution constraints matching selected quality option (e.g. "Auto" or specific vertical dimensions like "720p")
    LaunchedEffect(selectedQuality) {
        val parameters = trackSelector.buildUponParameters()
        if (selectedQuality == "Auto") {
            parameters.clearVideoSizeConstraints()
        } else {
            val targetHeight = selectedQuality.replace("p", "").toIntOrNull()
            if (targetHeight != null) {
                parameters.setMaxVideoSize(C.LENGTH_UNSET, targetHeight)
                parameters.setMinVideoSize(0, targetHeight)
            }
        }
        trackSelector.setParameters(parameters)
    }

    // Set up event listeners for live channel buffer detection and comprehensive error diagnostics
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                val isBuffering = state == Player.STATE_BUFFERING
                val qualities = getCurrentAvailableQualities(trackSelector)
                onPlaybackStateChanged(
                    exoPlayer.playWhenReady && state != Player.STATE_ENDED,
                    isBuffering,
                    null,
                    qualities
                )
            }

            override fun onPlayerError(error: PlaybackException) {
                val errorMessage = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
                        "Fallo de conexión: Verifica tu cobertura a Internet."
                    }
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                        "Emisión fuera de línea: La antena o estación remota está caída temporalmente."
                    }
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> {
                        "Incompatibilidad de códec: Resoluciones h.265 o h.264 no adaptativas en el dispositivo."
                    }
                    PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> {
                        "Acceso Denegado (DRM): No se pudo obtener la licencia de retransmisión."
                    }
                    else -> "Retransmisión interrumpida: (${error.message ?: error.errorCodeName})"
                }
                onPlaybackStateChanged(
                    false,
                    false,
                    errorMessage,
                    getCurrentAvailableQualities(trackSelector)
                )
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Embedded Media3 Video Player View Canvas
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                setShowNextButton(false)
                setShowPreviousButton(false)
            }
        },
        modifier = modifier
    )
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
private fun getCurrentAvailableQualities(trackSelector: DefaultTrackSelector): List<String> {
    val qualities = mutableListOf("Auto")
    val currentMappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return qualities
    for (rendererIndex in 0 until currentMappedTrackInfo.rendererCount) {
        if (currentMappedTrackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_VIDEO) {
            val trackGroups = currentMappedTrackInfo.getTrackGroups(rendererIndex)
            for (groupIndex in 0 until trackGroups.length) {
                val group = trackGroups.get(groupIndex)
                for (trackIndex in 0 until group.length) {
                    val format = group.getFormat(trackIndex)
                    val height = format.height
                    if (height > 0) {
                        val label = "${height}p"
                        if (!qualities.contains(label)) {
                            qualities.add(label)
                        }
                    }
                }
            }
        }
    }
    // Return sorted qualities descending
    return (qualities.filter { it == "Auto" } + qualities.filter { it != "Auto" }.distinct().sortedByDescending { it.replace("p", "").toIntOrNull() ?: 0 })
}
