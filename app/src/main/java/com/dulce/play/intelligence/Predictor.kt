package com.dulce.play.intelligence

import android.content.Context
import com.dulce.play.data.local.entity.UserIntelligenceEntity
import java.util.*

class Predictor(private val context: Context) {
    
    fun predictNextAction(intelligence: UserIntelligenceEntity): String? {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        // Example logic for "Son las 21:58..."
        if (hour == 21 && minute >= 55) {
            return "Sueles escuchar música relajante para dormir pronto. ¿Quieres que prepare tu lista?"
        }
        
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && hour in 14..16) {
            return "Es domingo tarde. ¿Quieres ver películas familiares?"
        }
        
        return null
    }
}
