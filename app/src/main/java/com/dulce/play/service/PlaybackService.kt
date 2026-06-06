package com.dulce.play.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    
    companion object {
        var activeSession: MediaSession? = null
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        // Return the session managed by the ViewModel or a shared instance
        return PlaybackService.activeSession
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
