package com.omnicast.tvsignage.data

data class TvDevice(
    val id: String,
    var name: String,
    var ipAddress: String,
    var macAddress: String = "",
    var port: Int = 8888,
    var currentVideoUrl: String? = null,
    var isLooping: Boolean = true,
    var isOnline: Boolean = false,
    var lastSeen: Long = System.currentTimeMillis()
)
