package com.omnicast.tvsignage.controller

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.omnicast.tvsignage.data.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class TvApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // Tiempo holgado para videos grandes
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getStatus(tv: TvDevice): Result<JsonObject> = withContext(Dispatchers.IO) {
        try {
            val url = "http://${tv.ipAddress}:${tv.port}/status"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val json = JsonParser.parseString(body).asJsonObject
                    Result.success(json)
                } else {
                    Result.failure(Exception("Error HTTP: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPlayUrl(
        tv: TvDevice,
        videoUrl: String,
        loop: Boolean = true,
        saveAsDefault: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("url", videoUrl)
                addProperty("loop", loop)
                addProperty("saveAsDefault", saveAsDefault)
            }

            val requestBody = json.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("http://${tv.ipAddress}:${tv.port}/play")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(response.body?.string() ?: "OK")
                } else {
                    Result.failure(Exception("Error al reproducir: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadVideo(
        tv: TvDevice,
        file: File,
        loop: Boolean = true,
        saveAsDefault: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileBody = file.asRequestBody("video/*".toMediaType())
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("video", file.name, fileBody)
                .build()

            val url = "http://${tv.ipAddress}:${tv.port}/upload?loop=$loop&saveAsDefault=$saveAsDefault"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(response.body?.string() ?: "OK")
                } else {
                    Result.failure(Exception("Error al subir video: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendControl(tv: TvDevice, action: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("action", action)
            }
            val requestBody = json.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("http://${tv.ipAddress}:${tv.port}/control")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(response.body?.string() ?: "OK")
                } else {
                    Result.failure(Exception("Error al controlar: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
