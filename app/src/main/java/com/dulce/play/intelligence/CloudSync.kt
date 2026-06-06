package com.dulce.play.intelligence

import com.dulce.play.data.local.entity.UserIntelligenceEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object CloudSync {
    private val db = FirebaseFirestore.getInstance()

    fun syncToFirebase(intelligence: UserIntelligenceEntity) {
        val data = mapOf(
            "lastUpdated" to intelligence.lastUpdated,
            "tasteMap" to intelligence.tasteMapJson,
            "routineMap" to intelligence.routineMapJson,
            "emotionalHistory" to intelligence.emotionalHistoryJson,
            "detectedState" to intelligence.detectedState
        )

        db.collection("user_profile_intelligence")
            .document(intelligence.profileId)
            .set(data, SetOptions.merge())
    }
}
