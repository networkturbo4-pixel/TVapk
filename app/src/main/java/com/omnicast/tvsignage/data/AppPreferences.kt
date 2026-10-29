package com.omnicast.tvsignage.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        const val ROLE_UNSET = 0
        const val ROLE_TV = 1
        const val ROLE_CONTROLLER = 2

        private const val KEY_ROLE = "user_role"
        private const val KEY_TV_ID = "tv_device_id"
        private const val KEY_TV_NAME = "tv_name"
        private const val KEY_TV_VIDEO_URI = "tv_video_uri"
        private const val KEY_TV_LOOP = "tv_loop_enabled"
        private const val KEY_TV_AUTO_BOOT = "tv_auto_boot"
        private const val KEY_SAVED_TVS = "saved_tv_devices"
    }

    var role: Int
        get() = prefs.getInt(KEY_ROLE, ROLE_UNSET)
        set(value) = prefs.edit().putInt(KEY_ROLE, value).apply()

    var tvDeviceId: String
        get() {
            var id = prefs.getString(KEY_TV_ID, null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString(KEY_TV_ID, id).apply()
            }
            return id
        }
        set(value) = prefs.edit().putString(KEY_TV_ID, value).apply()

    var tvName: String
        get() = prefs.getString(KEY_TV_NAME, "Smart TV") ?: "Smart TV"
        set(value) = prefs.edit().putString(KEY_TV_NAME, value).apply()

    var defaultVideoUri: String?
        get() = prefs.getString(KEY_TV_VIDEO_URI, null)
        set(value) = prefs.edit().putString(KEY_TV_VIDEO_URI, value).apply()

    var isLoopEnabled: Boolean
        get() = prefs.getBoolean(KEY_TV_LOOP, true)
        set(value) = prefs.edit().putBoolean(KEY_TV_LOOP, value).apply()

    var autoBootEnabled: Boolean
        get() = prefs.getBoolean(KEY_TV_AUTO_BOOT, true)
        set(value) = prefs.edit().putBoolean(KEY_TV_AUTO_BOOT, value).apply()

    // Lista de televisores administrados por el controlador
    fun getSavedTvs(): MutableList<TvDevice> {
        val json = prefs.getString(KEY_SAVED_TVS, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<TvDevice>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun saveTvDevice(tv: TvDevice) {
        val list = getSavedTvs()
        val index = list.indexOfFirst { it.id == tv.id || (it.ipAddress == tv.ipAddress && tv.ipAddress.isNotEmpty()) }
        if (index >= 0) {
            list[index] = tv
        } else {
            list.add(tv)
        }
        prefs.edit().putString(KEY_SAVED_TVS, gson.toJson(list)).apply()
    }

    fun removeTvDevice(tvId: String) {
        val list = getSavedTvs().filter { it.id != tvId }
        prefs.edit().putString(KEY_SAVED_TVS, gson.toJson(list)).apply()
    }
}
