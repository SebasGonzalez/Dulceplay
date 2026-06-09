package com.dulce.play.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    companion object {
        var activeSession: MediaSession? = null
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return activeSession
    }

    override fun onDestroy() {
        activeSession?.run {
            player.release()
            release()
        }
        activeSession = null
        super.onDestroy()
    }
}
