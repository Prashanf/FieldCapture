package com.fieldcapture.network

import android.util.Base64
import android.util.Log
import com.fieldcapture.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiApiClient @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val baseUrl =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val MAX_RETRIES = 3
        private val RETRY_DELAYS = longArrayOf(1000L, 2000L, 4000L)
    }

    /**
     * Send an optional image + text prompt to Gemini 2.0 Flash and get JSON text response.
     * Includes exponential backoff retry on HTTP 429, 5xx, and transient IO errors.
     */
    suspend fun analyzeImage(
        imageBytes: ByteArray? = null,
        mimeType: String = "image/jpeg",
        prompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key not configured. Add GEMINI_API_KEY to local.properties")
            )
        }

        val parts = buildList<Map<String, Any>> {
            if (imageBytes != null && imageBytes.isNotEmpty()) {
                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                add(
                    mapOf(
                        "inlineData" to mapOf(
                            "mimeType" to mimeType,
                            "data" to base64Image
                        )
                    )
                )
            }
            add(mapOf("text" to prompt))
        }

        val requestJson = JsonObject().apply {
            add("contents", gson.toJsonTree(listOf(mapOf("parts" to parts))))
            add(
                "generationConfig",
                gson.toJsonTree(
                    mapOf(
                        "temperature" to 0.1,
                        "maxOutputTokens" to 2048,
                        "responseMimeType" to "application/json"
                    )
                )
            )
        }

        val requestBody = gson.toJson(requestJson)
            .toRequestBody("application/json".toMediaType())

        val url = "$baseUrl?key=$apiKey"
        var lastException: Exception? = null

        for (attempt in 0 until MAX_RETRIES) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    when {
                        response.isSuccessful -> {
                            val text = extractTextFromResponse(responseBody)
                            return@withContext if (text != null) {
                                Result.success(text)
                            } else {
                                Result.failure(IOException("Failed to parse Gemini response: $responseBody"))
                            }
                        }
                        response.code == 429 -> {
                            Log.w(TAG, "Rate limited (429), attempt ${attempt + 1}/$MAX_RETRIES")
                            lastException = IOException("Rate limited: ${response.code}")
                            if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAYS[attempt])
                        }
                        response.code in 500..599 -> {
                            Log.w(TAG, "Server error (${response.code}), attempt ${attempt + 1}/$MAX_RETRIES")
                            lastException = IOException("Server error: ${response.code}")
                            if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAYS[attempt])
                        }
                        else -> {
                            return@withContext Result.failure(
                                IOException("API error ${response.code}: $responseBody")
                            )
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error, attempt ${attempt + 1}/$MAX_RETRIES", e)
                lastException = e
                if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAYS[attempt])
            }
        }

        Result.failure(lastException ?: IOException("Failed after $MAX_RETRIES attempts"))
    }

    private fun extractTextFromResponse(responseBody: String): String? {
        return try {
            val json = JsonParser.parseString(responseBody).asJsonObject
            json.getAsJsonArray("candidates")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini response envelope", e)
            null
        }
    }
}
