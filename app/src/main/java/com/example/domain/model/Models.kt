package com.example.domain.model

enum class MediaType {
    AUDIO, VIDEO, IPTV
}

data class MediaItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationText: String = "03:45",
    val durationSeconds: Int = 225,
    val coverUrl: String,
    val streamUrl: String,
    val mediaType: MediaType,
    val genre: String = "Electro",
    val isPremium: Boolean = false,
    val country: String = "Global"
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
