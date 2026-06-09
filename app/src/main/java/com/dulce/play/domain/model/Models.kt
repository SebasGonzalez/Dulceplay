package com.dulce.play.domain.model

import java.util.UUID

enum class MediaType {
    AUDIO, VIDEO, IPTV
}

data class MediaItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val artist: String = "Oficial",
    val album: String = "DulcePlay",
    val durationText: String = "03:45",
    val durationSeconds: Int = 225,
    val coverUrl: String = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17",
    val streamUrl: String,
    val mediaType: MediaType = MediaType.AUDIO,
    val genre: String = "Electro",
    val isPremium: Boolean = false,
    val country: String = "Global",
    val duration: String = "" // For compatibility
)

data class IPTVChannel(
    val id: String,
    val name: String,
    val group: String,
    val streamUrl: String,
    val logoUrl: String,
    val country: String = "España"
)

data class UserProfile(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val isPremium: Boolean = true,
    val favoriteGenre: String = "Synthwave"
)
