package com.example.ui.auth

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object OAuthHelper {
    private const val TAG = "OAuthHelper"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Redirect landing page URL
    const val REDIRECT_URI = "https://dulceplay.com/auth-callback"

    data class OAuthProfile(
        val email: String,
        val displayName: String,
        val avatarUrl: String,
        val provider: String,
        val providerId: String
    )

    /**
     * Extracts values from simple JSON payloads without needing dynamic reflection/serializers.
     */
    fun extractJsonValue(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }

    /**
     * Performs a real backend token exchange and fetches the profile details for a provider.
     */
    suspend fun authenticateWithProvider(
        provider: String,
        code: String,
        clientId: String,
        clientSecret: String
    ): OAuthProfile? = withContext(Dispatchers.IO) {
        try {
            when (provider.uppercase()) {
                "GITHUB" -> exchangeGitHub(code, clientId, clientSecret)
                "DISCORD" -> exchangeDiscord(code, clientId, clientSecret)
                "MICROSOFT" -> exchangeMicrosoft(code, clientId, clientSecret)
                "GOOGLE" -> exchangeGoogle(code, clientId, clientSecret)
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in OAuth authenticate with provider $provider", e)
            null
        }
    }

    private fun exchangeGitHub(code: String, clientId: String, clientSecret: String): OAuthProfile? {
        // 1. Exchange Code for Token
        val tokenUrl = "https://github.com/login/oauth/access_token"
        val body = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .build()

        val tokenRequest = Request.Builder()
            .url(tokenUrl)
            .post(body)
            .addHeader("Accept", "application/json")
            .build()

        val tokenResponse = client.newCall(tokenRequest).execute()
        val tokenResponseBody = tokenResponse.body?.string() ?: return null
        val accessToken = extractJsonValue(tokenResponseBody, "access_token") ?: return null

        // 2. Fetch User Info
        val userUrl = "https://api.github.com/user"
        val userRequest = Request.Builder()
            .url(userUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Accept", "application/json")
            .build()

        val userResponse = client.newCall(userRequest).execute()
        val userResponseBody = userResponse.body?.string() ?: return null

        val login = extractJsonValue(userResponseBody, "login") ?: "github_user"
        val name = extractJsonValue(userResponseBody, "name") ?: login
        val avatar = extractJsonValue(userResponseBody, "avatar_url") ?: ""
        val id = extractJsonValue(userResponseBody, "id") ?: login

        // Attempting to fetch email (sometimes private, fallback to id-domain)
        var email = extractJsonValue(userResponseBody, "email")
        if (email == null || email == "null") {
            email = "$login@github.com"
        }

        return OAuthProfile(
            email = email,
            displayName = name,
            avatarUrl = avatar,
            provider = "GitHub",
            providerId = id
        )
    }

    private fun exchangeDiscord(code: String, clientId: String, clientSecret: String): OAuthProfile? {
        // 1. Token Exchange
        val tokenUrl = "https://discord.com/api/v10/oauth2/token"
        val body = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .build()

        val tokenRequest = Request.Builder()
            .url(tokenUrl)
            .post(body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        val tokenResponse = client.newCall(tokenRequest).execute()
        val tokenResponseBody = tokenResponse.body?.string() ?: return null
        val accessToken = extractJsonValue(tokenResponseBody, "access_token") ?: return null

        // 2. Fetch Profile Info
        val userUrl = "https://discord.com/api/v10/users/@me"
        val userRequest = Request.Builder()
            .url(userUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val userResponse = client.newCall(userRequest).execute()
        val userResponseBody = userResponse.body?.string() ?: return null

        val username = extractJsonValue(userResponseBody, "username") ?: "discord_user"
        val id = extractJsonValue(userResponseBody, "id") ?: "discord_id"
        val avatarHash = extractJsonValue(userResponseBody, "avatar")
        val avatar = if (avatarHash != null && avatarHash != "null") {
            "https://cdn.discordapp.com/avatars/$id/$avatarHash.png"
        } else {
            "avatar_guest"
        }
        var email = extractJsonValue(userResponseBody, "email")
        if (email == null || email == "null") {
            email = "$username@discord.com"
        }

        return OAuthProfile(
            email = email,
            displayName = username,
            avatarUrl = avatar,
            provider = "Discord",
            providerId = id
        )
    }

    private fun exchangeMicrosoft(code: String, clientId: String, clientSecret: String): OAuthProfile? {
        // 1. Token Exchange
        val tokenUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/token"
        val body = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .add("grant_type", "authorization_code")
            .build()

        val tokenRequest = Request.Builder()
            .url(tokenUrl)
            .post(body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        val tokenResponse = client.newCall(tokenRequest).execute()
        val tokenResponseBody = tokenResponse.body?.string() ?: return null
        val accessToken = extractJsonValue(tokenResponseBody, "access_token") ?: return null

        // 2. Fetch User Profile
        val userUrl = "https://graph.microsoft.com/v1.0/me"
        val userRequest = Request.Builder()
            .url(userUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val userResponse = client.newCall(userRequest).execute()
        val userResponseBody = userResponse.body?.string() ?: return null

        val displayName = extractJsonValue(userResponseBody, "displayName") ?: "microsoft_user"
        val id = extractJsonValue(userResponseBody, "id") ?: "microsoft_id"
        var email = extractJsonValue(userResponseBody, "mail") 
            ?: extractJsonValue(userResponseBody, "userPrincipalName")
        if (email == null || email == "null") {
            email = "$id@outlook.com"
        }

        return OAuthProfile(
            email = email,
            displayName = displayName,
            avatarUrl = "avatar_guest",
            provider = "Microsoft",
            providerId = id
        )
    }

    private fun exchangeGoogle(code: String, clientId: String, clientSecret: String): OAuthProfile? {
        // 1. Token Exchange
        val tokenUrl = "https://oauth2.googleapis.com/token"
        val body = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .add("grant_type", "authorization_code")
            .build()

        val tokenRequest = Request.Builder()
            .url(tokenUrl)
            .post(body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        val tokenResponse = client.newCall(tokenRequest).execute()
        val tokenResponseBody = tokenResponse.body?.string() ?: return null
        val accessToken = extractJsonValue(tokenResponseBody, "access_token") ?: return null

        // 2. Fetch Profile Info
        val userUrl = "https://www.googleapis.com/oauth2/v3/userinfo"
        val userRequest = Request.Builder()
            .url(userUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val userResponse = client.newCall(userRequest).execute()
        val userResponseBody = userResponse.body?.string() ?: return null

        val name = extractJsonValue(userResponseBody, "name") ?: "google_user"
        val id = extractJsonValue(userResponseBody, "sub") ?: "google_id"
        val picture = extractJsonValue(userResponseBody, "picture") ?: ""
        val email = extractJsonValue(userResponseBody, "email") ?: "$id@gmail.com"

        return OAuthProfile(
            email = email,
            displayName = name,
            avatarUrl = picture,
            provider = "Google",
            providerId = id
        )
    }
}
