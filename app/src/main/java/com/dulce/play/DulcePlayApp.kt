package com.dulce.play

import android.app.Application
import com.yausername.youtubedl_android.YoutubeDL

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
            YoutubeDL.getInstance().init(this)
        } catch (e: Exception) {
            android.util.Log.e("DulcePlayApp", "Error al inicializar YoutubeDL", e)
        }
    }
}
