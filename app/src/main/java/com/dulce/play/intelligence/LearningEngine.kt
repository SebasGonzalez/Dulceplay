package com.dulce.play.intelligence

import android.content.Context
import com.dulce.play.data.local.entity.IntelligenceDao
import com.dulce.play.data.local.entity.PlaybackHistoryEntity
import com.dulce.play.data.local.entity.UserIntelligenceEntity
import com.google.gson.Gson
import kotlinx.coroutines.*

class LearningEngine(
    private val context: Context,
    private val intelligenceDao: IntelligenceDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val gson = Gson()

    fun processNewEvent(historyItem: PlaybackHistoryEntity) {
        scope.launch {
            val profile = intelligenceDao.getIntelligenceForProfile(historyItem.profileId) 
                ?: UserIntelligenceEntity(historyItem.profileId)
            
            val updatedProfile = updateIntelligence(profile, historyItem)
            intelligenceDao.insertIntelligence(updatedProfile)
            
            if (updatedProfile.isCloudSyncEnabled) {
                CloudSync.syncToFirebase(updatedProfile)
            }
        }
    }

    private fun updateIntelligence(
        current: UserIntelligenceEntity,
        item: PlaybackHistoryEntity
    ): UserIntelligenceEntity {
        // Simple logic to build maps (actual implementation would be more complex)
        val tastes = gson.fromJson(current.tasteMapJson, Map::class.java).toMutableMap()
        val genre = item.energyLevel // using energy as proxy for genre for now
        val count = (tastes[genre] as? Double ?: 0.0) + 1.0
        tastes[genre] = count

        val mood = EmotionAnalyzer.detectMood(item)
        
        return current.copy(
            lastUpdated = System.currentTimeMillis(),
            tasteMapJson = gson.toJson(tastes),
            detectedState = mood.name
        )
    }
}
