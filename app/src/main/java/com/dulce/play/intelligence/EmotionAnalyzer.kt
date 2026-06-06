package com.dulce.play.intelligence

import com.dulce.play.data.local.entity.PlaybackHistoryEntity

enum class Mood(val displayName: String, val colorHex: String) {
    HAPPY("Feliz", "#FFEB3B"),
    SAD("Triste", "#2196F3"),
    ENERGIZED("Motivado", "#FF5722"),
    RELAXED("Relajado", "#4CAF50"),
    FOCUSED("Concentrado", "#9C27B0"),
    NOSTALGIC("Nostálgico", "#795548"),
    NEUTRAL("Neutral", "#9E9E9E")
}

object EmotionAnalyzer {
    fun detectMood(historyItem: PlaybackHistoryEntity): Mood {
        val hour = historyItem.hourOfDay
        val energy = historyItem.energyLevel
        
        return when {
            hour in 6..10 && energy == "HIGH" -> Mood.ENERGIZED
            hour in 21..23 || hour in 0..5 -> Mood.RELAXED
            historyItem.mediaType == "AUDIO" && energy == "LOW" && hour > 18 -> Mood.NOSTALGIC
            historyItem.wasCompleted && historyItem.repeatCount > 0 -> Mood.HAPPY
            else -> Mood.NEUTRAL
        }
    }

    fun getMoodColor(mood: Mood): String = mood.colorHex
}
