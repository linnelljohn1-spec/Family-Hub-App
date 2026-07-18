package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class ExtendsClassResponse(
    val status: Int,
    val uri: String?,
    val id: String?
)

object SyncService {
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val payloadAdapter = moshi.adapter(SyncPayload::class.java)
    private val responseAdapter = moshi.adapter(ExtendsClassResponse::class.java)

    private const val BASE_URL = "https://extendsclass.com/api/json-storage/bin"

    suspend fun createSyncGroup(initialPayload: SyncPayload): String = suspendCancellableCoroutine { continuation ->
        val json = payloadAdapter.toJson(initialPayload)
        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(BASE_URL)
            .post(body)
            .build()

        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        continuation.resumeWithException(IOException("Server error: ${response.code}"))
                        return
                    }
                    val bodyString = response.body?.string()
                    if (bodyString.isNullOrEmpty()) {
                        continuation.resumeWithException(IOException("Empty response from server"))
                        return
                    }
                    try {
                        val res = responseAdapter.fromJson(bodyString)
                        val id = res?.id
                        if (id != null) {
                            continuation.resume(id)
                        } else {
                            continuation.resumeWithException(IOException("Failed to parse sync group ID from response"))
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        })
    }

    suspend fun fetchSyncPayload(code: String): SyncPayload = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url("$BASE_URL/$code")
            .get()
            .build()

        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        continuation.resumeWithException(IOException("Server error: ${response.code}"))
                        return
                    }
                    val json = response.body?.string()
                    if (json.isNullOrEmpty()) {
                        continuation.resumeWithException(IOException("Empty response from server"))
                        return
                    }
                    try {
                        val payload = payloadAdapter.fromJson(json)
                        if (payload != null) {
                            continuation.resume(payload)
                        } else {
                            continuation.resumeWithException(IOException("Failed to parse sync payload"))
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        })
    }

    suspend fun updateSyncPayload(code: String, payload: SyncPayload): Unit = withContext(Dispatchers.IO) {
        val json = payloadAdapter.toJson(payload)
        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/$code")
            .put(body)
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw e
        }

        response.use {
            if (response.isSuccessful) {
                return@withContext
            }
            throw IOException("Server error: ${response.code}")
        }
    }
}
