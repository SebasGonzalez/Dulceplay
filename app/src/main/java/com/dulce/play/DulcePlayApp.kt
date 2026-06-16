package com.dulce.play

import android.app.Application
import android.util.Log
import com.dulce.play.ui.assistant.LocalAIEngine

class DulcePlayApp : Application() {
    companion object {
        lateinit var instance: DulcePlayApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            // INICIALIZAMOS EL SISTEMA DE EXTRACCIÓN UNA SOLA VEZ
            com.yausername.youtubedl_android.YoutubeDL.getInstance().init(this)
        } catch (e: Exception) {
            Log.e("DulcePlayApp", "Error al inicializar YoutubeDL: ${e.message}")
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // Liberar motor IA SOLO al cerrar completamente la app
        try {
            LocalAIEngine.release()
            Log.d("DulcePlayApp", "✅ Motor IA liberado al cerrar app")
        } catch (e: Exception) {
            Log.w("DulcePlayApp", "Error liberando motor IA: ${e.message}")
        }
    }
}
