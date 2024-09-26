package com.techgriffo254.harmonyhub.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.techgriffo254.harmonyhub.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class JamendoAuthManager(
    private val context: Context,
    private val userPreferences: UserPreferences
) {
    private val clientId = "1c07a888"
    private val clientSecret = "ae6c49b586aa3649300422304d120903"
    val redirectUri = "com.techgriffo254.harmonyhub://callback"
    private val scope = "music"

    private val okHttpClient = OkHttpClient()

    fun getClientId(): String {
        return clientId
    }

    fun startAuthProcess() {
        val authorizeUrl = Uri.parse("https://api.jamendo.com/v3.0/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("audio_format", "mp3")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("scope", scope)
            .appendQueryParameter("response_type", "code")
            .build()
        Log.d("JamendoAuthManager", "Starting auth process with URL: $authorizeUrl")
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, authorizeUrl)
    }

    fun startSignUpProcess() {
        val signUpUrl = "https://www.jamendo.com/signup"
        Log.d("JamendoAuthManager", "Starting sign-up process with URL: $signUpUrl")

        // Try to use Custom Tabs if available
        try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(context, Uri.parse(signUpUrl))
        } catch (e: Exception) {
            // Fallback to default browser if Custom Tabs are not available
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(signUpUrl))
            context.startActivity(intent)
        }
    }

    suspend fun handleAuthorizationResponse(uri: Uri?): AuthResult {
        val code = uri?.getQueryParameter("code")
        if (code == null) {
            Log.e("JamendoAuthManager", "No code received in URI")
            return AuthResult.Error("No code received")
        }
        return withContext(Dispatchers.IO) {
            try {
                val tokenResponse = exchangeCodeForToken(code)
                val accessToken = tokenResponse.getString("access_token")
                val refreshToken = tokenResponse.getString("refresh_token")
                val expiresIn = tokenResponse.getInt("expires_in")

                userPreferences.saveAuthTokens(accessToken, refreshToken, expiresIn)
                AuthResult.Success(
                    accessToken = tokenResponse.getString("access_token"),
                    refreshToken = tokenResponse.getString("refresh_token"),
                    expiresIn = tokenResponse.getInt("expires_in")
                )
            } catch (e: Exception) {
                Log.e("JamendoAuthManager", "Error exchanging code for token", e)
                AuthResult.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    suspend fun refreshAccessToken(): AuthResult {
        val refreshToken = userPreferences.refreshToken.first()
            ?: return AuthResult.Error("No refresh token found")

        val requestBody = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()

        val request = Request.Builder()
            .url("https://api.jamendo.com/v3.0/oauth/grant")
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: throw Exception("Empty response body")
                val tokenResponse = JSONObject(responseBody)

                val newAccessToken = tokenResponse.getString("access_token")
                val newRefreshToken = tokenResponse.getString("refresh_token")
                val expiresIn = tokenResponse.getInt("expires_in")

                userPreferences.saveAuthTokens(newAccessToken, newRefreshToken, expiresIn)

                AuthResult.Success(newAccessToken, newRefreshToken, expiresIn)
            } catch (e: Exception) {
                AuthResult.Error(e.message ?: "Unknown error occurred")
            }
        }

    }

    private fun exchangeCodeForToken(code: String): JSONObject {
        val requestBody = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", redirectUri)
            .build()

        val request = Request.Builder()
            .url("https://api.jamendo.com/v3.0/oauth/grant")
            .post(requestBody)
            .build()

        Log.d("JamendoAuthManager", "Exchanging code for token with request: $request")
        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response body")
        Log.d("JamendoAuthManager", "Token exchange response: $responseBody")
        return JSONObject(responseBody)
    }
}

sealed class AuthResult {
    data class Success(val accessToken: String, val refreshToken: String, val expiresIn: Int) :
        AuthResult()

    data class Error(val message: String) : AuthResult()
}
