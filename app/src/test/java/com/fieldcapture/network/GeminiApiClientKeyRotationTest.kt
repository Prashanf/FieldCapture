package com.fieldcapture.network

import android.app.Application
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GeminiApiClientKeyRotationTest {

    @Test
    fun `constructor parses comma-separated keys`() {
        val client = GeminiApiClient("key1, key2, key3")
        assertEquals(3, client.keyCount())
        assertEquals(0, client.currentKeyIndex())
    }

    @Test
    fun `single key backward compatible`() {
        val client = GeminiApiClient("single_key")
        assertEquals(1, client.keyCount())
        assertEquals(0, client.currentKeyIndex())
    }

    @Test
    fun `empty key string returns failure on use`() = runTest {
        val client = GeminiApiClient("")
        assertEquals(0, client.keyCount())
        val result = client.analyzeImage(prompt = "test")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `rotates immediately to fresh key on 429 without backoff`() = runTest {
        val requestedKeys = mutableListOf<String?>()
        val mockOkHttp = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url
                val key = url.queryParameter("key")
                requestedKeys += key
                val (code, body) = when (key) {
                    "k1", "k2" -> 429 to """{"error":{"code":429,"status":"RESOURCE_EXHAUSTED"}}"""
                    else -> 200 to """{"candidates":[{"content":{"parts":[{"text":"{\"ok\":true}"}]}}]}"""
                }
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message(if (code == 200) "OK" else "Too Many Requests")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val client = GeminiApiClient(
            apiKeys = listOf("k1", "k2", "k3"),
            modelEndpoints = listOf("https://example.test/models/gemini-3.5-flash:generateContent"),
            client = mockOkHttp
        )

        val result = client.analyzeImage(prompt = "extract")
        assertTrue("Expected success on third key", result.isSuccess)
        assertEquals("{\"ok\":true}", result.getOrNull())
        assertEquals(listOf("k1", "k2", "k3"), requestedKeys)
        assertEquals(2, client.currentKeyIndex())
    }

    @Test
    fun `throws AllKeysExhaustedException when all keys return 429 across models`() = runTest {
        val requestedCalls = mutableListOf<Pair<String, String?>>()
        val mockOkHttp = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url
                requestedCalls += url.encodedPath to url.queryParameter("key")
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(429)
                    .message("Too Many Requests")
                    .body("""{"error":{"code":429}}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val client = GeminiApiClient(
            apiKeys = listOf("k1", "k2", "k3"),
            modelEndpoints = listOf(
                "https://example.test/models/primary:generateContent",
                "https://example.test/models/fallback:generateContent"
            ),
            client = mockOkHttp
        )

        val result = client.analyzeImage(prompt = "extract")
        assertTrue(result.isFailure)
        assertTrue(
            "Expected AllKeysExhaustedException, got ${result.exceptionOrNull()}",
            result.exceptionOrNull() is GeminiApiClient.AllKeysExhaustedException
        )
        assertEquals(6, requestedCalls.size)
    }

    @Test
    fun `503 retries same key without rotating to another key`() = runTest {
        var attempts = 0
        val requestedKeys = mutableListOf<String?>()
        val mockOkHttp = OkHttpClient.Builder()
            .addInterceptor { chain ->
                attempts++
                requestedKeys += chain.request().url.queryParameter("key")
                val (code, body) = if (attempts == 1) {
                    503 to """{"error":{"code":503}}"""
                } else {
                    200 to """{"candidates":[{"content":{"parts":[{"text":"{\"recovered\":true}"}]}}]}"""
                }
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message(if (code == 200) "OK" else "Service Unavailable")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val client = GeminiApiClient(
            apiKeys = listOf("k1", "k2"),
            modelEndpoints = listOf("https://example.test/models/primary:generateContent"),
            client = mockOkHttp
        )

        val result = client.analyzeImage(prompt = "extract")
        assertTrue(result.isSuccess)
        assertEquals(listOf("k1", "k1"), requestedKeys)
        assertEquals(0, client.currentKeyIndex())
    }
}
