package com.example.photos

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed interface GooglePhotosAuthOutcome {
    data class Authorized(val accessToken: String) : GooglePhotosAuthOutcome
    data class NeedsConsent(val pendingIntent: PendingIntent) : GooglePhotosAuthOutcome
    data class Failed(val message: String) : GooglePhotosAuthOutcome
}

data class GoogleAccountUserInfo(
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
)

data class GooglePickerSession(
    val id: String,
    val pickerUri: String,
    val mediaItemsSet: Boolean
)

data class GooglePhotoItem(
    val id: String,
    val type: String,
    val mimeType: String,
    val filename: String,
    val baseUrl: String,
    val createTime: String?,
    val width: Int,
    val height: Int
)

class GooglePhotosRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val requestedScopes = listOf(
        Scope(SCOPE_PHOTOS_PICKER),
        Scope("openid"),
        Scope("email"),
        Scope("profile")
    )

    suspend fun authorizeGooglePhotos(activity: Activity): GooglePhotosAuthOutcome {
        return try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(requestedScopes)
                .build()
            val result = Identity.getAuthorizationClient(activity)
                .authorize(request)
                .await()

            val pending = result.pendingIntent
            if (result.hasResolution() && pending != null) {
                GooglePhotosAuthOutcome.NeedsConsent(pending)
            } else {
                val token = result.accessToken
                if (!token.isNullOrBlank()) {
                    GooglePhotosAuthOutcome.Authorized(token)
                } else {
                    GooglePhotosAuthOutcome.Failed("Google Photos did not return an access token.")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            GooglePhotosAuthOutcome.Failed(
                e.localizedMessage ?: "Failed to connect Google Photos account."
            )
        }
    }

    fun extractTokenFromConsentResult(activity: Activity, data: Intent?): Result<String> {
        if (data == null) {
            return Result.failure(IllegalStateException("Google Photos sign-in was cancelled."))
        }
        return try {
            val result = Identity.getAuthorizationClient(activity)
                .getAuthorizationResultFromIntent(data)
            val token = result.accessToken
            if (!token.isNullOrBlank()) {
                Result.success(token)
            } else {
                Result.failure(IllegalStateException("Access token was empty after consent."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchUserInfo(accessToken: String): Result<GoogleAccountUserInfo> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://www.googleapis.com/oauth2/v3/userinfo")
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException(parseGoogleApiError(bodyStr, response.code))
                        )
                    }
                    val json = JSONObject(bodyStr)
                    Result.success(
                        GoogleAccountUserInfo(
                            email = json.optString("email").takeIf { it.isNotBlank() },
                            displayName = json.optString("name").takeIf { it.isNotBlank() },
                            photoUrl = json.optString("picture").takeIf { it.isNotBlank() }
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createPickerSession(accessToken: String): Result<GooglePickerSession> =
        withContext(Dispatchers.IO) {
            try {
                val emptyJson = "{}".toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("$PICKER_BASE_URL/sessions")
                    .header("Authorization", "Bearer $accessToken")
                    .post(emptyJson)
                    .build()
                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException(parseGoogleApiError(bodyStr, response.code))
                        )
                    }
                    val json = JSONObject(bodyStr)
                    val id = json.optString("id")
                    val pickerUri = json.optString("pickerUri")
                    if (id.isBlank() || pickerUri.isBlank()) {
                        return@withContext Result.failure(
                            IllegalStateException("Google Photos Picker returned an invalid session.")
                        )
                    }
                    Result.success(
                        GooglePickerSession(
                            id = id,
                            pickerUri = pickerUri,
                            mediaItemsSet = json.optBoolean("mediaItemsSet", false)
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getPickerSession(accessToken: String, sessionId: String): Result<GooglePickerSession> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$PICKER_BASE_URL/sessions/$sessionId")
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException(parseGoogleApiError(bodyStr, response.code))
                        )
                    }
                    val json = JSONObject(bodyStr)
                    Result.success(
                        GooglePickerSession(
                            id = json.optString("id", sessionId),
                            pickerUri = json.optString("pickerUri"),
                            mediaItemsSet = json.optBoolean("mediaItemsSet", false)
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun listPickedMediaItems(accessToken: String, sessionId: String): Result<List<GooglePhotoItem>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$PICKER_BASE_URL/mediaItems?sessionId=$sessionId&pageSize=100")
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException(parseGoogleApiError(bodyStr, response.code))
                        )
                    }
                    val json = JSONObject(bodyStr)
                    val arr = json.optJSONArray("mediaItems")
                    val items = buildList {
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val obj = arr.optJSONObject(i) ?: continue
                                val mediaFile = obj.optJSONObject("mediaFile") ?: continue
                                val baseUrl = mediaFile.optString("baseUrl")
                                if (baseUrl.isBlank()) continue
                                val meta = mediaFile.optJSONObject("mediaFileMetadata")
                                add(
                                    GooglePhotoItem(
                                        id = obj.optString("id", "item_$i"),
                                        type = obj.optString("type", "PHOTO"),
                                        mimeType = mediaFile.optString("mimeType", "image/jpeg"),
                                        filename = mediaFile.optString("filename", "Google Photo"),
                                        baseUrl = baseUrl,
                                        createTime = obj.optString("createTime").takeIf { it.isNotBlank() },
                                        width = meta?.optInt("width", 0) ?: 0,
                                        height = meta?.optInt("height", 0) ?: 0
                                    )
                                )
                            }
                        }
                    }
                    Result.success(items)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun parseGoogleApiError(rawBody: String, httpCode: Int): String {
        val message = runCatching {
            JSONObject(rawBody).optJSONObject("error")?.optString("message")
        }.getOrNull().orEmpty()
        return when {
            message.contains("has not been used in project", ignoreCase = true) ||
                message.contains("SERVICE_DISABLED", ignoreCase = true) ||
                rawBody.contains("photospicker.googleapis.com", ignoreCase = true) ->
                "Enable the Google Photos Picker API (photospicker.googleapis.com) in Google Cloud Console for this project to load cloud photos."
            httpCode == 401 ->
                "Your Google Photos session expired. Tap Reconnect Google Photos to refresh access."
            message.isNotBlank() -> message
            else -> "Google Photos request failed (HTTP $httpCode)."
        }
    }

    companion object {
        const val SCOPE_PHOTOS_PICKER = "https://www.googleapis.com/auth/photospicker.mediaitems.readonly"
        private const val PICKER_BASE_URL = "https://photospicker.googleapis.com/v1"
    }
}
