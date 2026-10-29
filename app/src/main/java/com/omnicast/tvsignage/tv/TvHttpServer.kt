package com.omnicast.tvsignage.tv

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class TvHttpServer(
    private val context: Context,
    port: Int = 8888,
    private val listener: ServerListener
) : NanoHTTPD(port) {

    interface ServerListener {
        fun onPlayUrlRequested(url: String, loop: Boolean, saveAsDefault: Boolean)
        fun onVideoFileUploaded(file: File, loop: Boolean, saveAsDefault: Boolean)
        fun onControlAction(action: String)
        fun getStatusJson(): String
    }

    private val gson = Gson()

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        // Permitir CORS para peticiones locales
        fun createCorsResponse(status: Response.IStatus, mimeType: String, txt: String): Response {
            val resp = newFixedLengthResponse(status, mimeType, txt)
            resp.addHeader("Access-Control-Allow-Origin", "*")
            resp.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            resp.addHeader("Access-Control-Allow-Headers", "Content-Type")
            return resp
        }

        if (method == Method.OPTIONS) {
            return createCorsResponse(Response.Status.OK, "text/plain", "OK")
        }

        return try {
            when {
                uri == "/status" && method == Method.GET -> {
                    val status = listener.getStatusJson()
                    createCorsResponse(Response.Status.OK, "application/json", status)
                }

                uri == "/play" && method == Method.POST -> {
                    val map = HashMap<String, String>()
                    session.parseBody(map)
                    val postData = map["postData"] ?: ""
                    val json = JsonParser.parseString(postData).asJsonObject

                    val videoUrl = json.get("url")?.asString ?: ""
                    val loop = json.get("loop")?.asBoolean ?: true
                    val saveDefault = json.get("saveAsDefault")?.asBoolean ?: true

                    if (videoUrl.isNotEmpty()) {
                        listener.onPlayUrlRequested(videoUrl, loop, saveDefault)
                        val resp = JsonObject().apply {
                            addProperty("success", true)
                            addProperty("message", "Reproduciendo URL")
                        }
                        createCorsResponse(Response.Status.OK, "application/json", resp.toString())
                    } else {
                        createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"URL vacía\"}")
                    }
                }

                uri == "/upload" && method == Method.POST -> {
                    val files = HashMap<String, String>()
                    session.parseBody(files)

                    // NanoHTTPD almacena archivos temporales en el mapa 'files'
                    val tempFilePath = files["video"] ?: files.values.firstOrNull()

                    if (tempFilePath != null) {
                        val videoDir = File(context.filesDir, "signage_videos").apply { mkdirs() }
                        val destFile = File(videoDir, "tv_video_${System.currentTimeMillis()}.mp4")

                        FileInputStream(File(tempFilePath)).use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }

                        val params = session.parms
                        val loop = params["loop"]?.toBoolean() ?: true
                        val saveDefault = params["saveAsDefault"]?.toBoolean() ?: true

                        listener.onVideoFileUploaded(destFile, loop, saveDefault)

                        val resp = JsonObject().apply {
                            addProperty("success", true)
                            addProperty("path", destFile.absolutePath)
                            addProperty("message", "Video recibido y reproduciendo")
                        }
                        createCorsResponse(Response.Status.OK, "application/json", resp.toString())
                    } else {
                        createCorsResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"Archivo no encontrado\"}")
                    }
                }

                uri == "/control" && method == Method.POST -> {
                    val map = HashMap<String, String>()
                    session.parseBody(map)
                    val postData = map["postData"] ?: ""
                    val json = JsonParser.parseString(postData).asJsonObject
                    val action = json.get("action")?.asString ?: ""

                    listener.onControlAction(action)
                    val resp = JsonObject().apply {
                        addProperty("success", true)
                        addProperty("action", action)
                    }
                    createCorsResponse(Response.Status.OK, "application/json", resp.toString())
                }

                else -> {
                    createCorsResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            createCorsResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"error\": \"${e.message}\"}")
        }
    }
}
