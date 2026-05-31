package com.example.data.local.entity

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Clean Architecture Database Entities prepared for Room local persistence.
 * This structure has been upgraded to support secure multi-profile and account isolation.
 */

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val email: String,
    val passwordHash: String,
    val displayName: String = "",
    val isLogged: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis()
)

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getAccountByEmail(email: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE isLogged = 1 LIMIT 1")
    suspend fun getActiveSession(): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: UserAccountEntity)

    @Query("UPDATE user_accounts SET isLogged = 0")
    suspend fun clearActiveSessions()

    @Query("UPDATE user_accounts SET isLogged = 1 WHERE email = :email")
    suspend fun setActiveSession(email: String)
}

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String, // composite or unique ID e.g., "p1", "p2", or UUID
    val parentEmail: String, // links back to UserAccountEntity
    val name: String,
    val avatarUrl: String,
    val isPremium: Boolean = true,
    val favoriteGenre: String = "Synthwave"
)

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE parentEmail = :email")
    fun getProfilesForAccount(email: String): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE parentEmail = :email")
    suspend fun getProfilesForAccountDirect(email: String): List<UserProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profiles WHERE id = :profileId")
    suspend fun deleteProfile(profileId: String)
}

@Entity(tableName = "iptv_playlists")
data class IPTVPlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val profileId: String = "p1",
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(tableName = "iptv_channels")
data class IPTVChannelEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val profileId: String = "p1",
    val name: String,
    val group: String,
    val streamUrl: String,
    val logoUrl: String,
    val country: String = "España"
)

@Dao
interface IPTVPlaylistDao {
    @Query("SELECT * FROM iptv_playlists WHERE profileId = :profileId ORDER BY dateCreated DESC")
    fun getAllPlaylists(profileId: String): Flow<List<IPTVPlaylistEntity>>

    @Query("SELECT * FROM iptv_playlists WHERE profileId = :profileId ORDER BY dateCreated DESC")
    suspend fun getAllPlaylistsDirect(profileId: String): List<IPTVPlaylistEntity>

    @Query("SELECT * FROM iptv_channels WHERE playlistId = :playlistId AND profileId = :profileId")
    fun getChannelsForPlaylist(playlistId: String, profileId: String): Flow<List<IPTVChannelEntity>>

    @Query("SELECT * FROM iptv_channels WHERE playlistId = :playlistId AND profileId = :profileId")
    suspend fun getChannelsForPlaylistDirect(playlistId: String, profileId: String): List<IPTVChannelEntity>

    @Query("SELECT * FROM iptv_channels WHERE profileId = :profileId")
    suspend fun getAllChannelsDirectly(profileId: String): List<IPTVChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: IPTVPlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<IPTVChannelEntity>)

    @Query("DELETE FROM iptv_playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("DELETE FROM iptv_channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsOfPlaylist(playlistId: String)
}

/**
 * Playback History entity to save recently listened or watched media.
 */
@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val compositeId: String, // format: "profileId_mediaId"
    @ColumnInfo(name = "id") val id: String, // media item id
    val profileId: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val streamUrl: String,
    val mediaType: String, // "AUDIO", "VIDEO", "IPTV"
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history WHERE profileId = :profileId ORDER BY timestamp DESC LIMIT 50")
    fun getPlaybackHistory(profileId: String): Flow<List<PlaybackHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE profileId = :profileId AND id = :id")
    suspend fun deleteHistoryItem(profileId: String, id: String)

    @Query("DELETE FROM playback_history WHERE profileId = :profileId")
    suspend fun clearHistory(profileId: String)
}

/**
 * User Playlist custom grouping entities.
 */
@Entity(tableName = "user_playlists")
data class UserPlaylistEntity(
    @PrimaryKey val id: String,
    val profileId: String = "p1",
    val name: String,
    val coverUrl: String = "playlist_default",
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_playlist_items")
data class UserPlaylistItemEntity(
    @PrimaryKey val id: String, // format: "playlistId_mediaId"
    val playlistId: String,
    val profileId: String = "p1",
    val mediaId: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val streamUrl: String,
    val mediaType: String,
    val durationText: String = "03:45"
)

@Dao
interface UserPlaylistDao {
    @Query("SELECT * FROM user_playlists WHERE profileId = :profileId ORDER BY dateCreated DESC")
    fun getAllUserPlaylists(profileId: String): Flow<List<UserPlaylistEntity>>

    @Query("SELECT * FROM user_playlist_items WHERE playlistId = :playlistId AND profileId = :profileId")
    fun getItemsForPlaylist(playlistId: String, profileId: String): Flow<List<UserPlaylistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPlaylist(playlist: UserPlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: UserPlaylistItemEntity)

    @Query("DELETE FROM user_playlist_items WHERE playlistId = :playlistId AND mediaId = :mediaId AND profileId = :profileId")
    suspend fun deletePlaylistItem(playlistId: String, mediaId: String, profileId: String)

    @Query("DELETE FROM user_playlists WHERE id = :playlistId AND profileId = :profileId")
    suspend fun deleteUserPlaylist(playlistId: String, profileId: String)

    @Query("DELETE FROM user_playlist_items WHERE playlistId = :playlistId AND profileId = :profileId")
    suspend fun deleteItemsForPlaylist(playlistId: String, profileId: String)
}

/**
 * Lightweight settings configuration inside database.
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Dao
interface AppSettingsDao {
    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: AppSettingsEntity)
}

/**
 * Central Database class representing the main Room relational store.
 */
@Database(
    entities = [
        UserAccountEntity::class,
        UserProfileEntity::class,
        IPTVPlaylistEntity::class,
        IPTVChannelEntity::class,
        PlaybackHistoryEntity::class,
        UserPlaylistEntity::class,
        UserPlaylistItemEntity::class,
        AppSettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userAccountDao(): UserAccountDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun iptvPlaylistDao(): IPTVPlaylistDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun userPlaylistDao(): UserPlaylistDao
    abstract fun appSettingsDao(): AppSettingsDao
}
